package org.springframework.cloud.deployer.spi.nomad;

/**
 * @author Donovan Muller
 */
public interface NomadDeploymentPropertyKeys {

	/**
	 * Job priority. See https://www.nomadproject.io/docs/jobspec/json.html#Priority
	 */
	String JOB_PRIORITY = "spring.cloud.deployer.nomad.job.priority";

	/**
	 * A flag to indicate whether the tags/labels that enable Fabio to configure routing are added
	 * or not. Specify a value of <code>true</code> to enable adding the URL prefix tags. See
	 * https://github.com/eBay/fabio/wiki/Quickstart
	 */
	String NOMAD_EXPOSE_VIA_FABIO = "spring.cloud.deployer.nomad.fabio.expose";

	/**
	 * The hostname that will be exposed via Fabio. If no hostname is provided, the deploymentId
	 * will be used. See https://github.com/eBay/fabio/wiki/Quickstart
	 */
	String NOMAD_FABIO_ROUTE_HOSTNAME = "spring.cloud.deployer.nomad.fabio.route.hostname";

	/**
	 * The <a href="https://www.nomadproject.io/docs/jobspec/json.html#CPU">CPU</a> required in MHz.
	 */
	String NOMAD_RESOURCES_CPU = "spring.cloud.deployer.nomad.resources.cpu";

	/**
	 * The <a href="https://www.nomadproject.io/docs/jobspec/json.html#MemoryMB">memory</a> required
	 * in MB.
	 */
	String NOMAD_RESOURCES_MEMORY = "spring.cloud.deployer.nomad.resources.memory";

	/**
	 * The <a href="https://www.nomadproject.io/docs/jobspec/json.html#DiskMB">disk</a> required in
	 * MB.
	 */
	String NOMAD_RESOURCES_DISK = "spring.cloud.deployer.nomad.resources.disk";
}
