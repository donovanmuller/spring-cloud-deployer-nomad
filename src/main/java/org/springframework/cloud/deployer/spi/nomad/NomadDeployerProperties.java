package org.springframework.cloud.deployer.spi.nomad;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.deployer.spi.nomad.docker.EntryPointStyle;

/**
 * @author Donovan Muller
 */
@ConfigurationProperties(prefix = "spring.cloud.deployer.nomad")
public class NomadDeployerProperties {

	/**
	 * Configuration properties for {@link io.github.zanella.nomad.v1.nodes.models.Resources}. See
	 * https://www.nomadproject.io/docs/job-specification/resources.html
	 */
	public static class Resources {

		/**
		 * The <a href="https://www.nomadproject.io/docs/jobspec/json.html#CPU">CPU</a> required in
		 * MHz. Default is 1000MHz;
		 */
		private String cpu = "1000";

		/**
		 * The <a href="https://www.nomadproject.io/docs/jobspec/json.html#MemoryMB">memory</a>
		 * required in MB. Default is 512MB.
		 *
		 * N.B: This should change to a much lower value once we can set -Xmx via JAVA_OPTS.
		 *
		 * See https://github.com/spring-cloud/spring-cloud-stream-app-maven-plugin/issues/10 for
		 * support for JAVA_OPTS on the starter apps.
		 */
		private String memory = "512";

		/**
		 * The number of <a href="https://www.nomadproject.io/docs/jobspec/json.html#MBits">MBits</a> in
		 * bandwidth required.
		 */
		private Integer networkMBits = 10;

		public Resources() {
		}

		public Resources(String cpu, String memory, Integer networkMBits) {
			this.cpu = cpu;
			this.memory = memory;
			this.networkMBits = networkMBits;
		}

		public String getCpu() {
			return cpu;
		}

		public void setCpu(String cpu) {
			this.cpu = cpu;
		}

		public String getMemory() {
			return memory;
		}

		public void setMemory(String memory) {
			this.memory = memory;
		}

		public Integer getNetworkMBits() {
			return networkMBits;
		}

		public void setNetworkMBits(final Integer networkMBits) {
			this.networkMBits = networkMBits;
		}
	}

	/**
	 * Configuration property for {@link EphemeralDisk}. See
	 * https://www.nomadproject.io/docs/job-specification/ephemeral_disk.html
	 */
	public static class EphemeralDisk {

		private Boolean sticky = true;

		private Boolean migrate = true;

		private Integer size = 300;

		public EphemeralDisk() {
		}

		public EphemeralDisk(Boolean sticky, Boolean migrate, Integer size) {
			this.sticky = sticky;
			this.migrate = migrate;
			this.size = size;
		}

		public Boolean getSticky() {
			return sticky;
		}

		public void setSticky(Boolean sticky) {
			this.sticky = sticky;
		}

		public Boolean getMigrate() {
			return migrate;
		}

		public void setMigrate(Boolean migrate) {
			this.migrate = migrate;
		}

		public Integer getSize() {
			return size;
		}

		public void setSize(Integer size) {
			this.size = size;
		}
	}

	/**
	 * The hostname/IP address where a Nomad client is listening. Default is localhost.
	 */
	private String nomadHost = "localhost";

	/**
	 * The port where a Nomad client is listening. Default is 4646.
	 */
	private int nomadPort = 4646;

	/**
	 * The region to deploy apps into. Default to <code>global</code>. See
	 * https://www.nomadproject.io/docs/jobspec/json.html#Region
	 */
	private String region = "global";

	/**
	 * A list of datacenters that should be targeted for deployment. Default value is dc1. See
	 * https://www.nomadproject.io/docs/jobspec/json.html#Datacenters
	 */
	private List<String> datacenters = Stream.of("dc1").collect(Collectors.toList());

	/**
	 * The default job priority. Default value is 50. See
	 * https://www.nomadproject.io/docs/jobspec/json.html#Priority
	 */
	private Integer priority = 50;

