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
		LIBRARIES.add(Library.library("org.slf4j:slf4j-api:2.0.1"));
		LIBRARIES.add(Library.library("org.apache.logging.log4j:log4j-slf4j2-impl:2.19.0"));
		LIBRARIES.add(Library.library("org.apache.logging.log4j:log4j-api:2.19.0"));
		LIBRARIES.add(Library.library("org.apache.logging.log4j:log4j-core:2.19.0"));
		LIBRARIES.add(Library.library("it.unimi.dsi:fastutil:8.5.9"));
		LIBRARIES.add(Library.library("com.google.code.gson:gson:2.10"));

		// logger-config is needed to make log4j work in versions prior to 13w39a
		LIBRARIES.add(Library.library("net.ornithemc:logger-config:1.0.0").withUrl(VersionDatabase.ORNITHE_MAVEN_URL).upTo("1.7.0-alpha.13.38.c"));
		LIBRARIES.add(Library.library("com.google.guava:guava:14.0").upTo("1.5.2"));
		LIBRARIES.add(Library.library("commons-codec:commons-codec:1.9").upTo("1.7.5"));
		LIBRARIES.add(Library.library("org.apache.commons:commons-compress:1.8.1").upTo("1.7.10"));
		LIBRARIES.add(Library.library("commons-io:commons-io:2.4").upTo("1.5.2"));
		LIBRARIES.add(Library.library("org.apache.commons:commons-lang3:3.1").upTo("1.5.2"));
		LIBRARIES.add(Library.library("commons-logging:commons-logging:1.1.3").upTo("1.7.10"));
		LIBRARIES.add(Library.library("org.apache.httpcomponents:httpcore:4.3.2").upTo("1.7.9"));
		LIBRARIES.add(Library.library("org.apache.httpcomponents:httpclient:4.3.3").upTo("1.7.9"));
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
				libraries.add(getLibrary(library.name, library.url));
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
		private final String name;
		private final String url;

		public static Library library(String name) {
			return new Library(null, null, name, VersionDatabase.MINECRAFT_LIBRARIES_URL);
		}

		public Library from(String minVersion) {
			return new Library(new Semver(minVersion), maxVersion, name, url);
		}

		public Library upTo(String maxVersion) {
			return new Library(minVersion, new Semver(maxVersion), name, url);
		}

		public Library between(String minVersion, String maxVersion) {
			return new Library(new Semver(minVersion), new Semver(maxVersion), name, url);
		}

		public Library withUrl(String url) {
			return new Library(minVersion, maxVersion, name, url);
		}

		private Library(Semver minVersion, Semver maxVersion, String name, String url) {
			this.minVersion = minVersion;
			this.maxVersion = maxVersion;
			this.name = name;
			this.url = url;
		}
	}
}
