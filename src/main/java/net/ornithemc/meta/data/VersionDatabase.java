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
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class VersionDatabase {

	public static final String FABRIC_MAVEN_URL = "https://maven.fabricmc.net/";
	public static final String QUILT_MAVEN_URL = "https://maven.quiltmc.org/repository/release/";
	public static final String ORNITHE_MAVEN_URL = "https://maven.ornithemc.net/releases/";
	public static final String ORNITHE_MAVEN_DETAILS_URL = "https://maven.ornithemc.net/api/maven/details/releases/";
	public static final String ORNITHE_MAVEN_VERSIONS_URL = "https://maven.ornithemc.net/api/maven/versions/releases/";
	public static final String MINECRAFT_LIBRARIES_URL = "https://libraries.minecraft.net/";

	public static final PomParser INTERMEDIARY_PARSER = new PomParser(ORNITHE_MAVEN_URL + "net/ornithemc/calamus-intermediary/maven-metadata.xml");
	public static final PomParser FEATHER_PARSER = new PomParser(ORNITHE_MAVEN_URL + "net/ornithemc/feather/maven-metadata.xml");
	public static final PomParser NESTS_PARSER = new PomParser(ORNITHE_MAVEN_URL + "net/ornithemc/nests/maven-metadata.xml");
	public static final PomParser FABRIC_LOADER_PARSER = new PomParser(FABRIC_MAVEN_URL + "net/fabricmc/fabric-loader/maven-metadata.xml");
	public static final PomParser QUILT_LOADER_PARSER = new PomParser(QUILT_MAVEN_URL + "org/quiltmc/quilt-loader/maven-metadata.xml");
	public static final PomParser INSTALLER_PARSER = new PomParser(ORNITHE_MAVEN_URL + "net/ornithemc/ornithe-installer/maven-metadata.xml");
	public static final PomParser OSL_PARSER = new PomParser(ORNITHE_MAVEN_URL + "net/ornithemc/osl/maven-metadata.xml");
	public static final PomDependencyParser OSL_DEPENDENCY_PARSER = new PomDependencyParser(ORNITHE_MAVEN_URL + "net/ornithemc/osl");

	public final Map<String, PomParser> getOslModulePomParsers() {
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

	private final Map<LoaderType, List<MavenBuildVersion>> loader;
	private final Map<String, List<MavenVersion>> oslDependencies;
	private final Map<String, List<MavenVersion>> oslModules;

	public final VersionManifest manifest = new VersionManifest();

	public List<BaseVersion> game;
	public List<MavenVersion> intermediary;
	public List<MavenBuildGameVersion> feather;
	public List<MavenBuildGameVersion> nests;
	public List<MavenUrlVersion> installer;
	public List<MavenVersion> osl;

	private VersionDatabase() {
		this.loader = new EnumMap<>(LoaderType.class);
		this.oslDependencies = new HashMap<>();
		this.oslModules = new HashMap<>();
	}

	public static VersionDatabase generate() throws IOException, XMLStreamException {
		long start = System.currentTimeMillis();
		VersionDatabase database = new VersionDatabase();
		database.intermediary = INTERMEDIARY_PARSER.getMeta(MavenVersion::new, "net.ornithemc:calamus-intermediary:");
		database.feather = FEATHER_PARSER.getMeta(MavenBuildGameVersion::new, "net.ornithemc:feather:");
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
		for (Map.Entry<String, PomParser> e : database.getOslModulePomParsers().entrySet()) {
			String module = e.getKey();
			PomParser parser = e.getValue();

			database.oslModules.put(module, parser.getMeta(MavenVersion::new, "net.ornithemc.osl:" + module + ":"));
		};
		database.loadMcData();
		System.out.println("DB update took " + (System.currentTimeMillis() - start) + "ms");
		return database;
	}

	private void loadMcData() throws IOException {
		if (intermediary == null || feather == null) {
			throw new RuntimeException("Mappings are null");
		}

		MinecraftLauncherMeta launcherMeta = MinecraftLauncherMeta.getAllMeta();

		// Sorts in the order of minecraft release dates
		intermediary = new ArrayList<>(intermediary);
		intermediary.sort(Comparator.comparingInt(o -> launcherMeta.getIndex(o.getVersionNoSide())));
		intermediary.forEach(version -> version.setStable(true));
		feather = new ArrayList<>(feather);
		feather.sort(Comparator.comparingInt(o -> launcherMeta.getIndex(o.getVersionNoSide())));
		feather.forEach(version -> version.setStable(true));
		nests = new ArrayList<>(nests);
		nests.sort(Comparator.comparingInt(o -> launcherMeta.getIndex(o.getVersionNoSide())));
		nests.forEach(version -> version.setStable(true));

		// Remove entries that do not match a valid mc version.
		intermediary.removeIf(o -> {
			if (launcherMeta.getVersions().stream().noneMatch(metaVersion -> metaVersion.getId().equals(o.getVersionNoSide()))) {
				System.out.println("Removing " + o.getVersion() + " as it is not match an mc version (v3 intermediary)");
				return true;
			}
			return false;
		});

		Predicate<MavenBuildGameVersion> p = o -> {
			if (launcherMeta.getVersions().stream().noneMatch(metaVersion -> metaVersion.getId().equals(o.getVersionNoSide()))) {
				System.out.println("Removing " + o.getGameVersion() + " as it is not match an mc version (v3 Feather/nests)");
				return true;
			}
			return false;
		};
		feather.removeIf(p);
		nests.removeIf(p);

		List<String> minecraftVersions = new ArrayList<>();
		for (MavenVersion gameVersion : intermediary) {
			if (!minecraftVersions.contains(gameVersion.getVersionNoSide())) {
				minecraftVersions.add(gameVersion.getVersionNoSide());
			}
		}

		game = minecraftVersions.stream().map(s -> new BaseVersion(s, launcherMeta.isStable(s))).collect(Collectors.toList());
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
