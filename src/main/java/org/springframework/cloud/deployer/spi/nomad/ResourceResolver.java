package org.springframework.cloud.deployer.spi.nomad;

import org.springframework.core.io.Resource;

public interface ResourceResolver {

	Resource resolveUri(String uri);
}
