package org.springframework.cloud.deployer.spi.nomad;

import java.net.SocketException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.embedded.EmbeddedServletContainerInitializedEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.deployer.resource.maven.MavenProperties;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.nomad.docker.DockerNomadTaskLauncher;
import org.springframework.cloud.deployer.spi.nomad.docker.IndexingDockerNomadAppDeployer;
import org.springframework.cloud.deployer.spi.nomad.maven.IndexingMavenNomadAppDeployer;
import org.springframework.cloud.deployer.spi.nomad.maven.MavenNomadTaskLauncher;
import org.springframework.cloud.deployer.spi.nomad.maven.MavenResourceController;
import org.springframework.cloud.deployer.spi.nomad.maven.MavenResourceResolver;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.web.util.UriComponentsBuilder;

import io.github.zanella.nomad.NomadClient;

/**
 * @author Donovan Muller
 */
@Configuration
@EnableConfigurationProperties(NomadDeployerProperties.class)
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
public class NomadAutoConfiguration {

	private NomadDeployerProperties deployerProperties;

	public NomadAutoConfiguration(NomadDeployerProperties deployerProperties) {
		this.deployerProperties = deployerProperties;
	}

	@Bean
	public NomadClient nomadClient() {
		return new NomadClient(deployerProperties.getNomadHost(), deployerProperties.getNomadPort());
	}

	@Bean
	public AppDeployer appDeployer(NomadClient nomadClient) {
		return new ResourceAwareNomadAppDeployer(new IndexingDockerNomadAppDeployer(nomadClient, deployerProperties),
				new IndexingMavenNomadAppDeployer(nomadClient, deployerProperties));
	}

	@Bean
	public TaskLauncher taskLauncher(NomadClient nomadClient) {
		return new ResourceAwareNomadTaskLauncher(new DockerNomadTaskLauncher(nomadClient, deployerProperties),
				new MavenNomadTaskLauncher(nomadClient, deployerProperties));
	}

	@Bean
	public MavenResourceResolver mavenResourceResolver(MavenProperties mavenProperties) {
		return new MavenResourceResolver(mavenProperties);
	}

	@Bean
	@ConditionalOnMissingBean
	public MavenResourceController mavenResourceController(MavenResourceResolver mavenResourceResolver) {
		return new MavenResourceController(mavenResourceResolver);
	}

	@Configuration
	public static class NomadDeployerConfiguration {

		private static final Logger logger = LoggerFactory.getLogger(NomadDeployerConfiguration.class);

		private NomadDeployerProperties deployerProperties;

		public NomadDeployerConfiguration(NomadDeployerProperties deployerProperties) {
			this.deployerProperties = deployerProperties;
		}

		@EventListener
		public void onApplicationEvent(EmbeddedServletContainerInitializedEvent event) throws SocketException {
			if (deployerProperties.getDeployerPort() == null) {
				deployerProperties.setDeployerPort(event.getEmbeddedServletContainer().getPort());
			}

			logger.info("Using deployer base URI: '{}'",
					UriComponentsBuilder.newInstance().scheme(deployerProperties.getDeployerScheme())
							.host(deployerProperties.getDeployerHost()).port(deployerProperties.getDeployerPort())
							.build());
		}
	}
}
