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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.vdurmont.semver4j.Semver;

import net.ornithemc.meta.OrnitheMeta;
import net.ornithemc.meta.data.VersionDatabase;
import net.ornithemc.meta.web.models.Library;

public class LibraryUpgradesV3 {

	private static final Path FILE_PATH = Paths.get("library-upgrades-v3.json");

	private static List<LibraryUpgrade> cache;

	public static List<LibraryUpgrade> get() {
		reload();
		validate();

		return cache;
	}

	private static void reload() {
		if (Files.exists(FILE_PATH)) {
			try (InputStream is = Files.newInputStream(FILE_PATH);) {
				cache = OrnitheMeta.MAPPER.readValue(is, new TypeReference<List<LibraryUpgrade>>() { });
			} catch (IOException e) {
				OrnitheMeta.LOGGER.warn("unable to load library upgrades from file", e);
			}
		}
	}

	private static void validate() {
		if (cache == null) {
			throw new RuntimeException("library upgrades v3 could not be read from file: file does not exist or is badly formatted");
		}

		for (LibraryUpgrade lib : cache) {
			String name = lib.name;
			String[] parts = name.split("[:]");

			if (parts.length < 3 || parts.length > 4) {
				throw new RuntimeException("invalid maven notation for library upgrade: " + name);
			}

			Integer minGeneration = lib.minIntermediaryGeneration;
			Integer maxGeneration = lib.maxIntermediaryGeneration;

			if (minGeneration != null && maxGeneration != null && minGeneration > maxGeneration) {
				throw new RuntimeException("invalid intermediary generation bounds for library upgrade: " + name + " (" + minGeneration + " > " + maxGeneration + ")");
			}

			String minGameVersion = lib.minGameVersion;
			String maxGameVersion = lib.maxGameVersion;

			if (minGameVersion != null && maxGameVersion != null) {
				Semver min = OrnitheMeta.database.manifest.getVersion(minGameVersion);
				Semver max = OrnitheMeta.database.manifest.getVersion(maxGameVersion);

				if (min == null) {
					throw new RuntimeException("unknown minimum game version for library upgrade: " + name + " (" + minGameVersion + ")");
				}
				if (max == null) {
					throw new RuntimeException("unknown maximum game version for library upgrade: " + name + " (" + maxGameVersion + ")");
				}

				if (min.compareTo(max) > 0) {
					throw new RuntimeException("invalid game version bounds for library upgrade: " + name + " (" + minGameVersion + " > " + maxGameVersion + ")");
				}
			}
		}
	}

	public static class LibraryUpgrade {

		public String name;
		public String url = VersionDatabase.MINECRAFT_LIBRARIES_URL;

		public Integer minIntermediaryGeneration;
		public Integer maxIntermediaryGeneration;
		public String minGameVersion;
		public String maxGameVersion;

		public boolean test(int generation, String gameVersion) {
			if (this.minIntermediaryGeneration != null && generation < this.minIntermediaryGeneration) {
				return false;
			}
			if (this.maxIntermediaryGeneration != null && generation > this.maxIntermediaryGeneration) {
				return false;
			}

			Semver v = OrnitheMeta.database.manifest.getVersion(gameVersion);

			if (this.minGameVersion != null) {
				Semver vmin = OrnitheMeta.database.manifest.getVersion(this.minGameVersion);

				if (v.compareTo(vmin) < 0) {
					return false;
				}
			}
			if (this.maxGameVersion != null) {
				Semver vmax = OrnitheMeta.database.manifest.getVersion(this.maxGameVersion);

				if (v.compareTo(vmax) > 0) {
					return false;
				}
			}

			return true;
		}

		public Library asLibrary() {
			return new Library(this.name, this.url);
		}
	}
}
