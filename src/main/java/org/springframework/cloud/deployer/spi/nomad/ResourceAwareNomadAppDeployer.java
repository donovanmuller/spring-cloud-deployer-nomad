package org.springframework.cloud.deployer.spi.nomad;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.deployer.resource.maven.MavenResource;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.app.AppStatus;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.core.RuntimeEnvironmentInfo;
import org.springframework.cloud.deployer.spi.nomad.docker.DockerNomadAppDeployer;
import org.springframework.cloud.deployer.spi.nomad.maven.MavenNomadAppDeployer;

public class ResourceAwareNomadAppDeployer implements AppDeployer {

	private static final Logger logger = LoggerFactory.getLogger(ResourceAwareNomadAppDeployer.class);

	private DockerNomadAppDeployer dockerAppDeployer;
	private MavenNomadAppDeployer mavenAppDeployer;

	public ResourceAwareNomadAppDeployer(DockerNomadAppDeployer dockerAppDeployer,
			MavenNomadAppDeployer mavenAppDeployer) {
		this.dockerAppDeployer = dockerAppDeployer;
		this.mavenAppDeployer = mavenAppDeployer;
	}

	@Override
	public String deploy(AppDeploymentRequest request) {
		String appId;

		try {
			if (request.getResource() instanceof MavenResource) {
				appId = mavenAppDeployer.deploy(request);
			}
			else {
				appId = dockerAppDeployer.deploy(request);
			}
		}
		catch (Exception e) {
			logger.error(String.format("Error deploying application deployment request: %s", request), e);
			throw e;
		}

		return appId;
	}

	@Override
	public void undeploy(String appId) {
		dockerAppDeployer.undeploy(appId);
	}

	@Override
	public AppStatus status(String appId) {
		return dockerAppDeployer.status(appId);
	}

	@Override
	public RuntimeEnvironmentInfo environmentInfo() {
		return dockerAppDeployer.environmentInfo();
	}
}
