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
import com.vdurmont.semver4j.Semver;

import net.ornithemc.meta.OrnitheMeta;
import net.ornithemc.meta.data.VersionDatabase;
import net.ornithemc.meta.utils.VersionManifest;
import net.ornithemc.meta.web.models.GameEnvironment;
import net.ornithemc.meta.web.models.GameVersion;
import net.ornithemc.meta.web.models.InstallationInfoV3;
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
import java.util.Optional;
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
		EndpointsV3.fileDownload(type, GameEnvironment.CLIENT, "profile", "json", ProfileHandlerV3::getJsonFileName, ProfileHandlerV3::profileJson);
		EndpointsV3.fileDownload(type, GameEnvironment.CLIENT, "profile", "zip", ProfileHandlerV3::getZipFileName, ProfileHandlerV3::profileZip);

		EndpointsV3.fileDownload(type, GameEnvironment.SERVER, "server", "json", ProfileHandlerV3::getJsonFileName, ProfileHandlerV3::serverJson);
	}

	private static String getJsonFileName(int generation, InstallationInfoV3 info) {
		return getJsonFileName(generation, info, "json");
	}

	private static String getZipFileName(int generation, InstallationInfoV3 info) {
		return getJsonFileName(generation, info, "zip");
	}

	private static String getJsonFileName(int generation, InstallationInfoV3 info, String ext) {
		return String.format("%s-loader-%s-%s-ornithe-gen%d.%s",
				info.loaderInfo().loaderType().getName(),
				info.loaderInfo().loaderVersion().id(),
				info.gameVersion().id(),
				generation,
				ext);
	}

	private static CompletableFuture<InputStream> profileJson(int generation, InstallationInfoV3 info) {
		return CompletableFuture.supplyAsync(() -> getProfileJsonStream(generation, info), EXECUTOR);
	}

	private static CompletableFuture<InputStream> serverJson(int generation, InstallationInfoV3 info) {
		return CompletableFuture.supplyAsync(() -> getProfileJsonStream(generation, info), EXECUTOR);
	}

	private static CompletableFuture<InputStream> profileZip(int generation, InstallationInfoV3 info) {
		return profileJson(generation, info)
				.thenApply(inputStream -> packageZip(generation, info, inputStream));
	}

	private static InputStream packageZip(int generation, InstallationInfoV3 info, InputStream profileJson) {
		String profileName = String.format("%s-loader-%s-%s-ornithe-gen%d",
				info.loaderInfo().loaderType().getName(),
				info.loaderInfo().loaderVersion().id(),
				info.gameVersion().id(),
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

	private static InputStream getProfileJsonStream(int generation, InstallationInfoV3 info) {
		JsonNode jsonNode = buildProfileJson(generation, info);
		return new ByteArrayInputStream(jsonNode.toString().getBytes());
	}

	//This is based of the installer code.
	private static JsonNode buildProfileJson(int generation, InstallationInfoV3 info) {
		GameVersion game = info.gameVersion();
		LoaderInfoV3 loader = info.loaderInfo();

		String profileName = String.format("%s-loader-%s-%s-ornithe-gen%d",
				loader.loaderType().getName(),
				loader.loaderVersion().id(),
				game.id(),
				generation);

		JsonNode launcherMeta = loader.getLauncherMeta();
		JsonNode librariesNode = launcherMeta.get("libraries");
		// Build the libraries array with the existing libs + loader and intermediary
		ArrayNode libraries = (ArrayNode) librariesNode.get("common");
		if (info.intermediaryVersion() != null) {
			libraries.add(getLibrary(info.intermediaryVersion().mavenNotation(), VersionDatabase.ORNITHE_MAVEN_URL));
		}
		libraries.add(getLibrary(info.loaderInfo().loaderVersion().mavenNotation(), info.loaderInfo().loaderType().getMavenUrl()));

		if (librariesNode.has(game.environment().id())) {
			libraries.addAll((ArrayNode) librariesNode.get(game.environment().id()));
		}

		String currentTime = ISO_8601.format(new Date());

		ObjectNode profile = OrnitheMeta.MAPPER.createObjectNode();
		profile.put("id", profileName);
		profile.put("inheritsFrom", String.format("%s-vanilla", game.id()));
		profile.put("releaseTime", currentTime);
		profile.put("time", currentTime);
		profile.put("type", "release");

		JsonNode mainClassNode = launcherMeta.get("mainClass");
		String mainClass;

		if (mainClassNode.isObject()) {
			mainClass = mainClassNode.get(game.environment().id()).asText();
		} else {
			mainClass = mainClassNode.asText();
		}

		profile.put("mainClass", mainClass);

		if (game.environment() == GameEnvironment.SERVER && launcherMeta.has("mainClass") && launcherMeta.get("mainClass").has("serverLauncher")) {
			// Add the server launch main class
			profile.put("launcherMainClass", launcherMeta.get("mainClass").get("serverLauncher").asText());
		}

		ObjectNode arguments = OrnitheMeta.MAPPER.createObjectNode();
		ArrayNode jvmArgs = arguments.putArray("jvm");
		// I believe this is required to stop the launcher from complaining
		arguments.putArray("game");

		if (generation >= 2) {
			// value not needed for FLoader, but QLoader requires the value to be true
			jvmArgs.add(loader.loaderType().getJvmArguments().fixPackageAccess(true));
		}
		jvmArgs.add(loader.loaderType().getJvmArguments().gameVersion(game.id()));

		profile.set("arguments", arguments);
		profile.set("libraries", libraries);

		VersionManifest manifest = VersionDatabase.getManifest(generation);

		Semver version = manifest.normalize(game.id());
		Semver boundary = manifest.normalize("17w15a");

		if (version.compareTo(boundary) < 0 && game.environment() == GameEnvironment.CLIENT) {
			Optional<Semver> log4jUpgrade = OrnitheMeta.database.libraryUpgrades.stream()
				.filter(l -> l.test(generation, game.id()))
				.filter(l -> l.name.split("[:]")[1].equals("log4j-core"))
				.findFirst()
				.map(l -> new Semver(l.name.split("[:]")[2]));

			if (log4jUpgrade.isPresent()) {
				Semver log4jVersion = log4jUpgrade.get();
				Semver log4jBoundary = new Semver("2.8.1");

				if (log4jVersion.compareTo(log4jBoundary) >= 0) {
					profile.set("logging", manifest.getLoggingConfig("17w15a"));
				}
			}
		}

		return profile;
	}

	private static JsonNode getLibrary(String mavenPath, String url) {
		ObjectNode objectNode = OrnitheMeta.MAPPER.createObjectNode();
		objectNode.put("name", mavenPath);
		objectNode.put("url", url);
		return objectNode;
	}
}
