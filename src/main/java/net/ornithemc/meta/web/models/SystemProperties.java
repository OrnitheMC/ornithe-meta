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

public class SystemProperties {

	String fixPackageAccess;
	String gameVersion;

	public SystemProperties(String fixPackageAccess, String gameVersion) {
		this.fixPackageAccess = fixPackageAccess;
		this.gameVersion = gameVersion;
	}

	public String fixPackageAccess() {
		return this.fixPackageAccess;
	}

	public String gameVersion() {
		return this.gameVersion;
	}
}
