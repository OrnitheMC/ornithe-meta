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

public class FeatherVersionParser {

	private String mappingsVersion;
	private String minecraftVersion;

	private String version;

	public FeatherVersionParser(String version) {
		this.version = version;

		this.minecraftVersion = version.substring(0, version.lastIndexOf('+'));
		this.mappingsVersion = version.substring(version.lastIndexOf('.') + 1);
	}

	public String getMappingsVersion() {
		return mappingsVersion;
	}

	public String getMinecraftVersion() {
		return minecraftVersion;
	}

	@Override
	public String toString() {
		return version;
	}
}