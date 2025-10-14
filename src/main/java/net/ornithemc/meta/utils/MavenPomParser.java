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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class MavenPomParser {

	public String mavenUrl;
	public String groupId;
	public String artifactId;
	public boolean require;

	public MavenPomParser(String mavenUrl, String groupId, String artifactId) {
		this(mavenUrl, groupId, artifactId, true);
	}

	public MavenPomParser(String mavenUrl, String groupId, String artifactId, boolean require) {
		this.mavenUrl = mavenUrl;
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.require = require;
	}

	public <T extends BaseVersion> List<T> getDependencies(Function<String, T> factory, String version) throws IOException, XMLStreamException {
		return getDependencies(factory, version, dependency -> true);
	}

	public <T extends BaseVersion> List<T> getDependencies(Function<String, T> factory, String version, DependencyFilter<T> filter) throws IOException, XMLStreamException {
		List<T> versions = new ArrayList<>();

		try {
			URL url = new URL(mavenUrl + groupId.replace('.', '/') + "/" + artifactId + "/" + version + "/" + artifactId + "-" + version + ".pom");
			XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(url.openStream());
			while (reader.hasNext()) {
				if (reader.next() == XMLStreamConstants.START_ELEMENT && reader.getLocalName().equals("dependency")) {
					String depGroup = null;
					String depArtifact = null;
					String depVersion = null;

					dependencyLoop: while (reader.hasNext()) {
						switch (reader.next()) {
						case XMLStreamConstants.START_ELEMENT:
							switch (reader.getLocalName()) {
							case "groupId":
								depGroup = reader.getElementText();
								break;
							case "artifactId":
								depArtifact = reader.getElementText();
								break;
							case "version":
								depVersion = reader.getElementText();
								break;
							}

							break;
						case XMLStreamConstants.END_ELEMENT:
							if (reader.getLocalName().equals("dependency")) {
								break dependencyLoop;
							}
						}
					}

					String maven = String.format("%s:%s:%s", depGroup, depArtifact, depVersion);
					T build = factory.apply(maven);

					if (filter.test(build)) {
						versions.add(build);
					}
				}
			}
			reader.close();
		} catch (IOException e){
			if (this.require) {
				throw new IOException("Failed to load " + mavenUrl + " " + groupId + ":" + artifactId + ":" + version, e);
			}

			versions.clear();
		}

		return Collections.unmodifiableList(versions);
	}
	
	public interface DependencyFilter<T extends BaseVersion> {
		boolean test(T version);
	}

}
