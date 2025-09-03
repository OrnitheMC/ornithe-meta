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

package net.ornithemc.meta.web;

import java.util.ArrayList;
import java.util.List;

import net.ornithemc.meta.data.VersionDatabase;
import net.ornithemc.meta.web.models.LibraryV3;

public class LibraryUpgradesV3 {

	public static List<LibraryV3> get() {
		List<LibraryV3> libraries = new ArrayList<>();

		// the slf4j binding for log4j - this version has been tested to work on 1.6, 1.11, 1.12
		// lower versions of mc not tested because they did not yet ship log4j to begin with
		libraries.add(LibraryV3.of("org.slf4j:slf4j-api:2.0.1"));
		libraries.add(LibraryV3.of("org.apache.logging.log4j:log4j-slf4j2-impl:2.19.0"));
		libraries.add(LibraryV3.of("org.apache.logging.log4j:log4j-api:2.19.0"));
		libraries.add(LibraryV3.of("org.apache.logging.log4j:log4j-core:2.19.0"));
		libraries.add(LibraryV3.of("it.unimi.dsi:fastutil:8.5.9"));
		libraries.add(LibraryV3.of("com.google.code.gson:gson:2.10"));

		// logger-config is needed to make log4j work in versions prior to 13w39a
		libraries.add(LibraryV3.of("net.ornithemc:logger-config:1.0.0").setUrl(VersionDatabase.ORNITHE_MAVEN_URL).setMaxGameVersion("13w38c"));
		libraries.add(LibraryV3.of("com.google.guava:guava:14.0").setMaxGameVersion("1.5.2"));
		libraries.add(LibraryV3.of("commons-codec:commons-codec:1.9").setMaxGameVersion("1.7.5"));
		libraries.add(LibraryV3.of("org.apache.commons:commons-compress:1.8.1").setMaxGameVersion("1.7.10"));
		libraries.add(LibraryV3.of("commons-io:commons-io:2.4").setMaxGameVersion("1.5.2"));
		libraries.add(LibraryV3.of("org.apache.commons:commons-lang3:3.1").setMaxGameVersion("1.5.2"));
		libraries.add(LibraryV3.of("commons-logging:commons-logging:1.1.3").setMaxGameVersion("1.7.10"));
		libraries.add(LibraryV3.of("org.apache.httpcomponents:httpcore:4.3.2").setMaxGameVersion("1.7.9"));
		libraries.add(LibraryV3.of("org.apache.httpcomponents:httpclient:4.3.3").setMaxGameVersion("1.7.9"));

		return libraries;
	}
}
