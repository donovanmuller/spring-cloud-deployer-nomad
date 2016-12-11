package org.springframework.cloud.deployer.spi.nomad;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.util.Assert;

/**
 * @author Donovan Muller
 */
public interface NomadSupport {

	default Map<String, String> arrayToMap(String[] properties) {
		return arrayToMap(Arrays.asList(properties));
	}

	default Map<String, String> arrayToMap(List<String> properties) {
		Map<String, String> map = new HashMap<>();
		for (String property : properties) {
			String[] strings = property.split("=", 2);
			Assert.isTrue(strings.length == 2, "Invalid command line property declared: " + property);
			map.put(strings[0], strings[1]);
		}

		return map;
	}

	default Long milliToNanoseconds(Long milliseconds) {
		return TimeUnit.MILLISECONDS.toNanos(milliseconds);
	}
}
