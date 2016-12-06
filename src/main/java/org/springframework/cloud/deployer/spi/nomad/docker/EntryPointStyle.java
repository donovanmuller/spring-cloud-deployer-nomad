package org.springframework.cloud.deployer.spi.nomad.docker;

/**
 * EntryPoint style used for a Docker image. Affects how app properties are passed in. For exec app
 * properties should be passed in as command line args, for shell they should be passed in as
 * environment variables and for boot they should be passed in using a SPRING_APPLICATION_JSON
 * environment variable.
 *
 * See
 * https://github.com/spring-cloud/spring-cloud-deployer-kubernetes/blob/master/src/main/java/org/springframework/cloud/deployer/spi/kubernetes/EntryPointStyle.java
 */
public enum EntryPointStyle {

	exec,
	shell,
	boot

}
