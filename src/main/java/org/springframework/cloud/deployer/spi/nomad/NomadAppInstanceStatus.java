package org.springframework.cloud.deployer.spi.nomad;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.cloud.deployer.spi.app.AppInstanceStatus;
import org.springframework.cloud.deployer.spi.app.DeploymentState;

import io.github.zanella.nomad.v1.nodes.models.NodeAllocation;
import io.github.zanella.nomad.v1.nodes.models.Resources;

/**
 * @author Donovan Muller
 */
public class NomadAppInstanceStatus implements AppInstanceStatus {

	private NodeAllocation allocation;

	public NomadAppInstanceStatus(NodeAllocation allocation) {
		this.allocation = allocation;
	}

	@Override
	public String getId() {
		return allocation != null ? allocation.getName() : null;
	}

	@Override
	public DeploymentState getState() {
		return allocation != null ? mapState() : DeploymentState.unknown;
	}

	/**
	 * Client statuses based on
	 * https://github.com/hashicorp/nomad/blob/master/nomad/structs/structs.go#L2805
	 * @return the {@link DeploymentState} of deployment
	 */
	protected DeploymentState mapState() {
		switch (allocation.getClientStatus()) {

		case "pending":
			return DeploymentState.deploying;

		case "running":
			return DeploymentState.deployed;

		case "failed":
			return DeploymentState.failed;

		case "lost":
			return DeploymentState.failed;

		case "dead":
			return DeploymentState.failed;

		case "complete":
			return DeploymentState.undeployed;

		default:
			return DeploymentState.unknown;
		}
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> result = new LinkedHashMap<>();
		result.put("job_id", allocation.getJobId());
		result.put("evaluation_id", allocation.getEvalId());
		result.put("node_id", allocation.getNodeId());
		if (allocation.getJob() != null) {
			Map<String, String> meta = allocation.getJob().getMeta();
			if (meta != null && !meta.isEmpty()) {
				StringBuilder metaAttribute = new StringBuilder();
				for (Iterator<Map.Entry<String, String>> metaIterator = meta.entrySet().iterator(); metaIterator
						.hasNext();) {
					final Map.Entry<String, String> entry = metaIterator.next();
					metaAttribute.append(String.format("%s=%s%s", entry.getKey(), entry.getValue(),
							metaIterator.hasNext() ? ", " : ""));
				}
				result.put("meta", metaAttribute.toString());
			}
		}
		if (allocation.getResources() != null) {
			Resources resources = allocation.getResources();
			result.put("cpu", resources.getCpu().toString());
			result.put("memory_mb", resources.getMemoryMB().toString());
			result.put("disk_mb", resources.getDiskMB().toString());
			result.put("iops", resources.getIops().toString());
			if (!resources.getNetworks().isEmpty()) {
				for (int x = 0; x < resources.getNetworks().size(); x++) {
					Resources.Network network = resources.getNetworks().get(x);
					result.put(String.format("ip[%d]", x), network.getIp());
					result.put(String.format("mbits[%d]", x), network.getMBits().toString());
					result.put(String.format("device[%d]", x), network.getDevice());
					if (!network.getDynamicPorts().isEmpty()) {
						for (int y = 0; y < network.getDynamicPorts().size(); y++) {
							Resources.Network.DynamicPort dynamicPort = network.getDynamicPorts().get(y);
							result.put(String.format("dyanmic_port_label[%d][%d]", x, y), dynamicPort.getLabel());
							result.put(String.format("dyanmic_port_ip[%d][%d]", x, y),
									dynamicPort.getValue().toString());
						}
					}
				}
			}
		}
		return result;
	}
}