	/**
	 * Common environment variables to set for any deployed app.
	 */
	private String[] environmentVariables = new String[] {};

	/**
	 * Flag to indicate whether an app should be exposed via
	 * <a href="https://github.com/eBay/fabio">Fabio</a>
	 */
	private boolean exposeViaFabio;

	/**
	 * The default route hostname to use for Fabio exposed apps. The default is to use a blank
	 * value, so that the <code>appId</code> alone is the host.
	 */
	private String defaultFabioRouteHostname = "";

	/**
	 * The <a href="https://www.nomadproject.io/docs/jobspec/json.html#Path">path</a> of the http
	 * endpoint which Consul will query to query the health
	 */
	private String checkHttpPath = "/health";

	/**
	 * This indicates the frequency of the health checks that Consul will perform. Specified in
	 * <b>milliseconds</b>. See https://www.nomadproject.io/docs/jobspec/json.html#Interval
	 */
	private Long checkInterval = 30000L;

	/**
	 * This indicates how long Consul will wait for a health check query to succeed. Specified in
	 * <b>milliseconds</b>. See https://www.nomadproject.io/docs/jobspec/json.html#Timeout
	 */
	private Long checkTimeout = 120000L;

	private Resources resources = new Resources();

	/**
	 * The <a href="https://www.nomadproject.io/docs/jobspec/json.html#DiskMB">disk</a> required in
	 * MB. Default is 200MB.
	 */
	private EphemeralDisk ephemeralDisk = new EphemeralDisk();

	/**
	 * The <a href="https://www.nomadproject.io/docs/jobspec/json.html#MaxFiles">maximum number of
	 * rotated files</a> Nomad will retain. The default is 1 log file retention size.
	 *
	 */
	private Integer loggingMaxFiles = 1;

	/**
	 * The <a href="https://www.nomadproject.io/docs/jobspec/json.html#MaxFileSizeMB">size</a> of
	 * each rotated file. The size is specified in MB. The default is 10MB max log file size.
	 */
	private Integer loggingMaxFileSize = 10;

	/**
	 * A duration to wait before restarting a task. See
	 * https://www.nomadproject.io/docs/jobspec/json.html#Delay. Specified in <b>milliseconds</b>.
	 * Default is 25000 milliseconds (25 seconds).
	 */
	private Long restartPolicyDelay = 25000L;

	/**
	 * The Interval begins when the first task starts and ensures that only Attempts number of
	 * restarts happens within it. See https://www.nomadproject.io/docs/jobspec/json.html#Interval.
	 * Specified in <b>milliseconds</b>. Default is 300000 milliseconds (300 seconds / 5 minutes).
	 */
	private Long restartPolicyInterval = 300000L;

	/**
	 * Attempts is the number of restarts allowed in an Interval. See
	 * https://www.nomadproject.io/docs/jobspec/json.html#Attempts. Default is 10 attempts within 5
	 * minutes (see {@link NomadDeployerProperties#restartPolicyInterval)
	 */
	private Integer restartPolicyAttempts = 10;

	/**
	 * Mode is given as a string and controls the behavior when the task fails more than Attempts
	 * times in an Interval. See https://www.nomadproject.io/docs/jobspec/json.html#Mode. Default
	 * value is <a href="https://www.nomadproject.io/docs/jobspec/json.html#delay">"delay"</a>.
	 * Possible values are:
	 *
	 * <ul>
	 * <li>delay (default)</li>
	 * <li>fail</li>
	 * </ul>
	 */
	private String restartPolicyMode = "delay";

	/**
	 * Entry point style used for the Docker image. To be used to determine how to pass in
	 * properties.
	 */
	private EntryPointStyle entryPointStyle = EntryPointStyle.exec;

	/**
	 * A comma separated list of host_path:container_path values. See
	 * https://www.nomadproject.io/docs/drivers/docker.html#volumes.
	 *
	 * E.g.
	 *
	 * <code>spring.cloud.deployer.nomad=/opt/data:/data,/opt/config:/config</code>
	 */
	private List<String> volumes = new ArrayList<>();

