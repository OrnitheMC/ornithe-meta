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
import net.ornithemc.meta.web.models.MavenBuildVersion;
import net.ornithemc.meta.web.models.MavenUrlVersion;
import net.ornithemc.meta.web.models.MavenVersion;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class VersionDatabaseOld {

	public static final String ORNITHE_MAVEN_URL = "https://maven.ornithemc.net/releases/";

	public static final PomParser CALAMUS_PARSER = new PomParser(ORNITHE_MAVEN_URL + "net/ornithemc/calamus/maven-metadata.xml");
	public static final PomParser LOADER_PARSER = new PomParser(ORNITHE_MAVEN_URL + "net/ornithemc/ornithe-loader/maven-metadata.xml");
	public static final PomParser INSTALLER_PARSER = new PomParser(ORNITHE_MAVEN_URL + "net/ornithemc/ornithe-installer-old/maven-metadata.xml");

	public List<BaseVersion> game;
	public List<MavenVersion> calamus;
	private List<MavenBuildVersion> loader;
	public List<MavenUrlVersion> installer;

	private VersionDatabaseOld() {
	}

	public static VersionDatabaseOld generate() throws IOException, XMLStreamException {
		long start = System.currentTimeMillis();
		VersionDatabaseOld database = new VersionDatabaseOld();
		database.calamus = CALAMUS_PARSER.getMeta(MavenVersion::new, "net.ornithemc:calamus:");
		database.loader = LOADER_PARSER.getMeta(MavenBuildVersion::new, "net.ornithemc:ornithe-loader:", list -> {
			for (BaseVersion version : list) {
				if (isPublicLoaderVersion(version)) {
					version.setStable(true);
					break;
				}
			}
		});
		database.installer = INSTALLER_PARSER.getMeta(MavenUrlVersion::new, "net.ornithemc:ornithe-installer-old:");
		database.loadMcData();
		System.out.println("DB update took " + (System.currentTimeMillis() - start) + "ms");
		return database;
	}

	private void loadMcData() throws IOException {
		if (calamus == null) {
			throw new RuntimeException("Mappings are null");
		}
		MinecraftLauncherMeta launcherMeta = MinecraftLauncherMeta.getSortedMeta(1);

		//Sorts in the order of minecraft release dates
		calamus = new ArrayList<>(calamus);
		calamus.sort(Comparator.comparingInt(o -> launcherMeta.getIndex(o.getVersion())));
		calamus.forEach(version -> version.setStable(true));

		// Remove entries that do not match a valid mc version.
		calamus.removeIf(o -> {
			String iVersion;
			if (o.getVersion().endsWith("-client") || o.getVersion().endsWith("-server")) {
				iVersion = o.getVersion().substring(0, o.getVersion().length() - 7);
			} else {
				iVersion = o.getVersion();
			}

			if (launcherMeta.getVersions().stream().noneMatch(metaVersion -> metaVersion.getId().equals(iVersion))) {
				System.out.println("Removing " + o.getVersion() + " as it is not match an mc version (v2)");
				return true;
			}
			return false;
		});

		List<String> minecraftVersions = new ArrayList<>();
		for (MavenVersion gameVersion : calamus) {
			if (!minecraftVersions.contains(gameVersion.getVersion())) {
				minecraftVersions.add(gameVersion.getVersion());
			}
		}

		game = minecraftVersions.stream().map(s -> new BaseVersion(s, launcherMeta.isStable(s))).collect(Collectors.toList());
	}

	public List<MavenBuildVersion> getLoader() {
		return loader.stream().filter(VersionDatabaseOld::isPublicLoaderVersion).collect(Collectors.toList());
	}
	
	private static boolean isPublicLoaderVersion(BaseVersion version) {
		return true;
	}

	public List<MavenBuildVersion> getAllLoader() {
		return Collections.unmodifiableList(loader);
	}
}
