package org.springframework.cloud.deployer.spi.nomad.maven;

import org.junit.runner.RunWith;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.deployer.spi.nomad.NomadWithConsulAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Integration tests for
 * {@link org.springframework.cloud.deployer.spi.nomad.maven.MavenNomadWithConsulAppDeployer}.
 *
 * @author Donovan Muller
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {
		MavenNomadWithConsulAppDeployerIntegrationTests.TestApplication.class }, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, value = {
				"maven.remoteRepositories.spring.url=https://repo.spring.io/libs-snapshot",
				"spring.cloud.deployer.nomad.restartPolicyAttempts=1" })
public class MavenNomadWithConsulAppDeployerIntegrationTests extends MavenNomadAppDeployerIntegrationTests {

	/**
	 * See
	 * org.springframework.cloud.deployer.spi.nomad.maven.MavenNomadAppDeployerIntegrationTests.{@link org.springframework.cloud.deployer.spi.nomad.maven.MavenNomadAppDeployerIntegrationTests.TestApplication}
	 */
	@SpringBootApplication
	@Import(NomadWithConsulAutoConfiguration.class)
	public static class TestApplication {

		public static void main(String[] args) {
			SpringApplication.run(MavenNomadTaskLauncherIntegrationTests.TestApplication.class, args);
		}
	}
}
