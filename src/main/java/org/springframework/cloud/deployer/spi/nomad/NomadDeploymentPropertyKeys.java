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
	String NOMAD_RESOURCES_CPU = "spring.cloud.deployer.nomad.cpu";

	/**
	 * The <a href="https://www.nomadproject.io/docs/jobspec/json.html#MemoryMB">memory</a> required
	 * in MB.
	 */
	String NOMAD_RESOURCES_MEMORY = "spring.cloud.deployer.nomad.memory";

	/**
	 * Environment variables passed at deployment time. This is to cater for adding variables like
	 * JAVA_OPTS to supported deployer types.
	 */
	String NOMAD_ENVIRONMENT_VARIABLES = "spring.cloud.deployer.nomad.environmentVariables";

	/**
	 * See {@link org.springframework.cloud.deployer.spi.nomad.docker.EntryPointStyle}
	 */
	String NOMAD_DOCKER_ENTRYPOINT_STYLE = "spring.cloud.deployer.nomad.entryPointStyle";

	/**
	 * Comma separated list of Docker volumes in the form <code>host_path:container_path</code>.
	 * These volumes will be added to <b>all</b> apps deployed.
	 *
	 * See https://www.nomadproject.io/docs/drivers/docker.html#volumes
	 */
	String NOMAD_DOCKER_VOLUMES = "spring.cloud.deployer.nomad.volumes";

	/**
	 * An optional comma separated list of meta stanzas to add at the <b>Job</b> level.
	 * <code>spring.cloud.deployer.nomad.meta=streamVersion=1.0.0,streamDescription=A test stream</code>
	 */
	String NOMAD_META = "spring.cloud.deployer.nomad.meta";

	/**
	 * Ephemeral disk migrate flag. Valid values are <code>true</code> or <code>false</code>. See
	 * https://www.nomadproject.io/docs/job-specification/ephemeral_disk.html
	 */
	String NOMAD_EPHEMERAL_DISK_MIGRATE = "spring.cloud.deployer.nomad.ephemeralDisk.migrate";

	/**
	 * Ephemeral disk sticky flag. Valid values are <code>true</code> or <code>false</code>. See
	 * https://www.nomadproject.io/docs/job-specification/ephemeral_disk.html
	 */
	String NOMAD_EPHEMERAL_DISK_STICKY = "spring.cloud.deployer.nomad.ephemeralDisk.sticky";

	/**
	 * Ephemeral disk size in MB. E.g.
	 * <code>spring.cloud.deployer.nomad.ephemeralDisk.size=300</code>, which represents a size of
	 * 300MB. See https://www.nomadproject.io/docs/job-specification/ephemeral_disk.html
	 */
	String NOMAD_EPHEMERAL_DISK_SIZE = "spring.cloud.deployer.nomad.ephemeralDisk.size";

	/**
	 * A comma seperated list of Java options to pass to the JVM. See
	 * http://docs.spring.io/spring-cloud-dataflow/docs/current/reference/htmlsingle/index.html#getting-started-application-configuration
	 * for reference.
	 */
	String NOMAD_JAVA_OPTS = "spring.cloud.deployer.nomad.javaOpts";
}
