package org.springframework.cloud.deployer.spi.nomad.docker;

import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.app.AppStatus;
import org.springframework.cloud.deployer.spi.app.DeploymentState;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.nomad.NomadAppInstanceStatus;
import org.springframework.cloud.deployer.spi.nomad.NomadDeployerProperties;
import org.springframework.cloud.deployer.spi.nomad.NomadDeploymentPropertyKeys;
import org.springframework.cloud.deployer.spi.nomad.NomadSupport;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.zanella.nomad.NomadClient;
import io.github.zanella.nomad.v1.jobs.models.JobAllocation;
import io.github.zanella.nomad.v1.jobs.models.JobEvalResult;
import io.github.zanella.nomad.v1.jobs.models.JobSpec;
import io.github.zanella.nomad.v1.jobs.models.JobSummary;
import io.github.zanella.nomad.v1.nodes.models.Resources;
import io.github.zanella.nomad.v1.nodes.models.Service;
import io.github.zanella.nomad.v1.nodes.models.Task;

/**
 * Deployer responsible for deploying
 * {@link org.springframework.cloud.deployer.resource.docker.DockerResource} based applications
 * using the Nomad <a href="https://www.nomadproject.io/docs/drivers/docker.html">Docker</a> driver.
 *
 * @author Donovan Muller
 */
public class DockerNomadAppDeployer extends AbstractDockerNomadDeployer implements AppDeployer, NomadSupport {

	private static final Logger log = LoggerFactory.getLogger(DockerNomadAppDeployer.class);

	private static final String SERVER_PORT_KEY = "server.port";
	protected static final String SPRING_DEPLOYMENT_KEY = "spring-deployment-id";
	protected static final String SPRING_GROUP_KEY = "spring-group-id";
	protected static final String SPRING_APP_KEY = "spring-app-id";

	private NomadClient client;
	private NomadDeployerProperties deployerProperties;

	public DockerNomadAppDeployer(NomadClient client, NomadDeployerProperties deployerProperties) {
		this.client = client;
		this.deployerProperties = deployerProperties;
	}

	@Override
	public String deploy(AppDeploymentRequest request) {
		log.info("Deploying request using the Docker task driver: {}", request);

		String deploymentId = createDeploymentId(request);

		AppStatus status = status(deploymentId);
		if (!status.getState().equals(DeploymentState.unknown)) {
			throw new IllegalStateException(String.format("App '%s' is already deployed", deploymentId));
		}

		JobSpec jobSpec = buildServiceJobSpec(deploymentId, deployerProperties, request);
		jobSpec.setTaskGroups(buildTaskGroups(deploymentId, request, deployerProperties));

		JobEvalResult jobEvalResult = client.v1.jobs.postJob(jobSpec);
		log.info("Deployed app '{}': {}", deploymentId, jobEvalResult);

		return deploymentId;
	}

	@Override
	public void undeploy(String deploymentId) {
		log.info("Undeploying job '{}'", deploymentId);
		JobSummary job = getJobByName(deploymentId);
		client.v1.job.deleteJob(job.getId());
	}

	/**
	 * TODO include option to check health via Consul if available. The Consul check would be much
	 * more reliable as a "running" job/allocation is not necessarily healthy in terms of the app.
	 */
	@Override
	public AppStatus status(String deploymentId) {
		JobSummary job = getJobByName(deploymentId);
		if (job == null) {
			return AppStatus.of(deploymentId).build();
		}

		JobAllocation allocation = getAllocationEvaluation(client, job);
		return buildAppStatus(deploymentId, allocation);
	}

	private JobSummary getJobByName(final String deploymentId) {
		return client.v1.jobs.getJobs().stream().filter(jobSummary -> jobSummary.getName().equals(deploymentId))
				.findFirst().orElse(null);
	}

