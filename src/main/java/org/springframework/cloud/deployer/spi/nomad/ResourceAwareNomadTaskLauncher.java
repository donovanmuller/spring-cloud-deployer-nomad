package org.springframework.cloud.deployer.spi.nomad;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.deployer.resource.maven.MavenResource;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.core.RuntimeEnvironmentInfo;
import org.springframework.cloud.deployer.spi.nomad.docker.DockerNomadTaskLauncher;
import org.springframework.cloud.deployer.spi.nomad.maven.MavenNomadTaskLauncher;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.cloud.deployer.spi.task.TaskStatus;

public class ResourceAwareNomadTaskLauncher implements TaskLauncher {

	private static final Logger logger = LoggerFactory.getLogger(ResourceAwareNomadTaskLauncher.class);

	private final DockerNomadTaskLauncher dockerTaskLauncher;
	private final MavenNomadTaskLauncher mavenTaskLauncher;

	public ResourceAwareNomadTaskLauncher(DockerNomadTaskLauncher dockerTaskLauncher,
			MavenNomadTaskLauncher mavenTaskLauncher) {
		this.dockerTaskLauncher = dockerTaskLauncher;
		this.mavenTaskLauncher = mavenTaskLauncher;
	}

	@Override
	public String launch(AppDeploymentRequest request) {
		String taskId;

		try {
			if (request.getResource() instanceof MavenResource) {
				taskId = mavenTaskLauncher.launch(request);
			}
			else {
				taskId = dockerTaskLauncher.launch(request);
			}
		}
		catch (Exception e) {
			logger.error(String.format("Error deploying application deployment request: %s", request), e);
			throw e;
		}

		return taskId;
	}

	@Override
	public void cancel(String taskId) {
		dockerTaskLauncher.cancel(taskId);
	}

	@Override
	public TaskStatus status(String taskId) {
		return dockerTaskLauncher.status(taskId);
	}

	@Override
	public void cleanup(final String taskId) {
		dockerTaskLauncher.cancel(taskId);
	}

	@Override
	public void destroy(final String taskId) {
		dockerTaskLauncher.destroy(taskId);
	}

	@Override
	public RuntimeEnvironmentInfo environmentInfo() {
		return dockerTaskLauncher.environmentInfo();
	}
}
