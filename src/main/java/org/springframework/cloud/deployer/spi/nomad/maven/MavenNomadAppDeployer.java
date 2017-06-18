package org.springframework.cloud.deployer.spi.nomad.maven;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.deployer.resource.maven.MavenResource;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.app.AppStatus;
import org.springframework.cloud.deployer.spi.app.DeploymentState;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.core.RuntimeEnvironmentInfo;
import org.springframework.cloud.deployer.spi.nomad.AbstractNomadDeployer;
import org.springframework.cloud.deployer.spi.nomad.NomadDeployerProperties;
import org.springframework.cloud.deployer.spi.nomad.NomadDeploymentPropertyKeys;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.zanella.nomad.NomadClient;
import io.github.zanella.nomad.v1.jobs.models.JobAllocation;
import io.github.zanella.nomad.v1.jobs.models.JobEvalResult;
import io.github.zanella.nomad.v1.jobs.models.JobSpec;
import io.github.zanella.nomad.v1.jobs.models.JobSummary;
import io.github.zanella.nomad.v1.nodes.models.Resources;
import io.github.zanella.nomad.v1.nodes.models.Task;

/**
 * Deployer responsible for deploying
 * {@link org.springframework.cloud.deployer.resource.maven.MavenResource} based applications using
 * the Nomad <a href="https://www.nomadproject.io/docs/drivers/java.html">Java</a> driver.
 *
 * @author Donovan Muller
 */
public class MavenNomadAppDeployer extends AbstractNomadDeployer implements AppDeployer, MavenSupport {

	private static final Logger logger = LoggerFactory.getLogger(MavenNomadAppDeployer.class);

	private NomadClient client;
	private NomadDeployerProperties deployerProperties;

	public MavenNomadAppDeployer(NomadClient client, NomadDeployerProperties deployerProperties) {
		super(client, deployerProperties);
		this.client = client;
		this.deployerProperties = deployerProperties;
	}

	@Override
	public String deploy(AppDeploymentRequest request) {
		String deploymentId = createDeploymentId(request);

		AppStatus status = status(deploymentId);
		if (!status.getState().equals(DeploymentState.unknown)) {
			throw new IllegalStateException(String.format("App '%s' is already deployed", deploymentId));
		}

		JobSpec jobSpec = buildJobSpec(deploymentId, deployerProperties, request);
		jobSpec.setTaskGroups(buildTaskGroups(deploymentId, request, deployerProperties));

		JobEvalResult jobEvalResult = client.v1.jobs.postJob(jobSpec);
		logger.info("Deployed app '{}': {}", deploymentId, jobEvalResult);

		return deploymentId;
	}

	@Override
	public void undeploy(String deploymentId) {
		logger.info("Undeploying job '{}'", deploymentId);
		JobSummary job = getJobByName(deploymentId);
		client.v1.job.deleteJob(job.getId());
	}

	@Override
	public AppStatus status(String deploymentId) {
		JobSummary job = getJobByName(deploymentId);
		if (job == null) {
			return AppStatus.of(deploymentId).build();
		}

		List<JobAllocation> allocations = getAllocationEvaluation(client, job);
		return buildAppStatus(deploymentId, allocations);
	}

	@Override
	public RuntimeEnvironmentInfo environmentInfo() {
		return createRuntimeEnvironmentInfo(AppDeployer.class, this.getClass());
	}

	@Override
	protected Task buildTask(AppDeploymentRequest request, String deploymentId) {
		Task.TaskBuilder taskBuilder = Task.builder();
		taskBuilder.name(deploymentId);
		taskBuilder.driver("java");

		Task.Config.ConfigBuilder configBuilder = Task.Config.builder();
		MavenResource resource = (MavenResource) request.getResource();

		configBuilder
				.jarPath(String.format("%s/%s", deployerProperties.getArtifactDestination(), resource.getFilename()));
		configBuilder.jvmOptions(new ArrayList<>(StringUtils.commaDelimitedListToSet(request.getDeploymentProperties()
				.getOrDefault(NomadDeploymentPropertyKeys.NOMAD_JAVA_OPTS, deployerProperties.getJavaOpts()))));
		taskBuilder.config(configBuilder.build());

		Resources.Network network = new Resources.Network();
		network.setMBits(deployerProperties.getResources().getNetworkMBits());
		List<Resources.Network.DynamicPort> dynamicPorts = new ArrayList<>();
		dynamicPorts.add(new Resources.Network.DynamicPort(configureExternalPort(request), "http"));
		network.setDynamicPorts(dynamicPorts);

		taskBuilder.resources(new Resources(getCpuResource(deployerProperties, request),
				getMemoryResource(deployerProperties, request),
				// deprecated, use
				// org.springframework.cloud.deployer.spi.nomad.NomadDeployerProperties.EphemeralDisk
				null, 0, Stream.of(network).collect(toList())));

		HashMap<String, String> env = new HashMap<>();
		env.putAll(getAppEnvironmentVariables(request));
		env.putAll(arrayToMap(deployerProperties.getEnvironmentVariables()));
		// just in case the server.port is specified as a env. variable, should never happen :)
		env.replaceAll((key, value) -> replaceServerPortWithPort(value));
		env.put("SPRING_APPLICATION_JSON", toSpringApplicationJson(request));

		taskBuilder.env(env);

		// see
		// https://www.nomadproject.io/docs/job-specification/artifact.html#download-and-verify-checksums
		Map<String, String> options = new HashMap<>();
		options.put("checksum", String.format("md5:%s", new ResourceChecksum().generateMD5Checksum(resource)));
		taskBuilder.artifacts(
				Stream.of(new Task.Artifacts(toURIString((MavenResource) request.getResource(), deployerProperties),
						deployerProperties.getArtifactDestination(), options)).collect(toList()));

		taskBuilder.logConfig(new Task.LogConfig(deployerProperties.getLoggingMaxFiles(),
				deployerProperties.getLoggingMaxFileSize()));

		return taskBuilder.build();
	}

	protected String replaceServerPortWithPort(String potentialBootServerPort) {
		// handle both `SERVER_PORT` and `server.port` variations
		if (potentialBootServerPort.replaceAll("_", ".").equalsIgnoreCase("server.port")) {
			return "${NOMAD_PORT_http}";
		}

		return potentialBootServerPort;
	}

	/**
	 * Coverts the results of {@link AbstractNomadDeployer#createCommandLineArguments} (a
	 * {@link List} of <code>--arg1=val1,--arg2=val2</code>) to SPRING_APPLICATION_JSON JSON.
	 */
	protected String toSpringApplicationJson(AppDeploymentRequest request) {
		try {
			return new ObjectMapper().writeValueAsString(
					createCommandLineArguments(request).stream().map(argument -> argument.replaceAll("-", ""))
							.collect(toMap(argument -> argument.substring(0, argument.indexOf("=")),
									argument -> argument.substring(argument.lastIndexOf("=") + 1))));
		}
		catch (JsonProcessingException e) {
			throw new IllegalStateException("Unable to create SPRING_APPLICATION_JSON", e);

		}
	}
}
