package org.springframework.cloud.deployer.spi.nomad.maven;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.app.AppStatus;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.nomad.NomadConsulAppInstanceStatus;
import org.springframework.cloud.deployer.spi.nomad.NomadDeployerProperties;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.health.model.Check;

import io.github.zanella.nomad.NomadClient;
import io.github.zanella.nomad.v1.jobs.models.JobAllocation;
import io.github.zanella.nomad.v1.jobs.models.JobSummary;
import io.github.zanella.nomad.v1.nodes.models.Task;
import io.github.zanella.nomad.v1.nodes.models.TaskGroup;

/**
 * Deployer responsible for deploying
 * {@link org.springframework.cloud.deployer.resource.docker.DockerResource} based applications
 * using the Nomad <a href="https://www.nomadproject.io/docs/drivers/java.html">Java</a> driver.
 *
 * See
 * {@link org.springframework.cloud.deployer.spi.nomad.docker.IndexingDockerNomadWithConsulAppDeployer}
 *
 * @author Donovan Muller
 */
public class IndexingMavenNomadWithConsulAppDeployer extends MavenNomadWithConsulAppDeployer {

	private static final Logger logger = LoggerFactory.getLogger(IndexingMavenNomadWithConsulAppDeployer.class);

	private final NomadClient client;
	private final ConsulClient consul;

	public IndexingMavenNomadWithConsulAppDeployer(NomadClient client, ConsulClient consul,
			NomadDeployerProperties deployerProperties) {
		super(client, consul, deployerProperties);

		this.client = client;
		this.consul = consul;
	}

	@Override
	protected List<TaskGroup> buildTaskGroups(String appId, AppDeploymentRequest request,
			NomadDeployerProperties deployerProperties) {

		List<TaskGroup> taskGroups = new ArrayList<>();
		if (isIndexed(request)) {
			for (Integer index = 0; index < getAppCount(request); index++) {
				String indexedId = appId + "-" + index;
				TaskGroup taskGroup = buildTaskGroup(indexedId, request, deployerProperties, 1);
				Task task = buildTask(request, indexedId);

				Map<String, String> environmentVariables = task.getEnv();
				environmentVariables.putIfAbsent(AppDeployer.INSTANCE_INDEX_PROPERTY_KEY, index.toString());
				environmentVariables.putIfAbsent("SPRING_APPLICATION_INDEX", index.toString());
				environmentVariables.putIfAbsent("SPRING_CLOUD_APPLICATION_GROUP",
						request.getDeploymentProperties().get(AppDeployer.GROUP_PROPERTY_KEY));

				taskGroup.setTasks(Stream.of(task).collect(toList()));
				taskGroups.add(taskGroup);
			}
		}
		else {
			taskGroups.addAll(super.buildTaskGroups(appId, request, deployerProperties));
		}

		return taskGroups;
	}

	@Override
	public AppStatus status(String deploymentId) {
		JobSummary job = getJobByName(deploymentId);
		if (job == null) {
			return AppStatus.of(deploymentId).build();
		}

		List<JobAllocation> allocations = getAllocationEvaluation(client, job);
		return buildAppStatus(deploymentId, allocations, consul);
	}

	/**
	 * Build the {@link AppStatus} based on a Job allocations. Each allocation will get it's own
	 * corresponding health check from Consul.
	 */
	protected AppStatus buildAppStatus(String id, List<JobAllocation> allocations, ConsulClient consul) {
		AppStatus.Builder statusBuilder = AppStatus.of(id);
		allocations.forEach(allocation -> {
			Response<List<Check>> healthChecks = consul.getHealthChecksForService(allocation.getTaskGroup(),
					QueryParams.DEFAULT);
			logger.debug("Health checks for '{}': {}", allocation.getTaskGroup(), healthChecks);
			Check check = healthChecks.getValue().isEmpty() ? null : healthChecks.getValue().get(0);
			statusBuilder.with(
					new NomadConsulAppInstanceStatus(client.v1.allocation.getAllocation(allocation.getId()), check));
		});
		return statusBuilder.build();
	}

	protected boolean isIndexed(AppDeploymentRequest request) {
		String indexedProperty = request.getDeploymentProperties().get(INDEXED_PROPERTY_KEY);
		return (indexedProperty != null) ? Boolean.valueOf(indexedProperty) : false;
	}
}
