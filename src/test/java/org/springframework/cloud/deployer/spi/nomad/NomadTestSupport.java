package org.springframework.cloud.deployer.spi.nomad;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.stream.test.junit.AbstractExternalResourceTestSupport;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.zanella.nomad.NomadClient;

/**
 * JUnit {@link org.junit.Rule} that detects the fact that a Nomad installation is
 * available.
 *
 * @author Donovan Muller
 */
public class NomadTestSupport extends AbstractExternalResourceTestSupport<NomadClient> {

	private ConfigurableApplicationContext context;

	protected NomadTestSupport() {
		super("NOMAD");
	}

	@Override
	protected void cleanupResource() throws Exception {
		context.close();
	}

	@Override
	protected void obtainResource() throws Exception {
		context = new SpringApplicationBuilder(Config.class).web(false).run();
		resource = context.getBean(NomadClient.class);
		resource.v1.status.getLeader();
	}

	@Configuration
	@EnableAutoConfiguration
	@EnableConfigurationProperties(NomadDeployerProperties.class)
	public static class Config {

		@Autowired
		private NomadDeployerProperties properties;

		@Bean
		public NomadClient nomadClient() {
			return new NomadClient(properties.getNomadHost(), properties.getNomadPort());
		}
	}
}
