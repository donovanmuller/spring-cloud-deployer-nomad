package org.springframework.cloud.deployer.spi.nomad.maven;

import java.io.IOException;
import java.util.Properties;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.deployer.resource.maven.MavenResource;
import org.springframework.cloud.deployer.spi.nomad.NomadAutoConfiguration;
import org.springframework.cloud.deployer.spi.nomad.docker.DockerNomadAppDeployerIntegrationTests;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Integration tests for
 * {@link org.springframework.cloud.deployer.spi.nomad.maven.MavenNomadAppDeployer}.
 *
 * @author Donovan Muller
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {
		MavenNomadTaskLauncherIntegrationTests.TestApplication.class }, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, value = {
				"maven.remoteRepositories.spring.url=https://repo.spring.io/libs-snapshot",
				"spring.cloud.consul.enabled=false", "spring.cloud.deployer.nomad.restartPolicyAttempts=1" })
public class MavenNomadAppDeployerIntegrationTests extends DockerNomadAppDeployerIntegrationTests {

	@Test
	@Ignore("See https://github.com/donovanmuller/spring-cloud-deployer-nomad/issues/18")
	public void testCommandLineArgumentsPassing() {
	}

	@Test
	@Ignore("See https://github.com/donovanmuller/spring-cloud-deployer-nomad/issues/18")
	public void testApplicationPropertiesPassing() {
	}

	@Override
	protected Resource testApplication() {
		Properties properties = new Properties();
		try {
			properties.load(new ClassPathResource("integration-test-app.properties").getInputStream());
		}
		catch (IOException e) {
			throw new RuntimeException("Failed to determine which version of integration-test-app to use", e);
		}
		return new MavenResource.Builder(mavenProperties).groupId("org.springframework.cloud")
				.artifactId("spring-cloud-deployer-spi-test-app").version(properties.getProperty("version"))
				.classifier("exec").extension("jar").build();
	}

	/**
	 * We need a web container running so that the Maven artifacts can be downloaded from the
	 * "deployer server" (the test). You must set the
	 * <code>spring.cloud.deployer.nomad.nomadHost/Port</code> values to that matching a running and
	 * accessible Nomad environment. Also, adjust the <code>server.port</code> and the corresponding
	 * <code>spring.cloud.deployer.nomad.deployerPort</code> values when in a CI environment where
	 * parallel build might occur and the default port (9393) might be used already.
	 */
	@SpringBootApplication
	@Import(NomadAutoConfiguration.class)
	public static class TestApplication {

		public static void main(String[] args) {
			SpringApplication.run(MavenNomadTaskLauncherIntegrationTests.TestApplication.class, args);
		}
	}
}