	/**
	 * Get the allocation for the specified Job. Since we get a
	 * <a href="https://www.nomadproject.io/docs/http/job.html">list of allocations</a>, we take the
	 * allocation with the largest create index (I.e. the latest allocation).
	 *
	 * TODO this strategy doesn't work for apps with an <code>app.xxx.count > 1</code> because there
	 * will be multiple allocations for the single Job definition. In that case we probably need to
	 * filter using another strategy and return a List of {@link JobAllocation}'s
	 */
	protected JobAllocation getAllocationEvaluation(NomadClient client, JobSummary jobSummary) {
		return client.v1.job.getJobAllocations(jobSummary.getId()).stream()
				.sorted((o1, o2) -> o2.getCreateIndex().compareTo(o1.getCreateIndex())).findFirst()
				.orElseThrow(() -> new IllegalStateException(
						String.format("Job '%s' does not have any allocations", jobSummary.getName())));
	}

	/**
	 * Build the {@link AppStatus} based on a single allocation.
	 *
	 * TODO As mentioned on {@link DockerNomadAppDeployer#getAllocationEvaluation}, this method
	 * should accept a List of {@link JobAllocation}'s to support multiple instances of an app.
	 */
	protected AppStatus buildAppStatus(String id, JobAllocation allocation) {
		AppStatus.Builder statusBuilder = AppStatus.of(id);
		statusBuilder.with(new NomadAppInstanceStatus(client.v1.allocation.getAllocation(allocation.getId())));
		return statusBuilder.build();
	}

	protected String createDeploymentId(AppDeploymentRequest request) {
		String groupId = request.getDeploymentProperties().get(AppDeployer.GROUP_PROPERTY_KEY);
		String deploymentId;
		if (groupId == null) {
			deploymentId = String.format("%s", request.getDefinition().getName());
		}
		else {
			deploymentId = String.format("%s-%s", groupId, request.getDefinition().getName());
		}
		return deploymentId.replace('.', '-');
	}

	protected Task buildTask(AppDeploymentRequest request, String deploymentId) {
		Task task = new Task();
		task.setName(deploymentId);
		task.setDriver("docker");
		Task.Config config = new Task.Config();
		try {
			config.setImage(request.getResource().getURI().getSchemeSpecificPart());
		}
		catch (IOException e) {
			throw new IllegalArgumentException("Unable to get URI for " + request.getResource(), e);
		}

		Resources.Network network = new Resources.Network();
		network.setMBits(deployerProperties.getResources().getNetworkMBits());
		List<Resources.Network.DynamicPort> dynamicPorts = new ArrayList<>();
		dynamicPorts.add(new Resources.Network.DynamicPort(configureExternalPort(request), "http"));
		network.setDynamicPorts(dynamicPorts);

		task.setResources(new Resources(getCpuResource(deployerProperties, request),
				getMemoryResource(deployerProperties, request),
				// deprecated, use
				// org.springframework.cloud.deployer.spi.nomad.NomadDeployerProperties.EphemeralDisk
				null, 0, Stream.of(network).collect(toList())));

		HashMap<String, String> env = new HashMap<>();
		env.putAll(createEnvironmentVariables(deployerProperties, request));

		Map<String, Integer> portMap = new HashMap<>();
		portMap.put("http", configureExternalPort(request));
		config.setPortMap(Stream.of(portMap).collect(toList()));
		config.setVolumes(createVolumes(deployerProperties, request));

		// See
		// https://github.com/spring-cloud/spring-cloud-deployer-kubernetes/blob/master/src/main/java/org/springframework/cloud/deployer/spi/kubernetes/DefaultContainerFactory.java#L91
		EntryPointStyle entryPointStyle = determineEntryPointStyle(deployerProperties, request);
		switch (entryPointStyle) {
		case exec:
			config.setArgs(request.getCommandlineArguments());
			break;
		case boot:
			if (env.containsKey("SPRING_APPLICATION_JSON")) {
				throw new IllegalStateException(
						"You can't use boot entry point style and also set SPRING_APPLICATION_JSON for the app");
			}
			try {
				env.put("SPRING_APPLICATION_JSON",
						new ObjectMapper().writeValueAsString(request.getDefinition().getProperties()));
			}
			catch (JsonProcessingException e) {
				throw new IllegalStateException("Unable to create SPRING_APPLICATION_JSON", e);
			}
			break;
		case shell:
			for (String key : request.getDefinition().getProperties().keySet()) {
				String envVar = key.replace('.', '_').toUpperCase();
				env.put(envVar, request.getDefinition().getProperties().get(key));
			}
			break;
		}

		task.setConfig(config);
		task.setEnv(env);

		Service service = new Service();
		service.setName(deploymentId);
		service.setPortLabel("http");

		ArrayList<String> tags = new ArrayList<>();
		tags.addAll(createTags(deploymentId, request));
		if (exposeFabioRoute(request)) {
			tags.add(buildFabioUrlPrefix(request, deploymentId));
		}
		service.setTags(tags);

		Service.Check check = new Service.Check();
		check.setName(String.format("%s HTTP Check", deploymentId));
		check.setType("http");
		check.setProtocol("http");
		check.setPath(deployerProperties.getCheckHttpPath());
		check.setInterval(milliToNanoseconds(deployerProperties.getCheckInterval()));
		check.setTimeout(milliToNanoseconds(deployerProperties.getCheckTimeout()));
		service.setChecks(Stream.of(check).collect(toList()));

		task.setServices(Stream.of(service).collect(toList()));
		task.setLogConfig(new Task.LogConfig(deployerProperties.getLoggingMaxFiles(),
				deployerProperties.getLoggingMaxFileSize()));

		return task;
	}

