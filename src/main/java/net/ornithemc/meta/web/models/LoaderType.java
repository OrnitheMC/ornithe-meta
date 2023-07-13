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

import net.ornithemc.meta.data.VersionDatabase;

public enum LoaderType {

	FABRIC("fabric", VersionDatabase.FABRIC_MAVEN_URL),
	QUILT("quilt", VersionDatabase.QUILT_MAVEN_URL),
	ORNITHE("ornithe", VersionDatabase.ORNITHE_MAVEN_URL);

	private final String name;
	private final String maven;

	private LoaderType(String name, String maven) {
		this.name = name;
		this.maven = maven;
	}

	public String getName() {
		return name;
	}

	public String getMavenUrl() {
		return maven;
	}
}
