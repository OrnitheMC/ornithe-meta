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
import com.vdurmont.semver4j.Semver;

import net.ornithemc.meta.OrnitheMeta;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties({"$schema", "latest"})
public class VersionManifest {

	private final List<Version> versions;
	private final Map<String, VersionDetails> details;

	public VersionManifest(@JsonProperty("versions") List<Version> versions) {
		this.versions = versions;
		this.details = new HashMap<>();
	}

	public static VersionManifest forGen(int generation) throws IOException {
		String url;
		if (generation < 1) {
			throw new IllegalArgumentException("invalid generation " + generation);
		} else {
			url = String.format("https://ornithemc.net/mc-versions/gen%d/version_manifest.json", generation);
		}
		String json = IOUtils.toString(new URL(url), StandardCharsets.UTF_8);
		return OrnitheMeta.MAPPER.readValue(json, VersionManifest.class);
	}

	public static VersionManifest forGenSorted(int generation) throws IOException {
		List<Version> versions = new ArrayList<>(forGen(generation).versions);

		// Order by release time
		versions.sort(Comparator.comparing(Version::releaseTime).reversed());

		return new VersionManifest(versions);
	}

	private VersionDetails versionDetails(String id) {
		return details.computeIfAbsent(id, (key) -> {
			int index = indexOf(id);
			Version version = versions.get(index);

			if (version == null) {
				return null;
			}

			try {
				String json = IOUtils.toString(new URL(version.details), StandardCharsets.UTF_8);
				return OrnitheMeta.MAPPER.readValue(json, VersionDetails.class);
			} catch (IOException e) {
				return null;
			}
		});
	}

	public boolean contains(String id) {
		return versions.stream().anyMatch(version -> version.id.equals(id));
	}

	public int indexOf(String id) {
		for (int i = 0; i < versions.size(); i++) {
			if (versions.get(i).id.equals(id)) {
				return i;
			}
		}

		return 0;
	}

	public boolean isStable(String id) {
		return versions.stream().anyMatch(version -> version.id.equals(id) && version.type.equals("release"));
	}

	public Semver normalize(String id) {
		VersionDetails details = versionDetails(id);
		return details == null ? null : new Semver(details.normalizedVersion);
	}

	public static class Version {

		public String id;
		public String type;
		public String url;
		public String sha1;
		public String time;
		public String releaseTime;
		public String details;
		public String detailsSha1;

		public String id() {
			return id;
		}

		public String type() {
			return type;
		}

		public String url() {
			return url;
		}

		public String time() {
			return time;
		}

		public String details() {
			return details;
		}

		public String releaseTime() {
			return releaseTime;
		}
	}

	public static class VersionDetails {

		public String id;
		public String normalizedVersion;

	}
}
