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

import com.fasterxml.jackson.databind.JsonNode;
import com.vdurmont.semver4j.Semver;
import net.ornithemc.meta.OrnitheMeta;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

public class VersionManifest {

	private static final String DETAILS_URL = "https://skyrising.github.io/mc-versions/version/%s.json";

	private final Map<String, Semver> versions;

	public VersionManifest() {
		this.versions = new HashMap<>();
	}

	public Semver get(String id) {
		return versions.computeIfAbsent(id, key -> {
			try (InputStreamReader input = new InputStreamReader(new URL(String.format(DETAILS_URL, id)).openStream())) {
				JsonNode details = OrnitheMeta.MAPPER.readTree(input);
				String normalized = details.get("normalizedVersion").asText();

				return new Semver(normalized);
			} catch (IOException e) {
				throw new NoSuchElementException("no version with id " + id + " exists!");
			}
		});
	}
}
