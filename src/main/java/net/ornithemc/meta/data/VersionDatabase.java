/*
 * Copyright (c) 2019 FabricMC
 *
 * Modifications copyright (c) 2022 OrnitheMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.ornithemc.meta.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.ornithemc.meta.OrnitheMeta;
import net.ornithemc.meta.utils.MinecraftLauncherMeta;
import net.ornithemc.meta.utils.MavenPomParser;
import net.ornithemc.meta.utils.MavenMetadataParser;
import net.ornithemc.meta.utils.MavenMetadataParser.StableVersionIdentifier;
import net.ornithemc.meta.utils.VersionManifest;
import net.ornithemc.meta.web.LibraryUpgradesV3;
import net.ornithemc.meta.web.LibraryUpgradesV3.LibraryUpgrade;
import net.ornithemc.meta.web.models.*;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@JsonIgnoreProperties({"manifest"})
public class VersionDatabase {

	public static final String FABRIC_MAVEN_URL = "https://maven.fabricmc.net/";
	public static final String QUILT_MAVEN_URL = "https://maven.quiltmc.org/repository/release/";
	public static final String ORNITHE_MAVEN_URL = "https://maven.ornithemc.net/releases/";
	public static final String ORNITHE_MAVEN_DETAILS_URL = "https://maven.ornithemc.net/api/maven/details/releases/";
	public static final String ORNITHE_MAVEN_VERSIONS_URL = "https://maven.ornithemc.net/api/maven/versions/releases/";
	public static final String MINECRAFT_LIBRARIES_URL = "https://libraries.minecraft.net/";

	public static final MavenMetadataParser RAVEN_METADATA_PARSER = new MavenMetadataParser(ORNITHE_MAVEN_URL, "net.ornithemc", "raven");
	public static final MavenMetadataParser SPARROW_METADATA_PARSER = new MavenMetadataParser(ORNITHE_MAVEN_URL, "net.ornithemc", "sparrow");
	public static final MavenMetadataParser NESTS_METADATA_PARSER = new MavenMetadataParser(ORNITHE_MAVEN_URL, "net.ornithemc", "nests");
	public static final MavenMetadataParser FABRIC_LOADER_METADATA_PARSER = new MavenMetadataParser(FABRIC_MAVEN_URL, "net.fabricmc", "fabric-loader");
	public static final MavenMetadataParser QUILT_LOADER_METADATA_PARSER = new MavenMetadataParser(QUILT_MAVEN_URL, "org.quiltmc", "quilt-loader");
	public static final MavenMetadataParser INSTALLER_METADATA_PARSER = new MavenMetadataParser(ORNITHE_MAVEN_URL, "net.ornithemc", "ornithe-installer");

	private static final Pattern INVALID_FABRIC_LOADER_VERSIONS_GEN2 = Pattern.compile("^(?:0\\.(?:\\d|1[0-6])\\..+|0\\.17\\.[0-2])");
	private static final Pattern INVALID_QUILT_LOADER_VERSIONS_GEN2 = Pattern.compile("^(?:0\\.(?:\\d|1\\d|2[0-8])\\..+|0\\.29\\.[0-2])");

	public static ConfigV3 config;

	private static final String modifyForIntermediaryGeneration(String s, int generation) {
		return generation == 1 ? s : (s + "-gen" + generation);
	}

	private static final MavenMetadataParser generationalMavenMetadataParser(int generation, String groupId, String artifactId) {
		return new MavenMetadataParser(ORNITHE_MAVEN_URL, groupId, modifyForIntermediaryGeneration(artifactId, generation), generation <= config.stableIntermediaryGeneration);
	}

	private static final MavenPomParser generationalMavenPomParser(int generation, String groupId, String artifactId) {
		return new MavenPomParser(ORNITHE_MAVEN_URL, groupId, modifyForIntermediaryGeneration(artifactId, generation), generation <= config.stableIntermediaryGeneration);
	}

	public static final MavenMetadataParser intermediaryMetadataParser(int generation) {
		return generationalMavenMetadataParser(generation, "net.ornithemc", "calamus-intermediary");
	}

	public static final MavenMetadataParser featherMetadataParser(int generation) {
		return generationalMavenMetadataParser(generation, "net.ornithemc", "feather");
	}

	public static final MavenMetadataParser oslMetadataParser(int generation) {
		return generationalMavenMetadataParser(generation, "net.ornithemc", "osl");
	}

	public static final MavenPomParser oslPomParser(int generation) {
		return generationalMavenPomParser(generation, "net.ornithemc", "osl");
	}

	public static final MavenMetadataParser oslModuleMetadataParser(int generation, String module) {
		return new MavenMetadataParser(ORNITHE_MAVEN_URL, modifyForIntermediaryGeneration("net.ornithemc.osl", generation), module, generation <= config.stableIntermediaryGeneration);
	}

	public static Pattern invalidLoaderVersionsPattern(LoaderType loaderType) {
		switch (loaderType) {
		case FABRIC:
			return INVALID_FABRIC_LOADER_VERSIONS_GEN2;
		case QUILT:
			return INVALID_QUILT_LOADER_VERSIONS_GEN2;
		default:
			throw new IllegalStateException("no invalid loader versions pattern for loader type " + loaderType.getName());
		}
	}

	public static StableVersionIdentifier filterLoaderVersions(int generation, LoaderType loaderType) {
		return versions -> {
			boolean foundStableVersion = false;

			for (Iterator<? extends BaseVersion> it = versions.iterator(); it.hasNext(); ) {
				BaseVersion version = it.next();

				if (generation >= 2 && invalidLoaderVersionsPattern(loaderType).matcher(version.getVersion()).matches()) {
					it.remove();
				} else if (!foundStableVersion && isPublicLoaderVersion(loaderType, version)) {
					foundStableVersion = true;
					version.setStable(true);
				}
			}
		};
	}

	private static List<String> oslModules(int generation) {
		List<String> modules = new ArrayList<>();

		try {
			URL url = new URL(modifyForIntermediaryGeneration(ORNITHE_MAVEN_DETAILS_URL + "net/ornithemc/osl", generation));

			try (InputStreamReader input = new InputStreamReader(url.openStream())) {
				JsonNode json = OrnitheMeta.MAPPER.readTree(input);

				if (json.isObject()) {
					ObjectNode obj = (ObjectNode) json;
					JsonNode files = obj.get("files");

					if (files != null && files.isArray()) {
						ArrayNode arr = (ArrayNode) files;

						for (JsonNode file : arr) {
							if (file.isObject()) {
								JsonNode name = file.get("name");
								JsonNode type = file.get("type");

								if (name != null && name.isValueNode() && type != null && type.isValueNode()) {
									if (!Character.isDigit(name.asText().charAt(0)) && "DIRECTORY".equals(type.asText())) {
										modules.add(name.asText());
									}
								}
							}
						}
					}
				}
			}
		} catch (IOException e) {
		}

		return modules;
	}

	public final VersionManifest manifest;
	private final Int2ObjectMap<List<BaseVersion>> game;
	private final Int2ObjectMap<List<MavenVersion>> intermediary;
	private final Int2ObjectMap<List<MavenBuildGameVersion>> feather;
	private final Int2ObjectMap<List<MavenVersion>> osl;
	private final Int2ObjectMap<Map<String, List<MavenVersion>>> oslDependencies;
	private final Int2ObjectMap<Map<String, List<MavenVersion>>> oslModules;
	private final Int2ObjectMap<Map<LoaderType, List<MavenBuildVersion>>> loader;

	public IntermediaryGenerations intermediaryGenerations;
	public List<MavenBuildGameVersion> raven;
	public List<MavenBuildGameVersion> sparrow;
	public List<MavenBuildGameVersion> nests;
	public List<MavenUrlVersion> installer;
	public List<LibraryUpgrade> libraryUpgrades;

	private VersionDatabase() {
		this.manifest = new VersionManifest();
		this.game = new Int2ObjectOpenHashMap<>();
		this.intermediary = new Int2ObjectOpenHashMap<>();
		this.feather = new Int2ObjectOpenHashMap<>();
		this.osl = new Int2ObjectOpenHashMap<>();
		this.oslDependencies = new Int2ObjectOpenHashMap<>();
		this.oslModules = new Int2ObjectOpenHashMap<>();
		this.loader = new Int2ObjectOpenHashMap<>();
	}

	public static VersionDatabase generate() throws Exception {
		long start = System.currentTimeMillis();
		VersionDatabase database = new VersionDatabase();
		config = ConfigV3.load();
		for (int generation = 1; generation <= config.latestIntermediaryGeneration; generation++) {
			database.intermediary.put(generation, intermediaryMetadataParser(generation).getVersions(MavenVersion::new));
			database.feather.put(generation, featherMetadataParser(generation).getVersions(MavenBuildGameVersion::new));
			database.osl.put(generation, oslMetadataParser(generation).getVersions(MavenVersion::new));
			database.oslDependencies.put(generation, new HashMap<>());
			database.oslModules.put(generation, new HashMap<>());
			for (MavenVersion version : database.osl.get(generation)) {
				database.oslDependencies.get(generation).put(version.getVersion(), oslPomParser(generation).getDependencies(MavenVersion::new, version.getVersion(), v -> {
					return v.getMaven().startsWith("net.ornithemc.osl");
				}));
			}
			for (String module : oslModules(generation)) {
				database.oslModules.get(generation).put(module, oslModuleMetadataParser(generation, module).getVersions(MavenVersion::new));
			}
			database.loader.put(generation, new EnumMap<>(LoaderType.class));
			database.loader.get(generation).put(LoaderType.FABRIC, FABRIC_LOADER_METADATA_PARSER.getVersions(MavenBuildVersion::new, filterLoaderVersions(generation, LoaderType.FABRIC)));
			database.loader.get(generation).put(LoaderType.QUILT, QUILT_LOADER_METADATA_PARSER.getVersions(MavenBuildVersion::new, filterLoaderVersions(generation, LoaderType.QUILT)));
		}
		database.intermediaryGenerations = new IntermediaryGenerations(config.latestIntermediaryGeneration, config.stableIntermediaryGeneration);
		database.raven = RAVEN_METADATA_PARSER.getVersions(MavenBuildGameVersion::new);
		database.sparrow = SPARROW_METADATA_PARSER.getVersions(MavenBuildGameVersion::new);
		database.nests = NESTS_METADATA_PARSER.getVersions(MavenBuildGameVersion::new);
		database.installer = INSTALLER_METADATA_PARSER.getVersions(MavenUrlVersion::new);
		database.libraryUpgrades = LibraryUpgradesV3.get();
		database.loadMcData();
		OrnitheMeta.LOGGER.info("DB update took {}ms", System.currentTimeMillis() - start);
		return database;
	}

	private static boolean isPublicLoaderVersion(LoaderType type, BaseVersion version) {
		// Quilt publishes beta versions of their loader, filter those out
		return !(type == LoaderType.QUILT && version.getVersion().contains("-"));
	}

	private void loadMcData() throws IOException {
		if (intermediary.isEmpty() || feather.isEmpty()) {
			throw new RuntimeException("Mappings are empty");
		}

		Int2ObjectMap<MinecraftLauncherMeta> launcherMetas = new Int2ObjectOpenHashMap<>();

		for (int generation = 1; generation <= config.latestIntermediaryGeneration; generation++) {
			final int gen = generation;
			final MinecraftLauncherMeta launcherMeta = MinecraftLauncherMeta.getSortedMeta(gen);
			launcherMetas.put(generation, launcherMeta);
			intermediary.compute(generation, (key, value) -> {
				// Sorts in the order of minecraft release dates
				value = new ArrayList<>(value);
				value.sort(Comparator.comparingInt(o -> launcherMeta.getIndex(o.getVersionNoSide())));
				value.forEach(version -> version.setStable(true));

				// Remove entries that do not match a valid mc version.
				value.removeIf(o -> {
					if (launcherMeta.getVersions().stream().noneMatch(metaVersion -> metaVersion.getId().equals(o.getVersionNoSide()))) {
						OrnitheMeta.LOGGER.info("Removing {} from intermediary v3{} as it does not match a mc version", o.getVersion(), (gen < 1 ? "" : " gen" + gen));
						return true;
					}
					return false;
				});

				return value;
			});
			feather.compute(generation, (key, value) -> {
				// Sorts in the order of minecraft release dates
				value = new ArrayList<>(value);
				value.sort(Comparator.comparingInt(o -> launcherMeta.getIndex(o.getVersionNoSide())));
				value.forEach(version -> version.setStable(true));

				// Remove entries that do not match a valid mc version.
				value.removeIf(o -> {
					if (launcherMeta.getVersions().stream().noneMatch(metaVersion -> metaVersion.getId().equals(o.getVersionNoSide()))) {
						OrnitheMeta.LOGGER.info("Removing {} from v3 feather gen{} as it does not match a mc version", o.getGameVersion(), gen);
						return true;
					}
					return false;
				});

				return value;
			});

			List<String> minecraftVersions = new ArrayList<>();
			for (MavenVersion gameVersion : intermediary.get(generation)) {
				if (!minecraftVersions.contains(gameVersion.getVersionNoSide())) {
					minecraftVersions.add(gameVersion.getVersionNoSide());
				}
			}

			game.put(generation, minecraftVersions.stream().map(s -> new BaseVersion(s, launcherMeta.isStable(s))).collect(Collectors.toList()));
		}

		Function<String, Predicate<MavenBuildGameVersion>> p = src -> {
			return o -> {
				for (int generation = 1; generation <= config.latestIntermediaryGeneration; generation++) {
					if (launcherMetas.get(generation).getVersions().stream().anyMatch(metaVersion -> metaVersion.getId().equals(o.getVersionNoSide()))) {
						return false;
					}
				}
				OrnitheMeta.LOGGER.info("Removing {} from {} as it does not match a mc version", o.getGameVersion(), src);
				return true;
			};
		};
		Comparator<MavenBuildGameVersion> c = (v1, v2) -> {
			int i1 = Integer.MAX_VALUE;
			int i2 = Integer.MAX_VALUE;
			for (int generation = 1; generation <= config.latestIntermediaryGeneration; generation++) {
				MinecraftLauncherMeta launcherMeta = launcherMetas.get(generation);
				i1 = Math.min(i1, launcherMeta.getIndex(v1.getVersionNoSide()));
				i2 = Math.min(i2, launcherMeta.getIndex(v2.getVersionNoSide()));
			}
			return Integer.compare(i1, i2);
		};

		raven = new ArrayList<>(raven);
		raven.sort(c);
		raven.forEach(version -> version.setStable(true));
		sparrow = new ArrayList<>(sparrow);
		sparrow.sort(c);
		sparrow.forEach(version -> version.setStable(true));
		nests = new ArrayList<>(nests);
		nests.sort(c);
		nests.forEach(version -> version.setStable(true));

		raven.removeIf(p.apply("v3 raven"));
		sparrow.removeIf(p.apply("v3 sparrow"));
		nests.removeIf(p.apply("v3 nests"));
	}

	public List<BaseVersion> getGame(int generation) {
		return game.get(generation);
	}

	public List<MavenVersion> getIntermediary(int generation) {
		return intermediary.get(generation);
	}

	public List<MavenBuildGameVersion> getFeather(int generation) {
		return feather.get(generation);
	}

	public List<MavenVersion> getOsl(int generation) {
		return osl.get(generation);
	}

	public List<MavenVersion> getOslDependencies(int generation, String version) {
		return oslDependencies.get(generation).get(version);
	}

	public List<MavenVersion> getOslModule(int generation, String module) {
		return oslModules.get(generation).get(module);
	}

	public List<MavenBuildVersion> getLoader(int generation, LoaderType type) {
		return loader.get(generation).get(type).stream().filter(v -> isPublicLoaderVersion(type, v)).collect(Collectors.toList());
	}

	public List<MavenBuildVersion> getAllLoader(int generation, LoaderType type) {
		return Collections.unmodifiableList(loader.get(generation).get(type));
	}
}
