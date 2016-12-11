package org.springframework.cloud.deployer.spi.nomad.maven;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

public class ResourceChecksumTest {

	/**
	 * Verify this test by running the following in the <code>src/main/test/resources</code>
	 * directory:
	 * <p>
	 * <code>
	 * $ md5sum md5me.please
	 * 40968ae82c5cee65930aca062b16fc0e  md5me.please
	 * </code>
	 */
	@Test
	public void testGenerateMD5Checksum() {
		String checksum = new ResourceChecksum().generateMD5Checksum(new ClassPathResource("md5me.please"));

		assertThat(checksum).isEqualTo("40968ae82c5cee65930aca062b16fc0e");
	}
}
