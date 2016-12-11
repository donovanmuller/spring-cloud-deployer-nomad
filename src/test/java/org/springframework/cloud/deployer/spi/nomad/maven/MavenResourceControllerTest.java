package org.springframework.cloud.deployer.spi.nomad.maven;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.deployer.resource.maven.MavenResource;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = MavenResourceControllerTest.Config.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = "spring.cloud.deployer.nomad.deployerHost=localhost")
public class MavenResourceControllerTest {

	@Autowired
	private MockMvc mvc;

	@MockBean
	private MavenResourceResolver mavenResourceResolver;

	@Test
	public void testStreamingSnapshot() throws Exception {
		MavenResource mavenResource = mock(MavenResource.class);
		when(mavenResource.getFile()).thenReturn(new ClassPathResource("test-app-1.0-SNAPSHOT.jar").getFile());
		when(mavenResourceResolver.resolveUri("io.switchbit:test-app:1.0.0-SNAPSHOT"))
			.thenReturn(mavenResource);

		this.mvc.perform(get("/resources/maven/io.switchbit/test-app-1.0.0-SNAPSHOT.jar"))
			.andExpect(status().isOk());
	}

	@Test
	public void testStreamingRelease() throws Exception {
		MavenResource mavenResource = mock(MavenResource.class);
		when(mavenResource.getFile()).thenReturn(new ClassPathResource("test-app-1.0-SNAPSHOT.jar").getFile());
		when(mavenResourceResolver.resolveUri("io.switchbit:test-app:1.0.0"))
			.thenReturn(mavenResource);

		this.mvc.perform(get("/resources/maven/io.switchbit/test-app-1.0.0.jar"))
			.andExpect(status().isOk());
	}

	@Test
	public void testStreamingExecClassifier() throws Exception {
		MavenResource mavenResource = mock(MavenResource.class);
		when(mavenResource.getFile()).thenReturn(new ClassPathResource("test-app-1.0-SNAPSHOT.jar").getFile());
		when(mavenResourceResolver.resolveUri("org.springframework.cloud:spring-cloud-deployer-spi-test-app:jar:exec:1.1.1.RELEASE"))
			.thenReturn(mavenResource);

		this.mvc.perform(get("/resources/maven/org.springframework.cloud/spring-cloud-deployer-spi-test-app-1.1.1.RELEASE-exec.jar"))
			.andExpect(status().isOk());
	}

	@SpringBootApplication
	public static class Config {

		@Bean
		public MavenResourceController mavenResourceController(MavenResourceResolver mavenResourceResolver) {
			return new MavenResourceController(mavenResourceResolver);
		}
	}
}
