package org.springframework.cloud.deployer.spi.nomad;

import static java.util.stream.Collectors.toList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.util.Assert;

import io.github.zanella.nomad.v1.jobs.models.JobSpec;
import io.github.zanella.nomad.v1.nodes.models.Task;
import io.github.zanella.nomad.v1.nodes.models.TaskGroup;

/**
 * @author Donovan Muller
 */
public abstract class AbstractNomadDeployer implements NomadSupport {

	private static final Logger log = LoggerFactory.getLogger(AbstractNomadDeployer.class);

	protected abstract Integer getAppCount(AppDeploymentRequest request);

	protected abstract Task buildTask(AppDeploymentRequest request, String deploymentId);

	protected JobSpec buildServiceJobSpec(String deploymentId, NomadDeployerProperties deployerProperties,
			AppDeploymentRequest request) {
		return buildJobSpec(deploymentId, deployerProperties, request, JobTypes.SERVICE);
	}

	protected JobSpec buildBatchJobSpec(String taskId, NomadDeployerProperties deployerProperties,
			AppDeploymentRequest request) {
		return buildJobSpec(taskId, deployerProperties, request, JobTypes.BATCH);
	}

	protected JobSpec buildJobSpec(String deploymentId, NomadDeployerProperties deployerProperties,
			AppDeploymentRequest request, JobTypes jobType) {
		JobSpec jobSpec = new JobSpec();
		jobSpec.setId(deploymentId);
		jobSpec.setName(deploymentId);
		jobSpec.setRegion(deployerProperties.getRegion());
		jobSpec.setType(jobType.name().toLowerCase());
		jobSpec.setDatacenters(deployerProperties.getDatacenters());
		jobSpec.setPriority(Integer.valueOf(request.getDeploymentProperties()
				.getOrDefault(NomadDeploymentPropertyKeys.JOB_PRIORITY, deployerProperties.getPriority().toString())));
		jobSpec.setMeta(createMeta(request));

		return jobSpec;
	}

	protected List<TaskGroup> buildTaskGroups(String deploymentId, AppDeploymentRequest request,
			NomadDeployerProperties deployerProperties) {
		TaskGroup taskGroup = buildTaskGroup(deploymentId, request, deployerProperties, getAppCount(request));
		taskGroup.setTasks(Stream.of(buildTask(request, deploymentId)).collect(toList()));

		return Stream.of(taskGroup).collect(toList());
	}

	/**
	 * The TaskGroup name uses the <code>deploymentId</code> (and not the
	 * AppDeployer.GROUP_PROPERTY_KEY deployment property) because we don't necessarily want apps in
	 * the same group to be scheduled on the same client. See
	 * https://www.nomadproject.io/docs/internals/architecture.html
	 */
	protected TaskGroup buildTaskGroup(String deploymentId, AppDeploymentRequest request,
			NomadDeployerProperties deployerProperties, int count) {
		TaskGroup taskGroup = new TaskGroup();
		taskGroup.setName(deploymentId);
		taskGroup.setCount(count);

		taskGroup.setRestartPolicy(
				new TaskGroup.RestartPolicy(milliToNanoseconds(deployerProperties.getRestartPolicyDelay()),
						milliToNanoseconds(deployerProperties.getRestartPolicyInterval()),
						deployerProperties.getRestartPolicyAttempts(), deployerProperties.getRestartPolicyMode()));

		taskGroup.setEphemeralDisk(new TaskGroup.EphemeralDisk(
				Boolean.parseBoolean(request.getDeploymentProperties().getOrDefault(
						NomadDeploymentPropertyKeys.NOMAD_EPHEMERAL_DISK_STICKY,
						deployerProperties.getEphemeralDisk().getSticky().toString())),
				Boolean.parseBoolean(request.getDeploymentProperties().getOrDefault(
						NomadDeploymentPropertyKeys.NOMAD_EPHEMERAL_DISK_MIGRATE,
						deployerProperties.getEphemeralDisk().getMigrate().toString())),
				Integer.valueOf(request.getDeploymentProperties().getOrDefault(
						NomadDeploymentPropertyKeys.NOMAD_EPHEMERAL_DISK_SIZE,
						deployerProperties.getEphemeralDisk().getSize().toString()))));

		return taskGroup;
	}

	protected Map<String, String> createEnvironmentVariables(NomadDeployerProperties deployerProperties,
			AppDeploymentRequest request) {
		Map<String, String> environmentVariables = new HashMap<>();
		environmentVariables.putAll(commandLineArgumentsToMap(deployerProperties.getEnvironmentVariables()));
		environmentVariables.putAll(request.getDefinition().getProperties());
		environmentVariables.putAll(getAppEnvironmentVariables(request));

		log.debug("Using environment variables: " + environmentVariables);
		return environmentVariables;
	}

	private Map<String, String> createMeta(AppDeploymentRequest request) {
		Map<String, String> metaMap = new HashMap<>();
		String metaValue = request.getDeploymentProperties().get(NomadDeploymentPropertyKeys.NOMAD_META);
		if (metaValue != null) {
			String[] metaKeyValue = metaValue.split(",");
			for (String metaKeyValues : metaKeyValue) {
				String[] strings = metaKeyValues.split("=", 2);
				Assert.isTrue(strings.length == 2, "Invalid meta value declared: " + metaKeyValues);
				metaMap.put(strings[0], strings[1]);
			}
		}
		return metaMap;
	}

	private Map<String, String> getAppEnvironmentVariables(AppDeploymentRequest request) {
		Map<String, String> appEnvVarMap = new HashMap<>();
		String appEnvVar = request.getDeploymentProperties()
				.get(NomadDeploymentPropertyKeys.NOMAD_ENVIRONMENT_VARIABLES);
		if (appEnvVar != null) {
			String[] appEnvVars = appEnvVar.split(",");
			for (String envVar : appEnvVars) {
				log.trace("Adding environment variable from AppDeploymentRequest: " + envVar);
				String[] strings = envVar.split("=", 2);
				Assert.isTrue(strings.length == 2, "Invalid environment variable declared: " + envVar);
				appEnvVarMap.put(strings[0], strings[1]);
			}
		}
		return appEnvVarMap;
	}
}
