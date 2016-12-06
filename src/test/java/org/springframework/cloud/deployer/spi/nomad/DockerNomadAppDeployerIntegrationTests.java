package org.springframework.cloud.deployer.spi.nomad;

import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.deployer.resource.docker.DockerResource;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.test.AbstractAppDeployerIntegrationTests;
import org.springframework.cloud.deployer.spi.test.Timeout;
import org.springframework.core.io.Resource;

/**
 * Integration tests for {@link DockerNomadAppDeployer}.
 *
 * @author Donovan Muller
 */
@SpringBootTest(classes = { NomadAutoConfiguration.class })
public class DockerNomadAppDeployerIntegrationTests extends AbstractAppDeployerIntegrationTests {

	@ClassRule
	public static NomadTestSupport nomadAvailable = new NomadTestSupport();

	@Autowired
	private AppDeployer appDeployer;

	@Autowired
	private NomadDeployerProperties deployerProperties;

	@Override
	protected AppDeployer appDeployer() {
		return appDeployer;
	}

	@Override
	public void testUnknownDeployment() {
		log.info("Testing {}...", "UnknownDeployment");
		super.testUnknownDeployment();
	}

	@Override
	public void testSimpleDeployment() {
		log.info("Testing {}...", "SimpleDeployment");
		super.testSimpleDeployment();
	}

	@Override
	public void testRedeploy() {
		log.info("Testing {}...", "Redeploy");
		super.testRedeploy();
	}

	@Override
	public void testDeployingStateCalculationAndCancel() {
		log.info("Testing {}...", "DeployingStateCalculationAndCancel");
		super.testDeployingStateCalculationAndCancel();
	}

	@Override
	@Test
	@Ignore("Ignore the failed deployment test until Consul health checks are implemented")
	public void testFailedDeployment() {
		log.info("Testing {}...", "FailedDeployment");
		deployerProperties.setRestartPolicyInterval(30000L);
		deployerProperties.setRestartPolicyDelay(10000L);
		deployerProperties.setRestartPolicyAttempts(2);
		deployerProperties.setRestartPolicyMode("fail");
		super.testFailedDeployment();
	}

	@Override
	protected String randomName() {
		return "app-" + System.currentTimeMillis();
	}

	@Override
	protected Timeout deploymentTimeout() {
		return new Timeout(36, 10000);
	}

	@Override
	protected Resource testApplication() {
		return new DockerResource("springcloud/spring-cloud-deployer-spi-test-app:1.0.3.RELEASE");
	}
}
