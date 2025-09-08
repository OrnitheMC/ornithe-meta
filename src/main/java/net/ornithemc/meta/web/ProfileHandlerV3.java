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
import net.ornithemc.meta.data.VersionDatabase;
import net.ornithemc.meta.web.models.LoaderInfoV3;
import net.ornithemc.meta.web.models.LoaderType;
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

public class ProfileHandlerV3 {

	private static final Executor EXECUTOR = Executors.newFixedThreadPool(2);
	private static final DateFormat ISO_8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

	public static void setup() {
		setup(LoaderType.FABRIC);
		setup(LoaderType.QUILT);
	}

	private static void setup(LoaderType type) {
		EndpointsV3.fileDownload(type, "profile", "json", ProfileHandlerV3::getJsonFileName, ProfileHandlerV3::profileJson);
		EndpointsV3.fileDownload(type, "profile", "zip", ProfileHandlerV3::getZipFileName, ProfileHandlerV3::profileZip);

		EndpointsV3.fileDownload(type, "server", "json", ProfileHandlerV3::getJsonFileName, ProfileHandlerV3::serverJson);
	}

	private static String getJsonFileName(int generation, LoaderInfoV3 info) {
		return getJsonFileName(generation, info, "json");
	}

	private static String getZipFileName(int generation, LoaderInfoV3 info) {
		return getJsonFileName(generation, info, "zip");
	}

	private static String getJsonFileName(int generation, LoaderInfoV3 info, String ext) {
		return String.format("%s-loader-%s-%s-ornithe-gen%d.%s",
				info.getLoaderType().getName(),
				info.getLoader().getVersion(),
				info.getIntermediary().getVersion(),
				generation,
				ext);
	}

	private static CompletableFuture<InputStream> profileJson(int generation, LoaderInfoV3 info) {
		return CompletableFuture.supplyAsync(() -> getProfileJsonStream(generation, info, "client"), EXECUTOR);
	}

	private static CompletableFuture<InputStream> serverJson(int generation, LoaderInfoV3 info) {
		return CompletableFuture.supplyAsync(() -> getProfileJsonStream(generation, info, "server"), EXECUTOR);
	}

	private static CompletableFuture<InputStream> profileZip(int generation, LoaderInfoV3 info) {
		return profileJson(generation, info)
				.thenApply(inputStream -> packageZip(generation, info, inputStream));
	}

	private static InputStream packageZip(int generation, LoaderInfoV3 info, InputStream profileJson) {
		String profileName = String.format("%s-loader-%s-%s-ornithe-gen%d",
				info.getLoaderType().getName(),
				info.getLoader().getVersion(),
				info.getIntermediary().getVersion(),
				generation);

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

	private static InputStream getProfileJsonStream(int generation, LoaderInfoV3 info, String side) {
		JsonNode jsonNode = buildProfileJson(generation, info, side);
		return new ByteArrayInputStream(jsonNode.toString().getBytes());
	}

	//This is based of the installer code.
	private static JsonNode buildProfileJson(int generation, LoaderInfoV3 info, String side) {
		JsonNode launcherMeta = info.getLauncherMeta();

		String profileName = String.format("%s-loader-%s-%s-ornithe-gen%d",
				info.getLoaderType().getName(),
				info.getLoader().getVersion(),
				info.getGame(side),
				generation);

		JsonNode librariesNode = launcherMeta.get("libraries");
		// Build the libraries array with the existing libs + loader and intermediary
		ArrayNode libraries = (ArrayNode) librariesNode.get("common");
		libraries.add(getLibrary(info.getIntermediary().getMaven(), VersionDatabase.ORNITHE_MAVEN_URL));
		libraries.add(getLibrary(info.getLoader().getMaven(), info.getLoaderType().getMavenUrl()));

		if (librariesNode.has(side)) {
			libraries.addAll((ArrayNode) librariesNode.get(side));
		}

		String currentTime = ISO_8601.format(new Date());

		ObjectNode profile = OrnitheMeta.MAPPER.createObjectNode();
		profile.put("id", profileName);
		profile.put("inheritsFrom", String.format("%s-vanilla", info.getGame(side)));
		profile.put("releaseTime", currentTime);
		profile.put("time", currentTime);
		profile.put("type", "release");

		JsonNode mainClassNode = launcherMeta.get("mainClass");
		String mainClass;

		if (mainClassNode.isObject()) {
			mainClass = mainClassNode.get(side).asText();
		} else {
			mainClass = mainClassNode.asText();
		}

		profile.put("mainClass", mainClass);

		if ("server".equals(side) && launcherMeta.has("mainClass") && launcherMeta.get("mainClass").has("serverLauncher")) {
			// Add the server launch main class
			profile.put("launcherMainClass", launcherMeta.get("mainClass").get("serverLauncher").asText());
		}

		ObjectNode arguments = OrnitheMeta.MAPPER.createObjectNode();

		// I believe this is required to stop the launcher from complaining
		arguments.putArray("game");

		profile.set("arguments", arguments);

		profile.put("libraries", libraries);

		return profile;
	}

	private static JsonNode getLibrary(String mavenPath, String url) {
		ObjectNode objectNode = OrnitheMeta.MAPPER.createObjectNode();
		objectNode.put("name", mavenPath);
		objectNode.put("url", url);
		return objectNode;
	}
}
