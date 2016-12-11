package org.springframework.cloud.deployer.spi.nomad.maven;

import java.io.FileInputStream;
import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.deployer.resource.maven.MavenResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MavenResourceController {

	private static final Logger logger = LoggerFactory.getLogger(MavenResourceController.class);

	private MavenResourceResolver mavenResourceResolver;

	public MavenResourceController(MavenResourceResolver mavenResourceResolver) {
		this.mavenResourceResolver = mavenResourceResolver;
	}

	@GetMapping("/resources/maven/{groupId}/{artifactId:[a-z-]+}-{version}.jar")
	public void stream(@PathVariable String groupId, @PathVariable String artifactId, @PathVariable String version,
			HttpServletResponse response) throws IOException {
		logger.debug("Getting resource for '{}/{}-{}.jar'", groupId, artifactId, version);

		MavenResource mavenResource;
		/**
		 * Perhaps identify the classifier better with regex. See
		 * {@link org.springframework.cloud.deployer.resource.maven.MavenResource#parse}
		 */
		if (version.contains("exec")) {
			mavenResource = mavenResourceResolver.resolveUri(String.format("%s:%s:jar:%s:%s", groupId, artifactId,
					version.substring(version.lastIndexOf("-") + 1), version.substring(0, version.lastIndexOf("-"))));
		}
		else {
			mavenResource = mavenResourceResolver.resolveUri(String.format("%s:%s:%s", groupId, artifactId, version));
		}

		response.addHeader("Content-disposition", String.format("attachment;filename=%s", mavenResource.getFilename()));
		response.setContentType("application/java-archive");
		IOUtils.copy(new FileInputStream(mavenResource.getFile()), response.getOutputStream());
		response.flushBuffer();
	}
}