	public String getNomadHost() {
		return nomadHost;
	}

	public void setNomadHost(String nomadHost) {
		this.nomadHost = nomadHost;
	}

	public int getNomadPort() {
		return nomadPort;
	}

	public void setNomadPort(int nomadPort) {
		this.nomadPort = nomadPort;
	}

	public String getRegion() {
		return region;
	}

	public void setRegion(String region) {
		this.region = region;
	}

	public List<String> getDatacenters() {
		return datacenters;
	}

	public void setDatacenters(List<String> datacenters) {
		this.datacenters = datacenters;
	}

	public Integer getPriority() {
		return priority;
	}

	public void setPriority(Integer priority) {
		this.priority = priority;
	}

	public String[] getEnvironmentVariables() {
		return environmentVariables;
	}

	public void setEnvironmentVariables(String[] environmentVariables) {
		this.environmentVariables = environmentVariables;
	}

	public boolean isExposeViaFabio() {
		return exposeViaFabio;
	}

	public void setExposeViaFabio(boolean exposeViaFabio) {
		this.exposeViaFabio = exposeViaFabio;
	}

	public String getDefaultFabioRouteHostname() {
		return defaultFabioRouteHostname;
	}

	public void setDefaultFabioRouteHostname(String defaultFabioRouteHostname) {
		this.defaultFabioRouteHostname = defaultFabioRouteHostname;
	}

	public String getCheckHttpPath() {
		return checkHttpPath;
	}

	public void setCheckHttpPath(String checkHttpPath) {
		this.checkHttpPath = checkHttpPath;
	}

	public Resources getResources() {
		return resources;
	}

	public void setResources(final Resources resources) {
		this.resources = resources;
	}

	public EphemeralDisk getEphemeralDisk() {
		return ephemeralDisk;
	}

	public void setEphemeralDisk(final EphemeralDisk ephemeralDisk) {
		this.ephemeralDisk = ephemeralDisk;
	}

	public Integer getLoggingMaxFiles() {
		return loggingMaxFiles;
	}

	public void setLoggingMaxFiles(Integer loggingMaxFiles) {
		this.loggingMaxFiles = loggingMaxFiles;
	}

	public Integer getLoggingMaxFileSize() {
		return loggingMaxFileSize;
	}

	public void setLoggingMaxFileSize(Integer loggingMaxFileSize) {
		this.loggingMaxFileSize = loggingMaxFileSize;
	}

	public Long getCheckInterval() {
		return checkInterval;
	}

	public void setCheckInterval(Long checkInterval) {
		this.checkInterval = checkInterval;
	}

	public Long getCheckTimeout() {
		return checkTimeout;
	}

	public void setCheckTimeout(Long checkTimeout) {
		this.checkTimeout = checkTimeout;
	}

	public Long getRestartPolicyDelay() {
		return restartPolicyDelay;
	}

	public void setRestartPolicyDelay(Long restartPolicyDelay) {
		this.restartPolicyDelay = restartPolicyDelay;
	}

	public Long getRestartPolicyInterval() {
		return restartPolicyInterval;
	}

	public void setRestartPolicyInterval(Long restartPolicyInterval) {
		this.restartPolicyInterval = restartPolicyInterval;
	}

	public Integer getRestartPolicyAttempts() {
		return restartPolicyAttempts;
	}

	public void setRestartPolicyAttempts(Integer restartPolicyAttempts) {
		this.restartPolicyAttempts = restartPolicyAttempts;
	}

	public String getRestartPolicyMode() {
		return restartPolicyMode;
	}

	public void setRestartPolicyMode(String restartPolicyMode) {
		this.restartPolicyMode = restartPolicyMode;
	}

	public EntryPointStyle getEntryPointStyle() {
		return entryPointStyle;
	}

	public void setEntryPointStyle(EntryPointStyle entryPointStyle) {
		this.entryPointStyle = entryPointStyle;
	}

	public List<String> getVolumes() {
		return volumes;
	}

	public void setVolumes(List<String> volumes) {
		this.volumes = volumes;
	}

}
