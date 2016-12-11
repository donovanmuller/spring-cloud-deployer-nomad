package org.springframework.cloud.deployer.spi.nomad.docker;

import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.deployer.resource.docker.DockerResource;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.nomad.NomadAutoConfiguration;
import org.springframework.cloud.deployer.spi.nomad.NomadTestSupport;
import org.springframework.cloud.deployer.spi.test.AbstractAppDeployerIntegrationTests;
import org.springframework.cloud.deployer.spi.test.Timeout;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Integration tests for
 * {@link org.springframework.cloud.deployer.spi.nomad.docker.DockerNomadAppDeployer}.
 *
 * @author Donovan Muller
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = NomadAutoConfiguration.class, value = { "spring.cloud.consul.enabled=false",
		"spring.cloud.deployer.nomad.restartPolicyAttempts=1" })
public class DockerNomadAppDeployerIntegrationTests extends AbstractAppDeployerIntegrationTests {

	@ClassRule
	public static NomadTestSupport nomadAvailable = new NomadTestSupport();

	@Autowired
	private AppDeployer appDeployer;

	@Test
	@Ignore("Skipping, test is flaky...")
	public void testDeployingStateCalculationAndCancel() {
	}

	@Test
	@Ignore("See https://github.com/donovanmuller/spring-cloud-deployer-nomad/issues/18")
	public void testCommandLineArgumentsPassing() {
	}

	@Test
	@Ignore("See https://github.com/donovanmuller/spring-cloud-deployer-nomad/issues/18")
	public void testApplicationPropertiesPassing() {
	}

	@Override
	protected AppDeployer appDeployer() {
		return appDeployer;
	}

	@Override
	protected String randomName() {
		return "app-" + System.currentTimeMillis();
	}

	@Override
	protected Timeout deploymentTimeout() {
		return new Timeout(36, 5000);
	}

	@Override
	protected Resource testApplication() {
		return new DockerResource("springcloud/spring-cloud-deployer-spi-test-app:latest");
	}
}
