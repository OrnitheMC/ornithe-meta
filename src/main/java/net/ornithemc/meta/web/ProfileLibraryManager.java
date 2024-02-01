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

package net.ornithemc.meta.web;

import java.util.LinkedHashSet;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.vdurmont.semver4j.Semver;

import net.ornithemc.meta.OrnitheMeta;
import net.ornithemc.meta.data.VersionDatabase;
import net.ornithemc.meta.web.models.LoaderInfoV3;

public class ProfileLibraryManager {

	private static final Set<Library> LIBRARIES;

	static {
		LIBRARIES = new LinkedHashSet<>();

		// the slf4j binding for log4j - this version has been tested to work on 1.6, 1.11, 1.12
		// lower versions of mc not tested because they did not yet ship log4j to begin with
		LIBRARIES.add(Library.all("org.slf4j:slf4j-api:2.0.1"));
		LIBRARIES.add(Library.all("org.apache.logging.log4j:log4j-slf4j2-impl:2.19.0"));
		LIBRARIES.add(Library.all("org.apache.logging.log4j:log4j-api:2.19.0", "org.apache.logging.log4j:log4j-core:2.19.0"));
		LIBRARIES.add(Library.all("it.unimi.dsi:fastutil:8.5.9"));
		LIBRARIES.add(Library.all("com.google.code.gson:gson:2.10"));

		LIBRARIES.add(Library.upTo("1.5.2", "com.google.guava:guava:14.0"));
		LIBRARIES.add(Library.upTo("1.7.5", "commons-codec:commons-codec:1.9"));
		LIBRARIES.add(Library.upTo("1.7.10", "org.apache.commons:commons-compress:1.8.1"));
		LIBRARIES.add(Library.upTo("1.5.2", "commons-io:commons-io:2.4"));
		LIBRARIES.add(Library.upTo("1.5.2", "org.apache.commons:commons-lang3:3.1"));
		LIBRARIES.add(Library.upTo("1.7.10", "commons-logging:commons-logging:1.1.3"));
		LIBRARIES.add(Library.upTo("1.7.9", "org.apache.httpcomponents:httpcore:4.3.2"));
		LIBRARIES.add(Library.upTo("1.7.9", "org.apache.httpcomponents:httpclient:4.3.3"));
	}

	public static JsonArray getLibraries(LoaderInfoV3 info, String side) {
		JsonObject launcherMeta = info.getLauncherMeta();

		JsonObject librariesObject = launcherMeta.get("libraries").getAsJsonObject();
		// Build the libraries array with the existing libs + loader and intermediary
		JsonArray libraries = (JsonArray) librariesObject.get("common");
		libraries.add(getLibrary(info.getIntermediary().getMaven(), VersionDatabase.ORNITHE_MAVEN_URL));
		libraries.add(getLibrary(info.getLoader().getMaven(), info.getLoaderType().getMavenUrl()));

		Semver mcVersion = OrnitheMeta.database.manifest.get(info.getGame(side));

		if (librariesObject.has(side)) {
			libraries.addAll(librariesObject.get(side).getAsJsonArray());
		}
		for (Library library : LIBRARIES) {
			boolean minSatisfied = (library.minVersion == null || mcVersion.compareTo(library.minVersion) >= 0);
			boolean maxSatisfied = (library.maxVersion == null || mcVersion.compareTo(library.maxVersion) <= 0);

			if (minSatisfied && maxSatisfied) {
				for (String maven : library.maven) {
					libraries.add(getLibrary(maven, VersionDatabase.MINECRAFT_LIBRARIES_URL));
				}
			}
		}

		return libraries;
	}

	private static JsonObject getLibrary(String mavenPath, String url) {
		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("name", mavenPath);
		jsonObject.addProperty("url", url);
		return jsonObject;
	}

	private static class Library {

		private final Semver minVersion;
		private final Semver maxVersion;
		private final String[] maven;

		public static Library all(String... maven) {
			return new Library(null, null, maven);
		}

		public static Library from(String minVersion, String... maven) {
			return new Library(new Semver(minVersion), null, maven);
		}

		public static Library upTo(String maxVersion, String... maven) {
			return new Library(null, new Semver(maxVersion), maven);
		}

		public static Library between(String minVersion, String maxVersion, String... maven) {
			return new Library(new Semver(minVersion), new Semver(maxVersion), maven);
		}

		private Library(Semver minVersion, Semver maxVersion, String... maven) {
			this.minVersion = minVersion;
			this.maxVersion = maxVersion;
			this.maven = maven;
		}
	}
}
