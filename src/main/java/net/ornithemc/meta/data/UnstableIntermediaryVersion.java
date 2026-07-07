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

import com.vdurmont.semver4j.Semver;

import net.ornithemc.meta.OrnitheMeta;
import net.ornithemc.meta.utils.VersionManifest;

public class UnstableIntermediaryVersion {

	private final int intermediaryGeneration = VersionDatabase.config.latestIntermediaryGeneration;

	public String gameVersion;
	public String minGameVersion;
	public String maxGameVersion;

	private boolean validated;

	private void validate() {
		if (this.validated) {
			return;
		}

		if (gameVersion != null && (minGameVersion != null || maxGameVersion != null)) {
			throw new RuntimeException("cannot specify both an exact game version and game version bounds!");
		}

		if (minGameVersion != null || maxGameVersion != null) {
			VersionManifest manifest = OrnitheMeta.database.getManifest(intermediaryGeneration);

			Semver minVersion = (minGameVersion == null) ? null : manifest.normalize(minGameVersion);
			Semver maxVersion = (maxGameVersion == null) ? null : manifest.normalize(maxGameVersion);

			if (minGameVersion != null && minVersion == null) {
				throw new RuntimeException("unknown minimum game version for unstable intermediary version (gen" + intermediaryGeneration + ": (" + minGameVersion + ")");
			}
			if (maxGameVersion != null && maxVersion == null) {
				throw new RuntimeException("unknown maximum game version for unstable intermediary version (gen" + intermediaryGeneration + ": (" + maxGameVersion + ")");
			}
			
			if (minVersion != null && maxVersion != null && minVersion.compareTo(maxVersion) > 0) {
				throw new RuntimeException("invalid game version bounds for unstable intermediary version (gen" + intermediaryGeneration + "): (" + minGameVersion + " > " + maxGameVersion + ")");
			}
		}

		this.validated = true;
	}

	public boolean test(int generation, String gameVersion) {
		validate();

		if (generation != this.intermediaryGeneration) {
			return false;
		}

		VersionManifest manifest = OrnitheMeta.database.getManifest(generation);
		Semver version = manifest.normalize(gameVersion);

		if (this.gameVersion != null) {
			Semver targetGameVersion = manifest.normalize(this.gameVersion);

			if (version.compareTo(targetGameVersion) != 0) {
				return false;
			}
		}
		if (this.minGameVersion != null) {
			Semver minVersion = manifest.normalize(this.minGameVersion);

			if (version.compareTo(minVersion) < 0) {
				return false;
			}
		}
		if (this.maxGameVersion != null) {
			Semver maxVersion = manifest.normalize(this.maxGameVersion);

			if (version.compareTo(maxVersion) > 0) {
				return false;
			}
		}

		return true;
	}
}
