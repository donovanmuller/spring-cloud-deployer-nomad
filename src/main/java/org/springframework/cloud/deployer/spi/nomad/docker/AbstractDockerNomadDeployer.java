package org.springframework.cloud.deployer.spi.nomad.docker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.nomad.AbstractNomadDeployer;
import org.springframework.cloud.deployer.spi.nomad.NomadDeployerProperties;
import org.springframework.cloud.deployer.spi.nomad.NomadDeploymentPropertyKeys;
import org.springframework.cloud.deployer.spi.util.ByteSizeUtils;
import org.springframework.util.StringUtils;

public abstract class AbstractDockerNomadDeployer extends AbstractNomadDeployer {

	protected EntryPointStyle determineEntryPointStyle(NomadDeployerProperties properties,
			AppDeploymentRequest request) {
		EntryPointStyle entryPointStyle = null;
		String deployProperty = request.getDeploymentProperties()
				.get(NomadDeploymentPropertyKeys.NOMAD_DOCKER_ENTRYPOINT_STYLE);
		if (deployProperty != null) {
			try {
				entryPointStyle = EntryPointStyle.valueOf(deployProperty.toLowerCase());
			}
			catch (IllegalArgumentException ignore) {
			}
		}
		if (entryPointStyle == null) {
			entryPointStyle = properties.getEntryPointStyle();
		}
		return entryPointStyle;
	}

	/**
	 * Volumes can be specified as deployer properties as well as app deployment properties.
	 * Deployment properties override deployer properties. <b>All</b> apps get deployer property
	 * defined volumes.
	 *
	 * See {@link NomadDeployerProperties#volumes} and
	 * {@link NomadDeploymentPropertyKeys#NOMAD_DOCKER_VOLUMES}
	 */
	protected List<String> createVolumes(NomadDeployerProperties properties, AppDeploymentRequest request) {
		List<String> volumes = new ArrayList<>();

		String volumesDeploymentProperty = request.getDeploymentProperties()
				.getOrDefault(NomadDeploymentPropertyKeys.NOMAD_DOCKER_VOLUMES, "");
		if (!StringUtils.isEmpty(volumesDeploymentProperty)) {
			Collections.addAll(volumes, volumesDeploymentProperty.split(","));
		}
		// only add volumes that have not already been added, based on the volume's name
		// i.e. allow provided deployment volumes to override deployer defined volumes
		volumes.addAll(properties.getVolumes().stream()
				.filter(volume -> volumes.stream()
						.noneMatch(existingVolume -> existingVolume.split(":")[0].equals(volume.split(":")[0])))
				.collect(Collectors.toList()));

		return volumes;
	}

	/**
	 * Get the CPU resource value from the following sources (in order of precedence):
	 *
	 * <ol>
	 * <li>{@link AppDeployer#CPU_PROPERTY_KEY} deployment property</li>
	 * <li>{@link NomadDeploymentPropertyKeys#NOMAD_RESOURCES_CPU} deployment property</li>
	 * <li>{@link NomadDeployerProperties.Resources#cpu} deployer property</li>
	 * </ol>
	 */
	protected Integer getCpuResource(NomadDeployerProperties properties, AppDeploymentRequest request) {
		return Integer.valueOf(request.getDeploymentProperties().getOrDefault(AppDeployer.CPU_PROPERTY_KEY,
				request.getDeploymentProperties().getOrDefault(NomadDeploymentPropertyKeys.NOMAD_RESOURCES_CPU,
						properties.getResources().getCpu())));
	}

	/**
	 * Get the memory resource value from the following sources (in order of precedence):
	 *
	 * <ol>
	 * <li>{@link AppDeployer#MEMORY_PROPERTY_KEY} deployment property</li>
	 * <li>{@link NomadDeploymentPropertyKeys#NOMAD_RESOURCES_MEMORY} deployment property</li>
	 * <li>{@link NomadDeployerProperties.Resources#memory} deployer property</li>
	 * </ol>
	 */
	protected Integer getMemoryResource(NomadDeployerProperties properties, AppDeploymentRequest request) {
		Integer memory = getCommonDeployerMemory(request);
		return memory != null ? memory
				: Integer.valueOf(request.getDeploymentProperties().getOrDefault(
						NomadDeploymentPropertyKeys.NOMAD_RESOURCES_MEMORY, properties.getResources().getMemory()));
	}

	/**
	 * See
	 * org.springframework.cloud.deployer.spi.kubernetes.AbstractKubernetesDeployer#getCommonDeployerMemory
	 */
	private Integer getCommonDeployerMemory(AppDeploymentRequest request) {
		String mem = request.getDeploymentProperties().get(AppDeployer.MEMORY_PROPERTY_KEY);
		if (mem == null) {
			return null;
		}
		return Math.toIntExact(ByteSizeUtils.parseToMebibytes(mem));
	}
}
