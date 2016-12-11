package org.springframework.cloud.deployer.spi.nomad.maven;

import org.springframework.cloud.deployer.resource.maven.MavenResource;
import org.springframework.cloud.deployer.spi.nomad.NomadDeployerProperties;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author Donovan Muller
 */
public interface MavenSupport {

	default String toURIString(MavenResource mavenResource, NomadDeployerProperties deployerProperties) {
		//@formatter:off
		return UriComponentsBuilder.newInstance()
			.scheme(deployerProperties.getDeployerScheme())
			.host(deployerProperties.getDeployerHost())
			.port(deployerProperties.getDeployerPort())
			.pathSegment("resources", "maven",
				mavenResource.getGroupId(),
				mavenResource.getFilename())
			.toUriString();
		//@formatter:on
	}
}
