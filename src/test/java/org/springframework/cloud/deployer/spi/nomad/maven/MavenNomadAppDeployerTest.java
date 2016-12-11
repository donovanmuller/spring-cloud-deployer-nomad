package org.springframework.cloud.deployer.spi.nomad.maven;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.core.io.Resource;

public class MavenNomadAppDeployerTest {

	@Test
	public void testToSpringApplicationJson() {
		Map<String, String> definitionProperties = new HashMap<>();
		definitionProperties.put("definitionProperty1", "definitionValue1");
		definitionProperties.put("definitionProperty2", "definitionValue2");
		List<String> commandLineArguments = Stream.of("cmdLineProp1=cmdLineVal1").collect(Collectors.toList());
		definitionProperties.put("definitionProperty1", "definitionValue1");
		definitionProperties.put("definitionProperty2", "definitionValue2");
		AppDeploymentRequest request = new AppDeploymentRequest(new AppDefinition("test-app", definitionProperties),
				mock(Resource.class), null, commandLineArguments);

		String springApplicationJson = new MavenNomadAppDeployer(null, null).toSpringApplicationJson(request);

		assertThat(springApplicationJson).isEqualToIgnoringWhitespace("{" +
			"\"cmdLineProp1\":\"cmdLineVal1\"," +
			"\"server.port\":\"${NOMAD_PORT_http}\"," +
			"\"definitionProperty2\":\"definitionValue2\"," +
			"\"definitionProperty1\":\"definitionValue1\"" +
			"}");
	}
}
