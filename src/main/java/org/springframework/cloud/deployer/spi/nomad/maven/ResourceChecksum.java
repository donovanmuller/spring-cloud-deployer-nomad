package org.springframework.cloud.deployer.spi.nomad.maven;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.core.io.Resource;

public class ResourceChecksum {

	/**
	 * Generates a MDF5 hash of the provided {@link Resource} which represents the checksum.
	 *
	 * @param resource
	 * @return Resource checksum
	 */
	public String generateMD5Checksum(Resource resource) {
		try {
			File file = resource.getFile();
			String checksum = DigestUtils.md5Hex(new FileInputStream(file));

			return checksum;
		}
		catch (IOException e) {
			throw new RuntimeException("Could not read resource to generate checksum", e);
		}
	}
}
