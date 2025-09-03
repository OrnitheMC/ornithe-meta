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

package net.ornithemc.meta.web.models;

import com.fasterxml.jackson.annotation.JsonIgnore;

import com.vdurmont.semver4j.Semver;

import net.ornithemc.meta.OrnitheMeta;
import net.ornithemc.meta.data.VersionDatabase;

public class LibraryV3 {

	public static LibraryV3 of(String maven) {
		return new LibraryV3(maven, VersionDatabase.MINECRAFT_LIBRARIES_URL);
	}

	String name;
	String version;
	String url;

	@JsonIgnore
	Integer minGeneration;
	@JsonIgnore
	Integer maxGeneration;
	@JsonIgnore
	String minGameVersion;
	@JsonIgnore
	String maxGameVersion;

	public LibraryV3(String maven, String url) {
		this(maven, maven.split("[:]")[2], url);
	}

	public LibraryV3(String name, String version, String url) {
		this.name = name;
		this.version = version;
		this.url = url;
	}

	public String getName() {
		return this.name;
	}

	public String getVersion() {
		return this.version;
	}

	public String getUrl() {
		return this.url;
	}

	public LibraryV3 setUrl(String url) {
		this.url = url;
		return this;
	}

	public LibraryV3 setMinGeneration(int generation) {
		this.minGeneration = generation;
		return this;
	}

	public LibraryV3 setMaxGeneration(int generation) {
		this.maxGeneration = generation;
		return this;
	}

	public LibraryV3 setMinGameVersion(String gameVersion) {
		this.minGameVersion = gameVersion;
		return this;
	}

	public LibraryV3 setMaxGameVersion(String gameVersion) {
		this.maxGameVersion = gameVersion;
		return this;
	}

	public boolean test(int generation, String gameVersion) {
		if (this.minGeneration != null && generation < this.minGeneration) {
			return false;
		}
		if (this.maxGeneration != null && generation > this.maxGeneration) {
			return false;
		}

		Semver v = OrnitheMeta.database.manifest.get(gameVersion);

		if (this.minGameVersion != null) {
			Semver vmin = OrnitheMeta.database.manifest.get(this.minGameVersion);

			if (v.compareTo(vmin) < 0) {
				return false;
			}
		}
		if (this.maxGameVersion != null) {
			Semver vmax = OrnitheMeta.database.manifest.get(this.maxGameVersion);

			if (v.compareTo(vmax) > 0) {
				return false;
			}
		}

		return true;
	}
}
