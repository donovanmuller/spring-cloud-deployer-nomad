package org.springframework.cloud.deployer.spi.nomad;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.nomad.docker.DockerNomadTaskLauncher;
import org.springframework.cloud.deployer.spi.nomad.docker.IndexingDockerNomadAppDeployer;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import io.github.zanella.nomad.NomadClient;

/**
 * @author Donovan Muller
 */
@Configuration
@EnableConfigurationProperties(NomadDeployerProperties.class)
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
public class NomadAutoConfiguration {

	@Autowired
	private NomadDeployerProperties deployerProperties;

	@Bean
	public NomadClient nomadClient() {
		return new NomadClient(deployerProperties.getNomadHost(), deployerProperties.getNomadPort());
	}

	@Bean
	public AppDeployer appDeployer(NomadClient nomadClient) {
		return new IndexingDockerNomadAppDeployer(nomadClient, deployerProperties);
	}

	@Bean
	public TaskLauncher taskLauncher(NomadClient nomadClient) {
		return new DockerNomadTaskLauncher(nomadClient, deployerProperties);
	}
}
