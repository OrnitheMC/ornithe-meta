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

public class BaseVersion implements Predicate<String> {

	String version;
	transient String versionNoSide;
	boolean stable = false;

	public BaseVersion(String version, boolean stable) {
		this.version = version;
		this.stable = stable;
	}

	public String getVersion() {
		return version;
	}

	public String getVersionNoSide() {
		if (versionNoSide == null) {
			versionNoSide = version;

			if (versionNoSide.endsWith("-client") || versionNoSide.endsWith("-server")) {
				versionNoSide = versionNoSide.substring(0, versionNoSide.length() - 7);
			}
		}

		return versionNoSide;
	}

	public boolean isStable() {
		return stable;
	}

	public void setStable(boolean stable) {
		this.stable = stable;
	}

	@Override
	public boolean test(String s) {
		return version.equals(s);
	}
}
