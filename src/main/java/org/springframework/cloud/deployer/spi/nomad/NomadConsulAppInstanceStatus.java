package org.springframework.cloud.deployer.spi.nomad;

import java.util.Map;

import org.springframework.cloud.deployer.spi.app.DeploymentState;

import com.ecwid.consul.v1.health.model.Check;

import io.github.zanella.nomad.v1.nodes.models.NodeAllocation;

/**
 * @author Donovan Muller
 */
public class NomadConsulAppInstanceStatus extends NomadAppInstanceStatus {

	private NodeAllocation allocation;
	private Check check;

	public NomadConsulAppInstanceStatus(NodeAllocation allocation, Check check) {
		super(allocation);

		this.allocation = allocation;
		this.check = check;
	}

	@Override
	public DeploymentState getState() {
		return allocation != null ? mapState() : DeploymentState.unknown;
	}

	/**
	 * Consul health check statuses determine state. See {@link Check.CheckStatus}.
	 * @return the {@link DeploymentState} of deployment
	 */
	protected DeploymentState mapState() {
		DeploymentState deploymentState = super.mapState();
		if ((deploymentState == DeploymentState.deploying || deploymentState == DeploymentState.deployed)
				&& (check == null || check.getStatus() == Check.CheckStatus.CRITICAL)) {
			return DeploymentState.deploying;
		}
		else if (check != null) {
			switch (check.getStatus()) {

			case WARNING:
			case CRITICAL: {
				if (check.getOutput() == null) {
					return DeploymentState.deploying;
				}
				else {
					return DeploymentState.failed;
				}
			}

			case PASSING:
				return DeploymentState.deployed;

			case UNKNOWN:
			default:
				return DeploymentState.unknown;
			}
		}

		return DeploymentState.failed;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attributes = super.getAttributes();
		if (check != null) {
			attributes.put("health_check_name", check.getName());
			attributes.put("health_check_status", check.getStatus().name());
		}

		return attributes;
	}
}
