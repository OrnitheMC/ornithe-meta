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

public class GameVersion extends Version {

	// TODO: fill this properly
	// unfortunately it requires parsing the version details for ALL versions
	@JsonIgnore
	String environment;
	@JsonIgnore
	GameEnvironment _environment;

	public GameVersion(String key) {
		this(key, false);
	}

	public GameVersion(String key, boolean stable) {
		this(key.substring(0, key.length() - GameEnvironment.fromVersion(key).suffix().length()), GameEnvironment.fromVersion(key), stable);
	}

	public GameVersion(String version, GameEnvironment environment, boolean stable) {
		super(version, stable);

		this._environment = environment;
		this.environment = this._environment.id();
	}

	public GameEnvironment environment() {
		return _environment;
	}

	public GameVersion setEnvironment(GameEnvironment environment) {
		this._environment = environment;
		this.environment = this._environment.id();
		return this;
	}

	public String key() {
		return version + _environment.suffix();
	}

	public GameVersion merge(GameVersion that) {
		return new GameVersion(this.version, this._environment.merge(that._environment), this.stable && that.stable);
	}
}