	protected Integer getAppCount(AppDeploymentRequest request) {
		String countProperty = request.getDeploymentProperties().get(COUNT_PROPERTY_KEY);
		return (countProperty != null) ? Integer.parseInt(countProperty) : 1;
	}

	/**
	 * Creates a {@link List} of labels for a given <code>deploymentId</code>.
	 */
	protected List<String> createTags(String deploymentId, AppDeploymentRequest request) {
		return createTags(deploymentId, request, null);
	}

	/**
	 * Creates a {@link List} of labels for a given <code>deploymentId</code> and
	 * <code>instanceIndex</code>.
	 */
	protected List<String> createTags(String deploymentId, AppDeploymentRequest request, Integer instanceIndex) {
		List<String> tags = new ArrayList<>();
		tags.add(String.format("%s=%s", SPRING_APP_KEY, deploymentId));
		String groupId = request.getDeploymentProperties().get(AppDeployer.GROUP_PROPERTY_KEY);
		if (groupId != null) {
			tags.add(String.format("%s=%s", SPRING_GROUP_KEY, groupId));
		}
		String appInstanceId = instanceIndex == null ? deploymentId : deploymentId + "-" + instanceIndex;
		tags.add(String.format("%s=%s", SPRING_DEPLOYMENT_KEY, appInstanceId));

		return tags;
	}

	/**
	 * Decide if the app should include tags that would allow Fabio to create the routing entries
	 * when registered with Consul. The following deployment/deployer properties enable this:
	 *
	 * <ul>
	 * <li>spring.cloud.deployer.nomad.fabio.expose</li>
	 * </ul>
	 */
	protected boolean exposeFabioRoute(AppDeploymentRequest request) {
		boolean expose = false;
		String exposeProperty = request.getDeploymentProperties()
				.get(NomadDeploymentPropertyKeys.NOMAD_EXPOSE_VIA_FABIO);
		if (StringUtils.isEmpty(exposeProperty)) {
			expose = deployerProperties.isExposeViaFabio();
		}
		else {
			if (Boolean.parseBoolean(exposeProperty.toLowerCase())) {
				expose = true;
			}
		}

		return expose;
	}

	/**
	 * See {@link NomadDeploymentPropertyKeys#NOMAD_FABIO_ROUTE_HOSTNAME}
	 */
	protected String buildFabioUrlPrefix(AppDeploymentRequest request, String deploymentId) {
		return String.format("urlprefix-%s/", request.getDeploymentProperties()
				.getOrDefault(NomadDeploymentPropertyKeys.NOMAD_FABIO_ROUTE_HOSTNAME, deploymentId));
	}

	private int configureExternalPort(AppDeploymentRequest request) {
		int externalPort = 8080;
		Map<String, String> parameters = request.getDefinition().getProperties();
		if (parameters.containsKey(SERVER_PORT_KEY)) {
			externalPort = Integer.valueOf(parameters.get(SERVER_PORT_KEY));
		}

		return externalPort;
	}
}
