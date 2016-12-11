package org.springframework.cloud.deployer.spi.nomad.maven;

import org.springframework.cloud.deployer.resource.maven.MavenProperties;
import org.springframework.cloud.deployer.resource.maven.MavenResource;
import org.springframework.cloud.deployer.spi.nomad.ResourceResolver;
import org.springframework.util.Assert;

public class MavenResourceResolver implements ResourceResolver {

	private MavenProperties mavenProperties;

	public MavenResourceResolver(final MavenProperties mavenProperties) {
		this.mavenProperties = mavenProperties;
	}

	@Override
	public MavenResource resolveUri(String uri) {
		Assert.notNull(uri, "Maven URI cannot be null");

		return MavenResource.parse(uri, mavenProperties);
	}
}
