package org.springframework.cloud.deployer.spi.nomad.maven;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Stream;

import org.springframework.cloud.deployer.resource.maven.MavenResource;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.nomad.NomadDeployerProperties;
import org.springframework.cloud.deployer.spi.nomad.NomadDeploymentPropertyKeys;
import org.springframework.cloud.deployer.spi.nomad.docker.DockerNomadTaskLauncher;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.util.StringUtils;

import io.github.zanella.nomad.NomadClient;
import io.github.zanella.nomad.v1.nodes.models.Resources;
import io.github.zanella.nomad.v1.nodes.models.Task;

/**
 * Deployer responsible for deploying
 * {@link org.springframework.cloud.deployer.resource.maven.MavenResource} based Task using the
 * Nomad <a href="https://www.nomadproject.io/docs/drivers/java.html">Java</a> driver.
 *
 * @author Donovan Muller
 */
public class MavenNomadTaskLauncher extends DockerNomadTaskLauncher implements TaskLauncher, MavenSupport {

	private final NomadDeployerProperties deployerProperties;

	public MavenNomadTaskLauncher(NomadClient nomadClient, NomadDeployerProperties deployerProperties) {
		super(nomadClient, deployerProperties);

		this.deployerProperties = deployerProperties;
	}

	@Override
	protected Task buildTask(AppDeploymentRequest request, String taskId) {
		Task.TaskBuilder taskBuilder = Task.builder();
		taskBuilder.name(taskId);
		taskBuilder.driver("java");

		Task.Config.ConfigBuilder configBuilder = Task.Config.builder();
		MavenResource resource = (MavenResource) request.getResource();

		configBuilder
				.jarPath(String.format("%s/%s", deployerProperties.getArtifactDestination(), resource.getFilename()));
		configBuilder.jvmOptions(new ArrayList<>(StringUtils.commaDelimitedListToSet(request.getDeploymentProperties()
				.getOrDefault(NomadDeploymentPropertyKeys.NOMAD_JAVA_OPTS, deployerProperties.getJavaOpts()))));

		taskBuilder.resources(new Resources(getCpuResource(deployerProperties, request),
				getMemoryResource(deployerProperties, request),
				// deprecated, use
				// org.springframework.cloud.deployer.spi.nomad.NomadDeployerProperties.EphemeralDisk
				null, 0, null));

		HashMap<String, String> env = new HashMap<>();
		env.putAll(getAppEnvironmentVariables(request));
		env.putAll(arrayToMap(deployerProperties.getEnvironmentVariables()));

		configBuilder.args(createCommandLineArguments(request));
		taskBuilder.config(configBuilder.build());

		// TODO support checksum:
		// https://github.com/donovanmuller/spring-cloud-deployer-nomad/issues/19
		// see:
		// https://www.nomadproject.io/docs/job-specification/artifact.html#download-and-verify-checksums
		taskBuilder.artifacts(
				Stream.of(new Task.Artifacts(toURIString((MavenResource) request.getResource(), deployerProperties),
						deployerProperties.getArtifactDestination(), null)).collect(toList()));

		taskBuilder.logConfig(new Task.LogConfig(deployerProperties.getLoggingMaxFiles(),
				deployerProperties.getLoggingMaxFileSize()));

		return taskBuilder.build();
	}
}
