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
import org.springframework.cloud.deployer.spi.app.AppInstanceStatus;
import org.springframework.cloud.deployer.spi.app.AppStatus;
import org.springframework.cloud.deployer.spi.app.DeploymentState;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.core.RuntimeEnvironmentInfo;
import org.springframework.cloud.deployer.spi.nomad.NomadDeployerProperties;
import org.springframework.cloud.deployer.spi.nomad.NomadSupport;

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
 * {@link org.springframework.cloud.deployer.resource.docker.DockerResource} based applications
 * using the Nomad <a href="https://www.nomadproject.io/docs/drivers/docker.html">Docker</a> driver.
 *
 * @author Donovan Muller
 */
public class DockerNomadAppDeployer extends AbstractDockerNomadDeployer implements AppDeployer, NomadSupport {

	private static final Logger logger = LoggerFactory.getLogger(DockerNomadAppDeployer.class);

	private NomadClient client;
	private NomadDeployerProperties deployerProperties;

	public DockerNomadAppDeployer(NomadClient client, NomadDeployerProperties deployerProperties) {
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

		AppStatus status = status(deploymentId);
		if (status.getState().equals(DeploymentState.unknown)) {
			throw new IllegalStateException(String.format("App '%s' is not deployed", deploymentId));
		}

		JobSummary job = getJobByName(deploymentId);
		client.v1.job.deleteJob(job.getId());
	}

	@Override
	public AppStatus status(String deploymentId) {
		JobSummary job = getJobByName(deploymentId);
		if (job == null) {
			return AppStatus.of(deploymentId).build();
		}

		AppStatus appStatus;
		if (!job.getStatus().equals("dead")) {
			List<JobAllocation> allocations = getAllocationEvaluation(client, job);
			appStatus = buildAppStatus(deploymentId, allocations);
		}
		else {
			appStatus = AppStatus.of(deploymentId).with(new AppInstanceStatus() {
				@Override
				public String getId() {
					return deploymentId;
				}

				@Override
				public DeploymentState getState() {
					return DeploymentState.failed;
				}

				@Override
				public Map<String, String> getAttributes() {
					return null;
				}
			}).build();
		}

		return appStatus;
	}

	@Override
	public RuntimeEnvironmentInfo environmentInfo() {
		return createRuntimeEnvironmentInfo(AppDeployer.class, this.getClass());
	}

	@Override
	protected Task buildTask(AppDeploymentRequest request, String deploymentId) {
		Task.TaskBuilder taskBuilder = Task.builder();
		taskBuilder.name(deploymentId);
		taskBuilder.driver("docker");
		Task.Config.ConfigBuilder configBuilder = Task.Config.builder();
		try {
			configBuilder.image(request.getResource().getURI().getSchemeSpecificPart());
		}
		catch (IOException e) {
			throw new IllegalArgumentException("Unable to get URI for " + request.getResource(), e);
		}

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

		Map<String, Integer> portMap = new HashMap<>();
		portMap.put("http", configureExternalPort(request));
		configBuilder.portMap(Stream.of(portMap).collect(toList()));
		configBuilder.volumes(createVolumes(deployerProperties, request));

		// See
		// https://github.com/spring-cloud/spring-cloud-deployer-kubernetes/blob/master/src/main/java/org/springframework/cloud/deployer/spi/kubernetes/DefaultContainerFactory.java#L91
		EntryPointStyle entryPointStyle = determineEntryPointStyle(deployerProperties, request);
		switch (entryPointStyle) {
		case exec:
			configBuilder.args(createCommandLineArguments(request));
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

		taskBuilder.config(configBuilder.build());
		taskBuilder.env(env);

		taskBuilder.logConfig(new Task.LogConfig(deployerProperties.getLoggingMaxFiles(),
				deployerProperties.getLoggingMaxFileSize()));

		return taskBuilder.build();
	}
}
