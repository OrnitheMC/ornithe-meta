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

import com.fasterxml.jackson.databind.JsonNode;
import net.ornithemc.meta.OrnitheMeta;
import net.ornithemc.meta.data.VersionDatabaseOld;
import net.ornithemc.meta.web.models.LoaderInfoBase;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;

public class LoaderMetaV2 {

	public static final File BASE_DIR = new File("metadata");

	public static JsonNode getMeta(LoaderInfoBase loaderInfo) {
		String loaderMaven = loaderInfo.getLoader().getMaven();
		String[] split = loaderMaven.split(":");
		String path = String.format("%s/%s/%s", split[0].replaceAll("\\.", "/"), split[1], split[2]);
		String filename = String.format("%s-%s.json", split[1], split[2]);

		File launcherMetaFile = new File(BASE_DIR, path + "/" + filename);
		if (!launcherMetaFile.exists()) {
			try {
				String url = String.format("%s%s/%s", VersionDatabaseOld.ORNITHE_MAVEN_URL, path, filename);
				System.out.println("Downloading " + url);
				FileUtils.copyURLToFile(new URL(url), launcherMetaFile);
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		}

		try {
			return OrnitheMeta.MAPPER.readTree(new FileReader(launcherMetaFile));
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
}
