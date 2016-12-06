package org.springframework.cloud.deployer.spi.nomad;

import org.springframework.boot.Banner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.deployer.spi.test.junit.AbstractExternalResourceTestSupport;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;

import io.github.zanella.nomad.NomadClient;

/**
 * JUnit {@link org.junit.Rule} that detects the fact that a Nomad installation is available.
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
		context = new SpringApplicationBuilder(Config.class)
			.bannerMode(Banner.Mode.OFF)
			.web(false)
			.run();
		resource = context.getBean(NomadClient.class);
		resource.v1.status.getLeader();
	}

	@TestConfiguration
	@Import(NomadAutoConfiguration.class)
	@ConditionalOnProperty(value = "nomad.enabled", matchIfMissing = true)
	public static class Config {

	}
}
