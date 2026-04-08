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

import java.util.function.Predicate;

public class Version implements Predicate<String> {

	public final String version;
	public boolean stable = false;

	public Version(String version, boolean stable) {
		this.version = version;
		this.stable = stable;
	}

	public String id() {
		return version;
	}

	public boolean isStable() {
		return stable;
	}

	public void setStable(boolean stable) {
		this.stable = stable;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof Version)) {
			return false;
		}

		Version that = (Version) o;
		return this.version.equals(that.version);
	}

	@Override
	public boolean test(String s) {
		return version.equals(s);
	}
}
