package org.springframework.cloud.deployer.spi.nomad.docker;

import java.io.IOException;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.nomad.AbstractNomadDeployer;
import org.springframework.cloud.deployer.spi.nomad.NomadDeployerProperties;
import org.springframework.cloud.deployer.spi.nomad.NomadDeploymentPropertyKeys;
import org.springframework.cloud.deployer.spi.task.LaunchState;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.cloud.deployer.spi.task.TaskStatus;

import io.github.zanella.nomad.NomadClient;
import io.github.zanella.nomad.v1.jobs.models.JobAllocation;
import io.github.zanella.nomad.v1.jobs.models.JobEvalResult;
import io.github.zanella.nomad.v1.jobs.models.JobSpec;
import io.github.zanella.nomad.v1.jobs.models.JobSummary;
import io.github.zanella.nomad.v1.nodes.models.Resources;
import io.github.zanella.nomad.v1.nodes.models.Task;
import io.github.zanella.nomad.v1.nodes.models.TaskGroup;

/**
 * Deployer responsible for deploying
 * {@link org.springframework.cloud.deployer.resource.docker.DockerResource} based tasks using the
 * Nomad <a href="https://www.nomadproject.io/docs/drivers/docker.html">Docker</a> driver.
 *
 * @author Donovan Muller
 */
public class DockerNomadTaskLauncher extends AbstractNomadDeployer implements TaskLauncher {

	private static final Logger log = LoggerFactory.getLogger(DockerNomadTaskLauncher.class);

	private NomadClient client;
	private NomadDeployerProperties deployerProperties;

	public DockerNomadTaskLauncher(NomadClient nomadClient, NomadDeployerProperties deployerProperties) {
		client = nomadClient;
		this.deployerProperties = deployerProperties;
	}

	@Override
	public String launch(AppDeploymentRequest request) {
		String taskId = createTaskId(request);
		JobSpec jobSpec = buildBatchJobSpec(taskId, deployerProperties, request);
		jobSpec.setTaskGroups(buildTaskGroups(taskId, request, deployerProperties));

		JobEvalResult jobEvalResult = client.v1.jobs.postJob(jobSpec);
		log.info("Launched task '{}': {}", taskId, jobEvalResult);

		return taskId;
	}

	@Override
	public void cancel(String taskId) {
		log.info("Cancelling task '{}'", taskId);
		JobSummary job = getJobByName(taskId);
		client.v1.job.deleteJob(job.getId());
	}

	@Override
	public TaskStatus status(String taskId) {
		JobSummary job = getJobByName(taskId);
		JobAllocation allocation = getAllocationEvaluation(client, job);
		return buildTaskStatus(taskId, allocation);
	}

	/**
	 * Multiple instances of tasks are not currently supported.
	 */
	@Override
	protected Integer getAppCount(final AppDeploymentRequest request) {
		return 1;
	}

	/**
	 * Adjust the restart policy for batch jobs. I.e they should not restart on failure.
	 *
	 * TODO currently this does not seem to be working when run from the tests
	 */
	@Override
	protected TaskGroup buildTaskGroup(final String appId, final AppDeploymentRequest request,
			final NomadDeployerProperties deployerProperties, final int count) {
		TaskGroup taskGroup = super.buildTaskGroup(appId, request, deployerProperties, count);

		taskGroup.setRestartPolicy(
				new TaskGroup.RestartPolicy(milliToNanoseconds(0L), milliToNanoseconds(0L), 1, "fail"));

		return taskGroup;
	}

	/**
	 * A task does not require health checks/service discovery and therefore a Service is not
	 * condigured. See https://www.nomadproject.io/docs/jobspec/servicediscovery.html
	 */
	protected Task buildTask(AppDeploymentRequest request, String appId) {
		Task task = new Task();
		task.setName(appId);
		task.setDriver("docker");
		Task.Config config = new Task.Config();
		try {
			config.setImage(request.getResource().getURI().getSchemeSpecificPart());
		}
		catch (IOException e) {
			throw new IllegalArgumentException("Unable to get URI for " + request.getResource(), e);
		}

		// according to the Nomad documentation, you cannot pass command like arguments to a Docker
		// container without also specifying a command, however, it is possible. See
		// https://www.nomadproject.io/docs/drivers/docker.html#args
		// Issue logged: https://github.com/hashicorp/nomad/issues/1813
		config.setArgs(request.getCommandlineArguments());
		task.setConfig(config);

		HashMap<String, String> env = new HashMap<>();
		env.putAll(createEnvironmentVariables(deployerProperties, request));
		task.setEnv(env);

		task.setResources(new Resources(
				Integer.valueOf(request.getDeploymentProperties().getOrDefault(
						NomadDeploymentPropertyKeys.NOMAD_RESOURCES_CPU, deployerProperties.getResourcesCpu())),
				Integer.valueOf(request.getDeploymentProperties().getOrDefault(
						NomadDeploymentPropertyKeys.NOMAD_RESOURCES_MEMORY, deployerProperties.getResourcesMemory())),
				Integer.valueOf(request.getDeploymentProperties().getOrDefault(
						NomadDeploymentPropertyKeys.NOMAD_RESOURCES_DISK, deployerProperties.getResourcesDisk())),
				0, null));

		task.setLogConfig(new Task.LogConfig(deployerProperties.getLoggingMaxFiles(),
				deployerProperties.getLoggingMaxFileSize()));

		return task;
	}

	protected JobAllocation getAllocationEvaluation(NomadClient client, JobSummary jobSummary) {
		return client.v1.job.getJobAllocations(jobSummary.getId()).stream()
				.sorted((o1, o2) -> o2.getCreateIndex().compareTo(o1.getCreateIndex())).findFirst()
				.orElseThrow(() -> new IllegalStateException(
						String.format("Job '%s' does not have any allocations", jobSummary.getName())));
	}

	protected TaskStatus buildTaskStatus(String id, JobAllocation allocation) {
		if (allocation == null) {
			return new TaskStatus(id, LaunchState.unknown, new HashMap<>());
		}
		switch (allocation.getClientStatus()) {
		case "pending":
			return new TaskStatus(id, LaunchState.launching, new HashMap<>());

		case "running":
			return new TaskStatus(id, LaunchState.running, new HashMap<>());

		case "failed":
			return new TaskStatus(id, LaunchState.failed, new HashMap<>());

		case "lost":
			return new TaskStatus(id, LaunchState.failed, new HashMap<>());

		case "complete":
			return new TaskStatus(id, LaunchState.complete, new HashMap<>());

		case "dead":
			return new TaskStatus(id, LaunchState.complete, new HashMap<>());

		default:
			return new TaskStatus(id, LaunchState.unknown, new HashMap<>());
		}
	}

	private String createTaskId(AppDeploymentRequest request) {
		String groupId = request.getDeploymentProperties().get(AppDeployer.GROUP_PROPERTY_KEY);
		String taskId;
		if (groupId == null) {
			taskId = String.format("%s", request.getDefinition().getName());
		}
		else {
			taskId = String.format("%s-%s", groupId, request.getDefinition().getName());
		}
		return String.format("%s-%d", taskId.replace('.', '-'), System.currentTimeMillis());
	}

	private JobSummary getJobByName(final String taskId) {
		return client.v1.jobs.getJobs().stream().filter(jobSummary -> jobSummary.getName().equals(taskId)).findFirst()
				.orElseThrow(
						() -> new IllegalStateException(String.format("Job with name '%s' does not exist", taskId)));
	}
}
