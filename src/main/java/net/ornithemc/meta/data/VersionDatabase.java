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

import net.ornithemc.meta.utils.MinecraftLauncherMeta;
import net.ornithemc.meta.utils.PomDependencyParser;
import net.ornithemc.meta.utils.PomParser;
import net.ornithemc.meta.utils.VersionManifest;
import net.ornithemc.meta.web.models.BaseVersion;
import net.ornithemc.meta.web.models.LoaderType;
import net.ornithemc.meta.web.models.MavenBuildGameVersion;
import net.ornithemc.meta.web.models.MavenBuildVersion;
import net.ornithemc.meta.web.models.MavenUrlVersion;
import net.ornithemc.meta.web.models.MavenVersion;

import javax.xml.stream.XMLStreamException;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class VersionDatabase {

	public static final String FABRIC_MAVEN_URL = "https://maven.fabricmc.net/";
	public static final String QUILT_MAVEN_URL = "https://maven.quiltmc.org/repository/release/";
	public static final String ORNITHE_MAVEN_URL = "https://maven.ornithemc.net/releases/";
	public static final String ORNITHE_MAVEN_DETAILS_URL = "https://maven.ornithemc.net/api/maven/details/releases/";
	public static final String ORNITHE_MAVEN_VERSIONS_URL = "https://maven.ornithemc.net/api/maven/versions/releases/";
	public static final String MINECRAFT_LIBRARIES_URL = "https://libraries.minecraft.net/";

	public static final int LATEST_GENERATION = 2;
	public static final int LATEST_STABLE_GENERATION = 1;

	public static final PomParser RAVEN_PARSER = new PomParser(ORNITHE_MAVEN_URL + "net/ornithemc/raven/maven-metadata.xml");
	public static final PomParser SPARROW_PARSER = new PomParser(ORNITHE_MAVEN_URL + "net/ornithemc/sparrow/maven-metadata.xml");
	public static final PomParser NESTS_PARSER = new PomParser(ORNITHE_MAVEN_URL + "net/ornithemc/nests/maven-metadata.xml");
	public static final PomParser FABRIC_LOADER_PARSER = new PomParser(FABRIC_MAVEN_URL + "net/fabricmc/fabric-loader/maven-metadata.xml");
	public static final PomParser QUILT_LOADER_PARSER = new PomParser(QUILT_MAVEN_URL + "org/quiltmc/quilt-loader/maven-metadata.xml");
	public static final PomParser INSTALLER_PARSER = new PomParser(ORNITHE_MAVEN_URL + "net/ornithemc/ornithe-installer/maven-metadata.xml");
	public static final PomParser OSL_PARSER = new PomParser(ORNITHE_MAVEN_URL + "net/ornithemc/osl/maven-metadata.xml");
	public static final PomDependencyParser OSL_DEPENDENCY_PARSER = new PomDependencyParser(ORNITHE_MAVEN_URL + "net/ornithemc/osl");

	public static final PomParser intermediaryParser(int generation) {
		return generation == 1
			? new PomParser(ORNITHE_MAVEN_URL + "net/ornithemc/calamus-intermediary/maven-metadata.xml")
			: new PomParser(ORNITHE_MAVEN_URL + "net/ornithemc/calamus-intermediary-gen" + generation + "/maven-metadata.xml", generation <= LATEST_STABLE_GENERATION);
	}

	public static final PomParser featherParser(int generation) {
		return generation == 1
			? new PomParser(ORNITHE_MAVEN_URL + "net/ornithemc/feather/maven-metadata.xml")
			: new PomParser(ORNITHE_MAVEN_URL + "net/ornithemc/feather-gen" + generation + "/maven-metadata.xml", generation <= LATEST_STABLE_GENERATION);
	}

	public static final Map<String, PomParser> getOslModulePomParsers(List<MavenVersion> osl) {
		Set<String> versions = new HashSet<>();
		Map<String, PomParser> parsers = new HashMap<>();

		for (MavenVersion v : osl) {
			versions.add(v.getVersion());
		}

		try {
			URL url = new URL(ORNITHE_MAVEN_DETAILS_URL + "net/ornithemc/osl");

			try (InputStreamReader input = new InputStreamReader(url.openStream())) {
				JsonElement json = JsonParser.parseReader(input);

				if (json.isJsonObject()) {
					JsonObject obj = json.getAsJsonObject();
					JsonElement files = obj.get("files");

					if (files != null && files.isJsonArray()) {
						JsonArray arr = files.getAsJsonArray();

						for (JsonElement file : arr) {
							if (file.isJsonObject()) {
								JsonObject f = file.getAsJsonObject();
								JsonElement name = f.get("name");
								JsonElement type = f.get("type");

								if (name != null && name.isJsonPrimitive() && type != null && type.isJsonPrimitive()) {
									if (!versions.contains(name.getAsString()) && "DIRECTORY".equals(type.getAsString())) {
										parsers.put(name.getAsString(), new PomParser(ORNITHE_MAVEN_URL + "net/ornithemc/osl/" + name.getAsString() + "/maven-metadata.xml"));
									}
								}
							}
						}
					}
				}
			}
		} catch (IOException e) {
		}

		return parsers;
	}

	private final Int2ObjectMap<List<BaseVersion>> game;
	private final Int2ObjectMap<List<MavenVersion>> intermediary;
	private final Int2ObjectMap<List<MavenBuildGameVersion>> feather;
	private final Map<LoaderType, List<MavenBuildVersion>> loader;
	private final Map<String, List<MavenVersion>> oslDependencies;
	private final Map<String, List<MavenVersion>> oslModules;

	public final VersionManifest manifest = new VersionManifest();

	public List<MavenBuildGameVersion> raven;
	public List<MavenBuildGameVersion> sparrow;
	public List<MavenBuildGameVersion> nests;
	public List<MavenUrlVersion> installer;
	public List<MavenVersion> osl;

	private VersionDatabase() {
		this.game = new Int2ObjectOpenHashMap<>();
		this.intermediary = new Int2ObjectOpenHashMap<>();
		this.feather = new Int2ObjectOpenHashMap<>();
		this.loader = new EnumMap<>(LoaderType.class);
		this.oslDependencies = new HashMap<>();
		this.oslModules = new HashMap<>();
	}

	public static VersionDatabase generate() throws IOException, XMLStreamException {
		long start = System.currentTimeMillis();
		VersionDatabase database = new VersionDatabase();
		for (int generation = 1; generation <= LATEST_GENERATION; generation++) {
			database.intermediary.put(generation, intermediaryParser(generation).getMeta(MavenVersion::new, generation == 1 ? "net.ornithemc:calamus-intermediary:" : String.format("net.ornithemc:calamus-intermediary-gen%d:", generation)));
			database.feather.put(generation, featherParser(generation).getMeta(MavenBuildGameVersion::new, generation == 1 ? "net.ornithemc:feather:" : String.format("net.ornithemc:feather-gen%d:", generation)));
		}
		database.raven = RAVEN_PARSER.getMeta(MavenBuildGameVersion::new, "net.ornithemc:raven:");
		database.sparrow = SPARROW_PARSER.getMeta(MavenBuildGameVersion::new, "net.ornithemc:sparrow:");
		database.nests = NESTS_PARSER.getMeta(MavenBuildGameVersion::new, "net.ornithemc:nests:");
		database.loader.put(LoaderType.FABRIC, FABRIC_LOADER_PARSER.getMeta(MavenBuildVersion::new, "net.fabricmc:fabric-loader:", list -> {
			for (BaseVersion version : list) {
				if (isPublicLoaderVersion(version)) {
					version.setStable(true);
					break;
				}
			}
		}));
		database.loader.put(LoaderType.QUILT, QUILT_LOADER_PARSER.getMeta(MavenBuildVersion::new, "org.quiltmc:quilt-loader:", list -> {
			for (BaseVersion version : list) {
				if (isPublicLoaderVersion(version)) {
					version.setStable(true);
					break;
				}
			}
		}));
		database.installer = INSTALLER_PARSER.getMeta(MavenUrlVersion::new, "net.ornithemc:ornithe-installer:");
		database.osl = OSL_PARSER.getMeta(MavenVersion::new, "net.ornithemc:osl:");
		for (MavenVersion version : database.osl) {
			database.oslDependencies.put(version.getVersion(), OSL_DEPENDENCY_PARSER.getMeta(MavenVersion::new, "osl", version.getVersion(), v -> {
				return v.getMaven().startsWith("net.ornithemc.osl");
			}));
		}
		for (Map.Entry<String, PomParser> e : getOslModulePomParsers(database.osl).entrySet()) {
			String module = e.getKey();
			PomParser parser = e.getValue();

			database.oslModules.put(module, parser.getMeta(MavenVersion::new, "net.ornithemc.osl:" + module + ":"));
		};
		database.loadMcData();
		System.out.println("DB update took " + (System.currentTimeMillis() - start) + "ms");
		return database;
	}

	private void loadMcData() throws IOException {
		if (intermediary.isEmpty() || feather.isEmpty()) {
			throw new RuntimeException("Mappings are empty");
		}

		MinecraftLauncherMeta launcherMeta = MinecraftLauncherMeta.getAllMeta();

		BiFunction<Integer, String, Predicate<MavenBuildGameVersion>> p = (generation, src) -> {
			return o -> {
				if (launcherMeta.getVersions().stream().noneMatch(metaVersion -> metaVersion.getId().equals(o.getVersionNoSide()))) {
					System.out.println("Removing " + o.getGameVersion() + " from " + src + (generation < 1 ? "" : " gen" + generation) + " as it does not match a mc version");
					return true;
				}
				return false;
			};
		};

		for (int generation = 1; generation <= LATEST_GENERATION; generation++) {
			final int gen = generation;
			intermediary.compute(generation, (key, value) -> {
				// Sorts in the order of minecraft release dates
				value = new ArrayList<>(value);
				value.sort(Comparator.comparingInt(o -> launcherMeta.getIndex(o.getVersionNoSide())));
				value.forEach(version -> version.setStable(true));

				// Remove entries that do not match a valid mc version.
				value.removeIf(o -> {
					if (launcherMeta.getVersions().stream().noneMatch(metaVersion -> metaVersion.getId().equals(o.getVersionNoSide()))) {
						System.out.println("Removing " + o.getVersion() + " from intermediary v3" + (gen < 1 ? "" : " gen" + gen) + " as it does not match a mc version");
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
				value.removeIf(p.apply(gen, "v3 feather"));

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

		raven = new ArrayList<>(raven);
		raven.sort(Comparator.comparingInt(o -> launcherMeta.getIndex(o.getVersionNoSide())));
		raven.forEach(version -> version.setStable(true));
		sparrow = new ArrayList<>(sparrow);
		sparrow.sort(Comparator.comparingInt(o -> launcherMeta.getIndex(o.getVersionNoSide())));
		sparrow.forEach(version -> version.setStable(true));
		nests = new ArrayList<>(nests);
		nests.sort(Comparator.comparingInt(o -> launcherMeta.getIndex(o.getVersionNoSide())));
		nests.forEach(version -> version.setStable(true));

		raven.removeIf(p.apply(-1, "v3 raven"));
		sparrow.removeIf(p.apply(-1, "v3 sparrow"));
		nests.removeIf(p.apply(-1, "v3 nests"));
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

	public List<MavenBuildVersion> getLoader(LoaderType type) {
		return loader.get(type).stream().filter(VersionDatabase::isPublicLoaderVersion).collect(Collectors.toList());
	}

	public List<MavenVersion> getOslDependencies(String version) {
		return oslDependencies.get(version);
	}

	public List<MavenVersion> getOslModule(String module) {
		return oslModules.get(module);
	}
	
	private static boolean isPublicLoaderVersion(BaseVersion version) {
		return true;
	}

	public List<MavenBuildVersion> getAllLoader(LoaderType type) {
		return Collections.unmodifiableList(loader.get(type));
	}
}
