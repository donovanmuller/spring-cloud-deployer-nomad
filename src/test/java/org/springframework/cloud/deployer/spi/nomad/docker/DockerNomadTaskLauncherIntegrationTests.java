/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.deployer.spi.nomad.docker;

import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.deployer.resource.docker.DockerResource;
import org.springframework.cloud.deployer.spi.nomad.NomadAutoConfiguration;
import org.springframework.cloud.deployer.spi.nomad.NomadTestSupport;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.cloud.deployer.spi.test.AbstractTaskLauncherIntegrationTests;
import org.springframework.cloud.deployer.spi.test.Timeout;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Integration tests for
 * {@link org.springframework.cloud.deployer.spi.nomad.docker.DockerNomadTaskLauncher}.
 *
 * @author Donovan Muller
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = NomadAutoConfiguration.class, value = { "spring.cloud.consul.enabled=false",
		"spring.cloud.deployer.nomad.restartPolicyAttempts=1" })
public class DockerNomadTaskLauncherIntegrationTests extends AbstractTaskLauncherIntegrationTests {

	@ClassRule
	public static NomadTestSupport NomadAvailable = new NomadTestSupport();

	@Autowired
	private TaskLauncher taskLauncher;

	@Test
	@Ignore("Currently cancelling a task is akin to deleting the Job. "
			+ "Therefore the cancelled state cannot be determined.")
	public void testSimpleCancel() throws InterruptedException {
	}

	@Override
	protected TaskLauncher provideTaskLauncher() {
		return taskLauncher;
	}

	@Override
	protected Timeout deploymentTimeout() {
		return new Timeout(36, 5000);
	}

	@Override
	protected Resource testApplication() {
		return new DockerResource("springcloud/spring-cloud-deployer-spi-test-app:latest");
	}
}
