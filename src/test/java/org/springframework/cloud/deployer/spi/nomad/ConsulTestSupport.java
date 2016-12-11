package org.springframework.cloud.deployer.spi.nomad;

import org.springframework.boot.Banner;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.consul.ConditionalOnConsulEnabled;
import org.springframework.cloud.consul.ConsulAutoConfiguration;
import org.springframework.cloud.deployer.resource.maven.MavenProperties;
import org.springframework.cloud.deployer.spi.test.junit.AbstractExternalResourceTestSupport;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import com.ecwid.consul.v1.ConsulClient;

/**
 * JUnit {@link org.junit.Rule} that detects the fact that a Consul instance is available.
 *
 * @author Donovan Muller
 */
public class ConsulTestSupport extends AbstractExternalResourceTestSupport<ConsulClient> {

	private ConfigurableApplicationContext context;

	public ConsulTestSupport() {
		super("CONSUL");
	}

	@Override
	protected void cleanupResource() throws Exception {
		context.close();
	}

	@Override
	protected void obtainResource() throws Exception {
		//@formatter:off
		context = new SpringApplicationBuilder(Config.class)
			.bannerMode(Banner.Mode.OFF)
			.web(false)
			.run();
		//@formatter:on
		resource = context.getBean(ConsulClient.class);
		resource.getAgentSelf().getConsulLastContact();
	}

	@TestConfiguration
	@Import({ ConsulAutoConfiguration.class, NomadAutoConfiguration.class })
	@ConditionalOnConsulEnabled
	public static class Config {

		@Bean
		public MavenProperties mavenProperties() {
			return new MavenProperties();
		}
	}
}
