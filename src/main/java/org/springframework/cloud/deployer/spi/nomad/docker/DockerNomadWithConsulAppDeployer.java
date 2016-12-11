package org.springframework.cloud.deployer.spi.nomad.docker;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.deployer.spi.app.AppStatus;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.nomad.NomadConsulAppInstanceStatus;
import org.springframework.cloud.deployer.spi.nomad.NomadDeployerProperties;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.health.model.Check;

import io.github.zanella.nomad.NomadClient;
import io.github.zanella.nomad.v1.jobs.models.JobAllocation;
import io.github.zanella.nomad.v1.jobs.models.JobSummary;
import io.github.zanella.nomad.v1.nodes.models.Service;
import io.github.zanella.nomad.v1.nodes.models.Task;

/**
 * Deployer responsible for deploying
 * {@link org.springframework.cloud.deployer.resource.docker.DockerResource} based applications
 * using the Nomad <a href="https://www.nomadproject.io/docs/drivers/docker.html">Docker</a> driver.
 * Consul is available and will be used to create a Service for discovery. See
 * https://www.nomadproject.io/docs/job-specification/service.html. Consul will also be used to help
 * determine app status using the Service's health checks.
 *
 * @author Donovan Muller
 */
public class DockerNomadWithConsulAppDeployer extends DockerNomadAppDeployer {

	private static final Logger logger = LoggerFactory.getLogger(DockerNomadWithConsulAppDeployer.class);

	private NomadClient client;
	private ConsulClient consul;
	private NomadDeployerProperties deployerProperties;

	public DockerNomadWithConsulAppDeployer(NomadClient client, ConsulClient consul,
			NomadDeployerProperties deployerProperties) {
		super(client, deployerProperties);

		this.client = client;
		this.consul = consul;
		this.deployerProperties = deployerProperties;
	}

	@Override
	public AppStatus status(String deploymentId) {
		Response<List<Check>> healthChecks = consul.getHealthChecksForService(deploymentId, QueryParams.DEFAULT);
		logger.debug("Health checks for '{}': {}", deploymentId, healthChecks);

		JobSummary job = getJobByName(deploymentId);
		if (job == null) {
			return AppStatus.of(deploymentId).build();
		}

		Check check = healthChecks.getValue().isEmpty() ? null : healthChecks.getValue().get(0);
		List<JobAllocation> allocations = getAllocationEvaluation(client, job);
		return buildAppStatus(deploymentId, check, allocations);
	}

	/**
	 * Build the {@link AppStatus} based on the Job allocations.
	 */
	protected AppStatus buildAppStatus(String id, Check check, List<JobAllocation> allocations) {
		AppStatus.Builder statusBuilder = AppStatus.of(id);
		allocations.forEach(allocation -> statusBuilder
				.with(new NomadConsulAppInstanceStatus(client.v1.allocation.getAllocation(allocation.getId()), check)));
		return statusBuilder.build();
	}

	@Override
	protected Task buildTask(AppDeploymentRequest request, String deploymentId) {
		Task task = super.buildTask(request, deploymentId);

		Service service = new Service();
		service.setName(deploymentId);
		service.setPortLabel("http");

		ArrayList<String> tags = new ArrayList<>();
		tags.addAll(createTags(deploymentId, request));
		if (exposeFabioRoute(request)) {
			tags.add(buildFabioUrlPrefix(request, deploymentId));
		}
		service.setTags(tags);

		Service.Check check = new Service.Check();
		check.setName(String.format("%s Health Check", deploymentId));
		check.setType("http");
		check.setProtocol("http");
		check.setPath(deployerProperties.getCheckHttpPath());
		check.setInterval(milliToNanoseconds(deployerProperties.getCheckInterval()));
		check.setTimeout(milliToNanoseconds(deployerProperties.getCheckTimeout()));
		service.setChecks(Stream.of(check).collect(toList()));

		task.setServices(Stream.of(service).collect(toList()));
		return task;
	}
}
