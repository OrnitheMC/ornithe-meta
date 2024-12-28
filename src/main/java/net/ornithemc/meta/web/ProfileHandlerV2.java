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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ornithemc.meta.OrnitheMeta;
import net.ornithemc.meta.data.VersionDatabaseOld;
import net.ornithemc.meta.web.models.LoaderInfoV2;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ProfileHandlerV2 {

	private static final Executor EXECUTOR = Executors.newFixedThreadPool(2);
	private static final DateFormat ISO_8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

	public static void setup() {
		EndpointsV2.fileDownload("profile", "json", ProfileHandlerV2::getJsonFileName, ProfileHandlerV2::profileJson);
		EndpointsV2.fileDownload("profile", "zip", ProfileHandlerV2::getZipFileName, ProfileHandlerV2::profileZip);

		EndpointsV2.fileDownload("server", "json", ProfileHandlerV2::getJsonFileName, ProfileHandlerV2::serverJson);
	}

	private static String getJsonFileName(LoaderInfoV2 info) {
		return getJsonFileName(info, "json");
	}

	private static String getZipFileName(LoaderInfoV2 info) {
		return getJsonFileName(info, "zip");
	}

	private static String getJsonFileName(LoaderInfoV2 info, String ext) {
		return String.format("ornithe-loader-%s-%s.%s", info.getLoader().getVersion(), info.getCalamus().getVersion(), ext);
	}

	private static CompletableFuture<InputStream> profileJson(LoaderInfoV2 info) {
		return CompletableFuture.supplyAsync(() -> getProfileJsonStream(info, "client"), EXECUTOR);
	}

	private static CompletableFuture<InputStream> serverJson(LoaderInfoV2 info) {
		return CompletableFuture.supplyAsync(() -> getProfileJsonStream(info, "server"), EXECUTOR);
	}

	private static CompletableFuture<InputStream> profileZip(LoaderInfoV2 info) {
		return profileJson(info)
				.thenApply(inputStream -> packageZip(info, inputStream));
	}

	private static InputStream packageZip(LoaderInfoV2 info, InputStream profileJson) {
		String profileName = String.format("ornithe-loader-%s-%s", info.getLoader().getVersion(), info.getCalamus().getVersion());

		try {
			ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

			try (ZipOutputStream zipStream = new ZipOutputStream(byteArrayOutputStream)) {
				//Write the profile json
				zipStream.putNextEntry(new ZipEntry(profileName + "/" + profileName + ".json"));
				IOUtils.copy(profileJson, zipStream);
				zipStream.closeEntry();

				//Write the dummy jar file
				zipStream.putNextEntry(new ZipEntry(profileName + "/" + profileName + ".jar"));
				zipStream.closeEntry();
			}

			//Is this really the best way??
			return new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
		} catch (IOException e) {
			throw new CompletionException(e);
		}
	}

	private static InputStream getProfileJsonStream(LoaderInfoV2 info, String side) {
		JsonNode jsonNode = buildProfileJson(info, side);
		return new ByteArrayInputStream(jsonNode.toString().getBytes());
	}

	//This is based of the installer code.
	private static JsonNode buildProfileJson(LoaderInfoV2 info, String side) {
		JsonNode launcherMeta = info.getLauncherMeta();

		String profileName = String.format("ornithe-loader-%s-%s", info.getLoader().getVersion(), info.getCalamus().getVersion());

		JsonNode librariesNode = launcherMeta.get("libraries");
		// Build the libraries array with the existing libs + loader and calamus
		ArrayNode libraries = (ArrayNode) librariesNode.get("common");
		libraries.add(getLibrary(info.getCalamus().getMaven(), VersionDatabaseOld.ORNITHE_MAVEN_URL));
		libraries.add(getLibrary(info.getLoader().getMaven(), VersionDatabaseOld.ORNITHE_MAVEN_URL));

		if (librariesNode.has(side)) {
			libraries.addAll((ArrayNode) librariesNode.get(side));
		}

		String currentTime = ISO_8601.format(new Date());

		ObjectNode profile = OrnitheMeta.MAPPER.createObjectNode();
		profile.put("id", profileName);
		profile.put("inheritsFrom", info.getCalamus().getVersion());
		profile.put("releaseTime", currentTime);
		profile.put("time", currentTime);
		profile.put("type", "release");

		JsonNode mainClassElement = launcherMeta.get("mainClass");
		String mainClass;

		if (mainClassElement.isObject()) {
			mainClass = mainClassElement.get(side).asText();
		} else {
			mainClass = mainClassElement.asText();
		}

		profile.put("mainClass", mainClass);

		ObjectNode arguments = OrnitheMeta.MAPPER.createObjectNode();

		// I believe this is required to stop the launcher from complaining
		arguments.putArray("game");

		profile.set("arguments", arguments);

		profile.putArray("libraries").addAll(libraries);

		return profile;
	}

	private static JsonNode getLibrary(String mavenPath, String url) {
		ObjectNode objectNode = OrnitheMeta.MAPPER.createObjectNode();
		objectNode.put("name", mavenPath);
		objectNode.put("url", url);
		return objectNode;
	}
}
