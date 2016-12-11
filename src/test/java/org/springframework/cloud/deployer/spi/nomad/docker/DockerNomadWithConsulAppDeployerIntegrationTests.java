package org.springframework.cloud.deployer.spi.nomad.docker;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.consul.ConsulAutoConfiguration;
import org.springframework.cloud.deployer.spi.nomad.ConsulTestSupport;
import org.springframework.cloud.deployer.spi.nomad.NomadWithConsulAutoConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Integration tests for
 * {@link org.springframework.cloud.deployer.spi.nomad.docker.DockerNomadWithConsulAppDeployer}.
 *
 * @author Donovan Muller
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = { ConsulAutoConfiguration.class, NomadWithConsulAutoConfiguration.class,
		DockerNomadWithConsulAppDeployerIntegrationTests.Config.class }, value = "spring.cloud.deployer.nomad.restartPolicyAttempts=1")
public class DockerNomadWithConsulAppDeployerIntegrationTests extends DockerNomadAppDeployerIntegrationTests {

	@ClassRule
	public static ConsulTestSupport consulAvailable = new ConsulTestSupport();
}
