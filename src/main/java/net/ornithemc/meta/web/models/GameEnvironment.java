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

import java.util.Locale;

public enum GameEnvironment {

	CLIENT, SERVER, BOTH;

	public String id() {
		return this == BOTH ? "*" : name().toLowerCase(Locale.ROOT);
	}

	public String suffix() {
		return this == BOTH ? "" : ("-" + this.name().toLowerCase());
	}

	public GameEnvironment merge(GameEnvironment that) {
		if (this == that) {
			return this;
		}
		if (this == BOTH || that == BOTH) {
			return BOTH;
		}
		if (this == CLIENT && that == SERVER) {
			return BOTH;
		}
		if (this == SERVER && that == CLIENT) {
			return BOTH;
		}

		throw new IllegalStateException("Don't know how to merge " + this.id() + " and " + that.id());
	}

	public static GameEnvironment fromVersion(String gameVersion) {
		for (GameEnvironment env : GameEnvironment.values()) {
			if (!env.suffix().isEmpty() && gameVersion.endsWith(env.suffix())) {
				return env;
			}
		}

		return BOTH;
	}
}
