package org.springframework.cloud.deployer.spi.nomad.maven;

import org.springframework.cloud.deployer.resource.maven.MavenResource;
import org.springframework.cloud.deployer.spi.nomad.NomadDeployerProperties;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author Donovan Muller
 */
public interface MavenSupport {

	default String toURIString(MavenResource mavenResource, NomadDeployerProperties deployerProperties) {
		String host = deployerProperties.getDeployerHost();
		if (!StringUtils.isEmpty(deployerProperties.getDeployerUsername())) {
			host = String.format("%s:%s@%s",
				deployerProperties.getDeployerUsername(),
				deployerProperties.getDeployerPassword(),
				host);
		}

		//@formatter:off
		return UriComponentsBuilder.newInstance()
			.scheme(deployerProperties.getDeployerScheme())
			.host(host)
			.port(deployerProperties.getDeployerPort())
			.pathSegment("resources", "maven",
				mavenResource.getGroupId(),
				mavenResource.getFilename())
			.build()
			.toUriString();
		//@formatter:on
	}
}
