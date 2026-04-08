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

public class MavenGameVersionWithBuildNumber extends MavenGameVersion {

	public String separator;
	public int build;

	public MavenGameVersionWithBuildNumber(String maven) {
		super(maven);

		if (version.contains("+build.")) {
			separator = "+build.";
		} else {
			separator = ".";
		}

		build = Integer.parseInt(version.substring(version.lastIndexOf(separator) + separator.length()));

		_gameVersion = new GameVersion(version.substring(0, version.lastIndexOf(separator)), stable);
		gameVersion = _gameVersion.id();
	}

	public String separator() {
		return separator;
	}

	public int buildNumber() {
		return build;
	}
}
