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

import net.ornithemc.meta.utils.FeatherVersionParser;

public class MavenBuildGameVersion extends MavenBuildVersion {

	String gameVersion;

	public MavenBuildGameVersion(String maven) {
		super(maven);
		gameVersion = new FeatherVersionParser(maven.split(":")[2]).getMinecraftVersion();

	}

	public String getGameVersion() {
		return gameVersion;
	}

	@Override
	public String getVersionNoSide() {
		if (versionNoSide == null) {
			versionNoSide = gameVersion;

			if (versionNoSide.endsWith("-client") || versionNoSide.endsWith("-server")) {
				versionNoSide = versionNoSide.substring(0, versionNoSide.length() - 7);
			}
		}

		return versionNoSide;
	}

	@Override
	public boolean test(String s) {
		return getGameVersion().equals(s);
	}
}