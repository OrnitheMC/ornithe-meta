package net.ornithemc.meta.data;

import java.io.File;

import net.ornithemc.meta.OrnitheMeta;

public class ConfigV3 {

	private static final File FILE = new File("config-v3.json");

	public int latestIntermediaryGeneration = -1;
	public int stableIntermediaryGeneration = -1;

	public static ConfigV3 load() throws Exception {
		return OrnitheMeta.MAPPER.readValue(FILE, ConfigV3.class);
	}
}
