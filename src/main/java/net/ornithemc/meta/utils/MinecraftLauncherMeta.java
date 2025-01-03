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

package net.ornithemc.meta.utils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.ornithemc.meta.OrnitheMeta;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@JsonIgnoreProperties({"$schema", "latest"})
public class MinecraftLauncherMeta {

	List<Version> versions;

	public MinecraftLauncherMeta(@JsonProperty("versions") List<Version> versions) {
		this.versions = versions;
	}

	public static MinecraftLauncherMeta getMeta(int generation) throws IOException {
		String url;
		if (generation < 1) {
			throw new IllegalArgumentException("invalid generation " + generation);
		} else if (generation == 1) {
			url = "https://skyrising.github.io/mc-versions/version_manifest.json";
		} else {
			url = "https://ornithemc.net/mc-versions/version_manifest.json";
		}
		String json = IOUtils.toString(new URL(url), StandardCharsets.UTF_8);
		return OrnitheMeta.MAPPER.readValue(json, MinecraftLauncherMeta.class);
	}

	public static MinecraftLauncherMeta getSortedMeta(int generation) throws IOException {
		List<Version> versions = new ArrayList<>(getMeta(generation).versions);

		// Order by release time
		versions.sort(Comparator.comparing(Version::getReleaseTime).reversed());

		return new MinecraftLauncherMeta(versions);
	}

	public boolean isStable(String id) {
		return versions.stream().anyMatch(version -> version.id.equals(id) && version.type.equals("release"));
	}

	public int getIndex(String version) {
		for (int i = 0; i < versions.size(); i++) {
			if (versions.get(i).id.equals(version)) {
				return i;
			}
		}
		return 0;
	}

	public List<Version> getVersions() {
		return Collections.unmodifiableList(versions);
	}

	public static class Version {

		String id;
		String type;
		String url;
		String time;
		String releaseTime;
		String details;

		public String getId() {
			return id;
		}

		public String getType() {
			return type;
		}

		public String getUrl() {
			return url;
		}

		public String getTime() {
			return time;
		}

		public String getDetails() {
			return details;
		}

		public String getReleaseTime() {
			return releaseTime;
		}
	}

}
