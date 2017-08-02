package org.springframework.cloud.deployer.spi.nomad.maven;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.springframework.cloud.deployer.resource.maven.MavenResource;
import org.springframework.cloud.deployer.spi.nomad.NomadDeployerProperties;

public class MavenSupportTest implements MavenSupport {

	@Test
	public void testToURIString() {
		NomadDeployerProperties deployerProperties = new NomadDeployerProperties();
		deployerProperties.setDeployerHost("localhost");
		deployerProperties.setDeployerPort(9393);
		MavenResource resource = MavenResource
				.parse("org.springframework.cloud.stream.app:time-source-kafka:1.1.0.RELEASE");

		String uri = toURIString(resource, deployerProperties);

		assertThat(uri).isEqualTo(
				"http://localhost:9393/resources/maven/org.springframework.cloud.stream.app/time-source-kafka-1.1.0.RELEASE.jar");
	}

	@Test
	public void testToURIStringWithHttps() {
		NomadDeployerProperties deployerProperties = new NomadDeployerProperties();
		deployerProperties.setDeployerScheme("https");
		deployerProperties.setDeployerHost("192.168.1.10");
		deployerProperties.setDeployerPort(443);
		MavenResource resource = MavenResource
				.parse("org.springframework.cloud.stream.app:time-source-kafka:1.1.0.RELEASE");

		String uri = toURIString(resource, deployerProperties);

		assertThat(uri).isEqualTo(
				"https://192.168.1.10:443/resources/maven/org.springframework.cloud.stream.app/time-source-kafka-1.1.0.RELEASE.jar");
	}

	@Test
	public void testToURIStringWithClassifier() {
		NomadDeployerProperties deployerProperties = new NomadDeployerProperties();
		deployerProperties.setDeployerScheme("https");
		deployerProperties.setDeployerHost("192.168.1.10");
		deployerProperties.setDeployerPort(443);
		MavenResource resource = MavenResource
				.parse("org.springframework.cloud.stream.app:time-source-kafka:jar:exec:1.1.0.RELEASE");

		String uri = toURIString(resource, deployerProperties);

		assertThat(uri).isEqualTo(
				"https://192.168.1.10:443/resources/maven/org.springframework.cloud.stream.app/time-source-kafka-1.1.0.RELEASE-exec.jar");
	}

	@Test
	public void testToURIStringWithAuthentication() {
		NomadDeployerProperties deployerProperties = new NomadDeployerProperties();
		deployerProperties.setDeployerHost("192.168.1.10");
		deployerProperties.setDeployerPort(9393);
		deployerProperties.setDeployerUsername("test");
		deployerProperties.setDeployerPassword("password");
		MavenResource resource = MavenResource
				.parse("org.springframework.cloud.stream.app:time-source-kafka:jar:exec:1.1.0.RELEASE");

		String uri = toURIString(resource, deployerProperties);

		assertThat(uri).isEqualTo(
				"http://test:password@192.168.1.10:9393/resources/maven/org.springframework.cloud.stream.app/time-source-kafka-1.1.0.RELEASE-exec.jar");
	}
}
