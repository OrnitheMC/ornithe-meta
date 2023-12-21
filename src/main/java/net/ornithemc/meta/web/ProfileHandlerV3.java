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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.apache.commons.io.IOUtils;

import net.ornithemc.meta.web.models.LoaderInfoV3;
import net.ornithemc.meta.web.models.LoaderType;

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

	private static String getJsonFileName(LoaderInfoV3 info) {
		return getJsonFileName(info, "json");
	}

	private static String getZipFileName(LoaderInfoV3 info) {
		return getJsonFileName(info, "zip");
	}

	private static String getJsonFileName(LoaderInfoV3 info, String ext) {
		return String.format("%s-loader-%s-%s-ornithe.%s",
				info.getLoaderType().getName(),
				info.getLoader().getVersion(),
				info.getIntermediary().getVersion(),
				ext);
	}

	private static CompletableFuture<InputStream> profileJson(LoaderInfoV3 info) {
		return CompletableFuture.supplyAsync(() -> getProfileJsonStream(info, "client"), EXECUTOR);
	}

	private static CompletableFuture<InputStream> serverJson(LoaderInfoV3 info) {
		return CompletableFuture.supplyAsync(() -> getProfileJsonStream(info, "server"), EXECUTOR);
	}

	private static CompletableFuture<InputStream> profileZip(LoaderInfoV3 info) {
		return profileJson(info)
				.thenApply(inputStream -> packageZip(info, inputStream));
	}

	private static InputStream packageZip(LoaderInfoV3 info, InputStream profileJson)  {
		String profileName = String.format("%s-loader-%s-%s-ornithe",
				info.getLoaderType().getName(),
				info.getLoader().getVersion(), 
				info.getIntermediary().getVersion());

		try {
			ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

			try (ZipOutputStream zipStream = new ZipOutputStream(byteArrayOutputStream))  {
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

	private static InputStream getProfileJsonStream(LoaderInfoV3 info, String side) {
		JsonObject jsonObject = buildProfileJson(info, side);
		return new ByteArrayInputStream(jsonObject.toString().getBytes());
	}

	//This is based of the installer code.
	private static JsonObject buildProfileJson(LoaderInfoV3 info, String side) {
		JsonObject launcherMeta = info.getLauncherMeta();

		String profileName = String.format("%s-loader-%s-%s-ornithe",
				info.getLoaderType().getName(),
				info.getLoader().getVersion(),
				info.getGame(side));

		JsonArray libraries = ProfileLibraryManager.getLibraries(info, side);

		String currentTime = ISO_8601.format(new Date());

		JsonObject profile = new JsonObject();
		profile.addProperty("id", profileName);
		profile.addProperty("inheritsFrom", String.format("%s-vanilla", info.getGame(side)));
		profile.addProperty("releaseTime", currentTime);
		profile.addProperty("time", currentTime);
		profile.addProperty("type", "release");

		JsonElement mainClassElement = launcherMeta.get("mainClass");
		String mainClass;

		if (mainClassElement.isJsonObject()) {
			mainClass = mainClassElement.getAsJsonObject().get(side).getAsString();
		} else {
			mainClass = mainClassElement.getAsString();
		}

		profile.addProperty("mainClass", mainClass);

		if ("server".equals(side) && launcherMeta.has("mainClass") && launcherMeta.get("mainClass").getAsJsonObject().has("serverLauncher")) {
			// Add the server launch main class
			profile.addProperty("launcherMainClass", launcherMeta.get("mainClass").getAsJsonObject().get("serverLauncher").getAsString());
		}

		JsonObject arguments = new JsonObject();

		// I believe this is required to stop the launcher from complaining
		arguments.add("game", new JsonArray());

		profile.add("arguments", arguments);

		profile.add("libraries", libraries);

		return profile;
	}
}
