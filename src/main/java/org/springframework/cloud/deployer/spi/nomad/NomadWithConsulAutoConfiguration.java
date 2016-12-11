package org.springframework.cloud.deployer.spi.nomad;

import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.cloud.consul.ConditionalOnConsulEnabled;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.nomad.docker.IndexingDockerNomadWithConsulAppDeployer;
import org.springframework.cloud.deployer.spi.nomad.maven.IndexingMavenNomadWithConsulAppDeployer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;

import com.ecwid.consul.v1.ConsulClient;

import io.github.zanella.nomad.NomadClient;

/**
 * @author Donovan Muller
 */
@Configuration
@ConditionalOnConsulEnabled
@Import(NomadAutoConfiguration.class)
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
public class NomadWithConsulAutoConfiguration {

	private NomadDeployerProperties deployerProperties;

	public NomadWithConsulAutoConfiguration(NomadDeployerProperties deployerProperties) {
		this.deployerProperties = deployerProperties;
	}

	@Bean
	public AppDeployer appDeployer(NomadClient nomadClient, ConsulClient consulClient) {
		return new ResourceAwareNomadAppDeployer(
				new IndexingDockerNomadWithConsulAppDeployer(nomadClient, consulClient, deployerProperties),
				new IndexingMavenNomadWithConsulAppDeployer(nomadClient, consulClient, deployerProperties));
	}
}
