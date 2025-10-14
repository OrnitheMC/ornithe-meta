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
import net.ornithemc.meta.utils.VersionManifest;
import net.ornithemc.meta.web.models.Library;

public class LibraryUpgradesV3 {

	private static final Path FILE_PATH = Paths.get("library-upgrades-v3.json");

	private static List<LibraryUpgrade> cache;

	public static List<LibraryUpgrade> reload() {
		if (Files.exists(FILE_PATH)) {
			try (InputStream is = Files.newInputStream(FILE_PATH);) {
				cache = OrnitheMeta.MAPPER.readValue(is, new TypeReference<List<LibraryUpgrade>>() { });
			} catch (IOException e) {
				OrnitheMeta.LOGGER.warn("unable to load library upgrades from file", e);
			}
		}

		return cache;
	}

	public static class LibraryUpgrade {

		public String name;
		public String url = VersionDatabase.MINECRAFT_LIBRARIES_URL;

		public Integer minIntermediaryGeneration;
		public Integer maxIntermediaryGeneration;
		public String minGameVersion;
		public String maxGameVersion;

		private boolean validated;

		private void validate() {
			if (this.validated) {
				return;
			}

			String[] parts = this.name.split("[:]");

			if (parts.length < 3 || parts.length > 4) {
				throw new RuntimeException("invalid maven notation for library upgrade: " + name);
			}

			if (minIntermediaryGeneration != null && maxIntermediaryGeneration != null && minIntermediaryGeneration > maxIntermediaryGeneration) {
				throw new RuntimeException("invalid intermediary generation bounds for library upgrade: " + name + " (" + minIntermediaryGeneration + " > " + maxIntermediaryGeneration + ")");
			}

			// generation bounds for checking version bounds
			int minGen = (minIntermediaryGeneration == null) ? 1 : minIntermediaryGeneration;
			int maxGen = (maxIntermediaryGeneration == null) ? VersionDatabase.config.latestIntermediaryGeneration : maxIntermediaryGeneration;


			if (minGameVersion != null || maxGameVersion != null) {
				for (int generation = minGen; generation <= maxGen; generation++) {
					VersionManifest manifest = OrnitheMeta.database.getManifest(generation);

					Semver minVersion = (minGameVersion == null) ? null : manifest.getVersion(minGameVersion);
					Semver maxVersion = (maxGameVersion == null) ? null : manifest.getVersion(maxGameVersion);

					if (minGameVersion != null && minVersion == null) {
						throw new RuntimeException("unknown minimum game version for library upgrade (gen" + generation + ": " + name + " (" + minGameVersion + ")");
					}
					if (maxGameVersion != null && maxVersion == null) {
						throw new RuntimeException("unknown maximum game version for library upgrade (gen" + generation + ": " + name + " (" + maxGameVersion + ")");
					}
					
					if (minVersion != null && maxVersion != null && minVersion.compareTo(maxVersion) > 0) {
						throw new RuntimeException("invalid game version bounds for library upgrade (gen" + generation + "): " + name + " (" + minGameVersion + " > " + maxGameVersion + ")");
					}
				}

			}

			this.validated = true;
		}

		public boolean test(int generation, String gameVersion) {
			validate();

			if (this.minIntermediaryGeneration != null && generation < this.minIntermediaryGeneration) {
				return false;
			}
			if (this.maxIntermediaryGeneration != null && generation > this.maxIntermediaryGeneration) {
				return false;
			}

			VersionManifest manifest = OrnitheMeta.database.getManifest(generation);
			Semver version = manifest.getVersion(gameVersion);

			if (this.minGameVersion != null) {
				Semver minVersion = manifest.getVersion(this.minGameVersion);

				if (version.compareTo(minVersion) < 0) {
					return false;
				}
			}
			if (this.maxGameVersion != null) {
				Semver maxVersion = manifest.getVersion(this.maxGameVersion);

				if (version.compareTo(maxVersion) > 0) {
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
