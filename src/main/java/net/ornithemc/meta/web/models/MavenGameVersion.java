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

public class MavenGameVersion extends MavenVersion {

	public String gameVersion;
	public String environment;
	@JsonIgnore
	GameVersion _gameVersion;

	public MavenGameVersion(String maven) {
		super(maven);

		_gameVersion = new GameVersion(version, stable);
		gameVersion = _gameVersion.id();
		environment = _gameVersion.environment().id();
	}

	public GameVersion gameVersion() {
		return _gameVersion;
	}

	@Override
	public boolean test(String s) {
		return gameVersion.equals(s);
	}
}