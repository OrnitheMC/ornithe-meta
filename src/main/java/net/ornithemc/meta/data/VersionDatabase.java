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
import net.ornithemc.meta.utils.PomParser;
import net.ornithemc.meta.web.models.BaseVersion;
import net.ornithemc.meta.web.models.LoaderType;
import net.ornithemc.meta.web.models.MavenBuildGameVersion;
import net.ornithemc.meta.web.models.MavenBuildVersion;
import net.ornithemc.meta.web.models.MavenUrlVersion;
import net.ornithemc.meta.web.models.MavenVersion;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class VersionDatabase {

	public static final String FABRIC_MAVEN_URL = "https://maven.fabricmc.net/";
	public static final String QUILT_MAVEN_URL = "https://maven.quiltmc.org/repository/release/";
	public static final String ORNITHE_MAVEN_URL = "https://maven.ornithemc.net/releases/";

	public static final PomParser INTERMEDIARY_PARSER = new PomParser(ORNITHE_MAVEN_URL + "net/ornithemc/calamus-intermediary/maven-metadata.xml");
	public static final PomParser FEATHER_PARSER = new PomParser(ORNITHE_MAVEN_URL + "net/ornithemc/feather/maven-metadata.xml");
	public static final PomParser NESTS_PARSER = new PomParser(ORNITHE_MAVEN_URL + "net/ornithemc/nests/maven-metadata.xml");
	public static final PomParser FABRIC_LOADER_PARSER = new PomParser(FABRIC_MAVEN_URL + "net/fabricmc/fabric-loader/maven-metadata.xml");
	public static final PomParser QUILT_LOADER_PARSER = new PomParser(QUILT_MAVEN_URL + "org/quiltmc/quilt-loader/maven-metadata.xml");
	public static final PomParser INSTALLER_PARSER = new PomParser(ORNITHE_MAVEN_URL + "net/ornithemc/ornithe-installer/maven-metadata.xml");

	private final Map<LoaderType, List<MavenBuildVersion>> loader;

	public List<BaseVersion> game;
	public List<MavenVersion> intermediary;
	public List<MavenBuildGameVersion> feather;
	public List<MavenBuildGameVersion> nests;
	public List<MavenUrlVersion> installer;

	private VersionDatabase() {
		this.loader = new EnumMap<>(LoaderType.class);
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
		intermediary.sort(Comparator.comparingInt(o -> launcherMeta.getIndex(o.getVersion())));
		intermediary.forEach(version -> version.setStable(true));
		feather = new ArrayList<>(feather);
		feather.sort(Comparator.comparingInt(o -> launcherMeta.getIndex(o.getVersion())));
		feather.forEach(version -> version.setStable(true));
		nests = new ArrayList<>(nests);
		nests.sort(Comparator.comparingInt(o -> launcherMeta.getIndex(o.getVersion())));
		nests.forEach(version -> version.setStable(true));

		// Remove entries that do not match a valid mc version.
		intermediary.removeIf(o -> {
			String iVersion;
			if (o.getVersion().endsWith("-client") || o.getVersion().endsWith("-server")) {
				iVersion = o.getVersion().substring(0, o.getVersion().length() - 7);
			} else {
				iVersion = o.getVersion();
			}

			if (launcherMeta.getVersions().stream().noneMatch(metaVersion -> metaVersion.getId().equals(iVersion))) {
				System.out.println("Removing " + o.getVersion() + " as it is not match an mc version (v3 intermediary)");
				return true;
			}
			return false;
		});

		Predicate<MavenBuildGameVersion> p = o -> {
			String iVersion;
			if (o.getGameVersion().endsWith("-client") || o.getGameVersion().endsWith("-server")) {
				iVersion = o.getGameVersion().substring(0, o.getGameVersion().length() - 7);
			} else {
				iVersion = o.getGameVersion();
			}

			if (launcherMeta.getVersions().stream().noneMatch(metaVersion -> metaVersion.getId().equals(iVersion))) {
				System.out.println("Removing " + o.getGameVersion() + " as it is not match an mc version (v3 Feather/nests)");
				return true;
			}
			return false;
		};
		feather.removeIf(p);
		nests.removeIf(p);

		List<String> minecraftVersions = new ArrayList<>();
		for (MavenVersion gameVersion : intermediary) {
			if (!minecraftVersions.contains(gameVersion.getVersion())) {
				minecraftVersions.add(gameVersion.getVersion());
			}
		}

		game = minecraftVersions.stream().map(s -> new BaseVersion(s, launcherMeta.isStable(s))).collect(Collectors.toList());
	}

	public List<MavenBuildVersion> getLoader(LoaderType type) {
		return loader.get(type).stream().filter(VersionDatabase::isPublicLoaderVersion).collect(Collectors.toList());
	}
	
	private static boolean isPublicLoaderVersion(BaseVersion version) {
		return true;
	}

	public List<MavenBuildVersion> getAllLoader(LoaderType type) {
		return Collections.unmodifiableList(loader.get(type));
	}
}
