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

package net.ornithemc.meta.utils;

import net.ornithemc.meta.web.models.BaseVersion;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class MavenMetadataParser {

	public String mavenUrl;
	public String groupId;
	public String artifactId;
	public boolean require;

	public MavenMetadataParser(String mavenUrl, String groupId, String artifactId) {
		this(mavenUrl, groupId, artifactId, true);
	}

	public MavenMetadataParser(String mavenUrl, String groupId, String artifactId, boolean require) {
		this.mavenUrl = mavenUrl;
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.require = require;
	}

	public <T extends BaseVersion> List<T> getVersions(Function<String, T> function) throws IOException, XMLStreamException {
		return getVersions(function, list -> {
			if (!list.isEmpty()) list.get(0).setStable(true);
		});
	}

	public <T extends BaseVersion> List<T> getVersions(Function<String, T> function, StableVersionIdentifier stableIdentifier) throws IOException, XMLStreamException {
		List<T> versions = new ArrayList<>();

		try {
			URL url = new URL(mavenUrl + groupId.replace('.', '/') + "/" + artifactId + "/maven-metadata.xml");
			XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(url.openStream());
			while (reader.hasNext()) {
				if (reader.next() == XMLStreamConstants.START_ELEMENT && reader.getLocalName().equals("version")) {
					String version = reader.getElementText();
					String maven = String.format("%s:%s:%s", groupId, artifactId, version);

					versions.add(function.apply(maven));
				}
			}
			reader.close();
			Collections.reverse(versions);
		} catch (IOException e){
			if (require) {
				throw new IOException("Failed to load " + mavenUrl + " " + groupId + ":" + artifactId, e);
			}

			versions.clear();
		}

		Path unstableVersionsPath = Paths.get(groupId.replace('.', '_') + "_" + artifactId + ".txt");

		if (Files.exists(unstableVersionsPath)) {
			// Read a file containing a new line separated list of versions that should not be marked as stable.
			List<String> unstableVersions = Files.readAllLines(unstableVersionsPath);
			versions.stream()
					.filter(v -> !unstableVersions.contains(v.getVersion()))
					.findFirst()
					.ifPresent(v -> v.setStable(true));
		} else {
			stableIdentifier.process(versions);
		}

		return Collections.unmodifiableList(versions);
	}
	
	public interface StableVersionIdentifier {
		void process(List<? extends BaseVersion> versions);
	}

}
