package org.springframework.cloud.deployer.spi.nomad.docker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.nomad.NomadDeployerProperties;
import org.springframework.core.io.Resource;

import io.github.zanella.nomad.v1.nodes.models.Task;

public class AbstractDockerNomadDeployerTest {

	private AbstractDockerNomadDeployer deployer;

	@Before
	public void setup() {
		deployer = new AbstractDockerNomadDeployer() {

			@Override
			protected Integer getAppCount(final AppDeploymentRequest request) {
				return null;
			}

			@Override
			protected Task buildTask(final AppDeploymentRequest request, final String deploymentId) {
				return null;
			}
		};
	}

	@Test
	public void testCreateVolumesFromDeploymentProperty() {
		Map<String, String> deploymentProperties = new HashMap<>();
		deploymentProperties.put("spring.cloud.deployer.nomad.volumes", "/test:/data/test,/config:/data/config");
		AppDeploymentRequest request = new AppDeploymentRequest(new AppDefinition("test-app", null),
				mock(Resource.class), deploymentProperties);

		List<String> volumes = deployer.createVolumes(new NomadDeployerProperties(), request);

		assertThat(volumes).containsExactly("/test:/data/test", "/config:/data/config");
	}

	@Test
	public void testCreateVolumesFromDeployerProperty() {
		Map<String, String> deploymentProperties = new HashMap<>();
		AppDeploymentRequest request = new AppDeploymentRequest(new AppDefinition("test-app", null),
				mock(Resource.class), deploymentProperties);

		NomadDeployerProperties deployerProperties = new NomadDeployerProperties();
		deployerProperties.getVolumes().add("/test:/data/test");
		deployerProperties.getVolumes().add("/config:/data/config");

		List<String> volumes = deployer.createVolumes(deployerProperties, request);

		assertThat(volumes).containsExactly("/test:/data/test", "/config:/data/config");
	}

	@Test
	public void testCreateVolumesDeploymentPropertyOverridingDeployerProperty() {
		Map<String, String> deploymentProperties = new HashMap<>();
		deploymentProperties.put("spring.cloud.deployer.nomad.volumes", "/test:/data/test,/config:/data/config");
		AppDeploymentRequest request = new AppDeploymentRequest(new AppDefinition("test-app", null),
				mock(Resource.class), deploymentProperties);

		NomadDeployerProperties deployerProperties = new NomadDeployerProperties();
		deployerProperties.getVolumes().add("/test:/data/test");
		deployerProperties.getVolumes().add("/config:/data/config");

		List<String> volumes = deployer.createVolumes(deployerProperties, request);

		assertThat(volumes).containsExactly("/test:/data/test", "/config:/data/config");
	}
}
