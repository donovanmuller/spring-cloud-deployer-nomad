package org.springframework.cloud.deployer.spi.nomad.docker;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.nomad.NomadDeployerProperties;

import io.github.zanella.nomad.NomadClient;
import io.github.zanella.nomad.v1.nodes.models.TaskGroup;

/**
 * Deployer responsible for deploying
 * {@link org.springframework.cloud.deployer.resource.docker.DockerResource} based applications
 * using the Nomad <a href="https://www.nomadproject.io/docs/drivers/docker.html">Docker</a> driver.
 *
 * This implementation adds support for partitioned/indexed applications. Each indexed application
 * is configured as a separate TaskGroup, allowing each application instance to be scheduled on a
 * different client. If implemented as separate tasks, the application instance would be scheduled
 * on the same client, which might not be ideal. See
 * https://www.nomadproject.io/docs/internals/architecture.html
 *
 * @author Donovan Muller
 */
public class IndexingDockerNomadAppDeployer extends DockerNomadAppDeployer {

	public IndexingDockerNomadAppDeployer(NomadClient client, NomadDeployerProperties deployerProperties) {
		super(client, deployerProperties);
	}

	@Override
	protected List<TaskGroup> buildTaskGroups(String appId, AppDeploymentRequest request,
			NomadDeployerProperties deployerProperties) {
		String indexedProperty = request.getDeploymentProperties().get(INDEXED_PROPERTY_KEY);
		boolean indexed = (indexedProperty != null) ? Boolean.valueOf(indexedProperty) : false;

		List<TaskGroup> taskGroups = new ArrayList<>();
		if (indexed) {
			for (int index = 0; index < getAppCount(request); index++) {
				String indexedId = appId + "-" + index;
				TaskGroup taskGroup = buildTaskGroup(indexedId, request, deployerProperties, 1);
				taskGroup.setTasks(Stream.of(buildTask(request, indexedId)).collect(toList()));
				taskGroups.add(taskGroup);
			}
		}
		else {
			taskGroups.addAll(super.buildTaskGroups(appId, request, deployerProperties));
		}

		return taskGroups;
	}
}
