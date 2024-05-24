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

import io.javalin.core.util.Header;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import net.ornithemc.meta.OrnitheMeta;
import net.ornithemc.meta.data.VersionDatabase;
import net.ornithemc.meta.web.models.BaseVersion;
import net.ornithemc.meta.web.models.LoaderInfoV3;
import net.ornithemc.meta.web.models.LoaderType;
import net.ornithemc.meta.web.models.MavenBuildGameVersion;
import net.ornithemc.meta.web.models.MavenBuildVersion;
import net.ornithemc.meta.web.models.MavenVersion;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vdurmont.semver4j.Semver;

public class EndpointsV3 {

	public static void setup() {

		jsonGet("", () -> OrnitheMeta.database);

		jsonGetS("/game", generation -> () -> OrnitheMeta.database.getGame(generation));
		jsonGetS("/game/intermediary", generation -> () -> compatibleGameVersions(OrnitheMeta.database.getIntermediary(generation), BaseVersion::getVersion, v -> new BaseVersion(v.getVersion(), v.isStable())));
		jsonGetS("/game/feather", generation -> () -> compatibleGameVersions(OrnitheMeta.database.getFeather(generation), MavenBuildGameVersion::getGameVersion, v -> new BaseVersion(v.getGameVersion(), v.isStable())));
		jsonGet("/game/nests", () -> compatibleGameVersions(OrnitheMeta.database.nests, MavenBuildGameVersion::getGameVersion, v -> new BaseVersion(v.getGameVersion(), v.isStable())));

		jsonGetS("/intermediary", generation -> () -> OrnitheMeta.database.getIntermediary(generation));
		jsonGetF("/intermediary/:game_version", generation -> context -> filter(context, OrnitheMeta.database.getIntermediary(generation)));

		jsonGetF("/feather", generation -> context -> withLimitSkip(context, OrnitheMeta.database.getFeather(generation)));
		jsonGetF("/feather/:game_version", generation -> context -> withLimitSkip(context, filter(context, OrnitheMeta.database.getFeather(generation))));

		jsonGet("/nests", context -> withLimitSkip(context, OrnitheMeta.database.nests));
		jsonGet("/nests/:game_version", context -> withLimitSkip(context, filter(context, OrnitheMeta.database.nests)));

		jsonGet("/fabric-loader", context -> withLimitSkip(context, OrnitheMeta.database.getLoader(LoaderType.FABRIC)));
		jsonGetF("/fabric-loader/:game_version", generation -> context -> withLimitSkip(context, getLoaderInfoAll(context, generation, LoaderType.FABRIC)));
		jsonGetF("/fabric-loader/:game_version/:loader_version", generation -> context -> getLoaderInfo(context, generation, LoaderType.FABRIC));

		jsonGet("/quilt-loader", context -> withLimitSkip(context, OrnitheMeta.database.getLoader(LoaderType.QUILT)));
		jsonGetF("/quilt-loader/:game_version", generation -> context -> withLimitSkip(context, getLoaderInfoAll(context, generation, LoaderType.QUILT)));
		jsonGetF("/quilt-loader/:game_version/:loader_version", generation -> context -> getLoaderInfo(context, generation, LoaderType.QUILT));

		jsonGet("/installer", context -> withLimitSkip(context, OrnitheMeta.database.installer));

		jsonGet("/osl", context -> withLimitSkip(context, OrnitheMeta.database.osl));
		jsonGet("/osl/:version", context -> withLimitSkip(context, getOslDependencyInfo(context)));
		jsonGet("/osl/:module/:game_version", context -> withLimitSkip(context, getOslModuleInfo(context)));
		jsonGet("/osl/:module/:game_version/:base_version", context -> withLimitSkip(context, getOslModuleInfo(context)));

		ProfileHandlerV3.setup();
	}

	private static <T> void jsonGet(String path, Supplier<T> supplier) {
		WebServer.jsonGet("/v3/versions" + path, supplier);
	}

	private static <T> void jsonGet(String path, Function<Context, T> function) {
		WebServer.jsonGet("/v3/versions" + path, function);
	}

	private static <T> void jsonGetS(String path, Function<Integer, Supplier<T>> supplier) {
		for (int generation = 1; generation <= VersionDatabase.LATEST_GENERATION; generation++) {
			Handler handler = WebServer.jsonGet("/v3/versions/gen" + generation + path, supplier.apply(generation));

			if (generation == VersionDatabase.LATEST_STABLE_GENERATION) {
				WebServer.javalin.get("/v3/versions" + path, handler);
			}
		}
	}

	private static <T> void jsonGetF(String path, Function<Integer, Function<Context, T>> function) {
		for (int generation = 1; generation <= VersionDatabase.LATEST_GENERATION; generation++) {
			Handler handler = WebServer.jsonGet("/v3/versions/gen" + generation + path, function.apply(generation));

			if (generation == VersionDatabase.LATEST_STABLE_GENERATION) {
				WebServer.javalin.get("/v3/versions" + path, handler);
			}
		}
	}

	private static <T> void jsonGetH(String path, Function<Integer, Handler> function) {
		for (int generation = 1; generation <= VersionDatabase.LATEST_GENERATION; generation++) {
			Handler handler = function.apply(generation);
			WebServer.javalin.get("/v3/versions/gen" + generation + path, handler);

			if (generation == VersionDatabase.LATEST_STABLE_GENERATION) {
				WebServer.javalin.get("/v3/versions" + path, handler);
			}
		}
	}

	private static <T> List<T> withLimitSkip(Context context, List<T> list) {
		if(list == null){
			return Collections.emptyList();
		}
		int limit = context.queryParam("limit", Integer.class, "0").check(i -> i >= 0).get();
		int skip = context.queryParam("skip", Integer.class, "0").check(i -> i >= 0).get();

		Stream<T> listStream = list.stream().skip(skip);

		if (limit > 0) {
			listStream = listStream.limit(limit);
		}

		return listStream.collect(Collectors.toList());
	}

	private static <T extends Predicate<String>> List<T> filter(Context context, List<T> versionList) {
		if (!context.pathParamMap().containsKey("game_version")) {
			return Collections.emptyList();
		}
		return versionList.stream().filter(t -> t.test(context.pathParam("game_version"))).collect(Collectors.toList());

	}

	private static Object getLoaderInfo(Context context, int generation, LoaderType type) {
		if (!context.pathParamMap().containsKey("game_version")) {
			return null;
		}
		if (!context.pathParamMap().containsKey("loader_version")) {
			return null;
		}

		String gameVersion = context.pathParam("game_version");
		String loaderVersion = context.pathParam("loader_version");

		MavenBuildVersion loader = OrnitheMeta.database.getAllLoader(type).stream()
			.filter(mavenBuildVersion -> loaderVersion.equals(mavenBuildVersion.getVersion()))
			.findFirst().orElse(null);

		MavenVersion mappings = OrnitheMeta.database.getIntermediary(generation).stream()
			.filter(t -> t.test(gameVersion))
			.findFirst().orElse(null);

		if (loader == null) {
			context.status(400);
			return "no loader version found for " + gameVersion;
		}
		if (mappings == null) {
			context.status(400);
			return "no mappings version found for " + gameVersion;
		}
		return new LoaderInfoV3(type, loader, mappings).populateMeta();
	}

	private static List<?> getLoaderInfoAll(Context context, int generation, LoaderType type) {
		if (!context.pathParamMap().containsKey("game_version")) {
			return null;
		}
		String gameVersion = context.pathParam("game_version");

		MavenVersion mappings = OrnitheMeta.database.getIntermediary(generation).stream()
			.filter(t -> t.test(gameVersion))
			.findFirst().orElse(null);

		if(mappings == null){
			return Collections.emptyList();
		}

		List<LoaderInfoV3> infoList = new ArrayList<>();

		for(MavenBuildVersion loader : OrnitheMeta.database.getLoader(type)){
			infoList.add(new LoaderInfoV3(type, loader, mappings).populateMeta());
		}
		return infoList;
	}

	private static <T extends BaseVersion> List<BaseVersion> compatibleGameVersions(List<T> list, Function<T, String> gameVersionSupplier, Function<T, BaseVersion> baseVersionSupplier){
		List<BaseVersion> versions = new ArrayList<>();
		Predicate<String> contains = s -> versions.stream().anyMatch(baseVersion -> baseVersion.getVersion().equals(s));

		for(T entry : list){
			if (!contains.test(gameVersionSupplier.apply(entry))){
				versions.add(baseVersionSupplier.apply(entry));
			}
		}

		return versions;
	}

	private static List<?> getOslDependencyInfo(Context context) {
		if (!context.pathParamMap().containsKey("version")) {
			return null;
		}

		String version = context.pathParam("version");
		List<MavenVersion> versions = OrnitheMeta.database.getOslDependencies(version);

		return versions;
	}

	private static List<?> getOslModuleInfo(Context context) {
		if (!context.pathParamMap().containsKey("module")) {
			return null;
		}
		if (!context.pathParamMap().containsKey("game_version")) {
			return null;
		}

		String module = context.pathParam("module");
		String gameVersion = context.pathParam("game_version");
		List<MavenVersion> versions = OrnitheMeta.database.getOslModule(module);

		if (context.pathParamMap().containsKey("base_version")) {
			String baseVersion = context.pathParam("base_version");

			versions = versions.stream()
					.filter(v -> v.getVersion().startsWith(baseVersion))
					.collect(Collectors.toList());
		}

		versions = versions.stream()
				.filter(version -> {
					String minGameVersion = null;
					String maxGameVersion = null;

					String buildVersion = version.getVersion();
					String[] parts = buildVersion.split("mc");

					if (parts.length == 2) { // old format: <base version>+mc<min mc version>#<max mc version>
						parts = parts[1].split("[#]");

						if (parts.length == 2) {
							minGameVersion = parts[0];
							maxGameVersion = parts[1];
						}
					} else if (parts.length == 3) { // new format: <base version>+mc<min mc version>-mc<max mc version>
						minGameVersion = parts[1].substring(0, parts[1].length() - 1);
						maxGameVersion = parts[2];
					} else { // module without mc dependency
						return true;
					}

					try {
						Semver v = OrnitheMeta.database.manifest.get(gameVersion);
						Semver vmin = OrnitheMeta.database.manifest.get(minGameVersion);
						Semver vmax = OrnitheMeta.database.manifest.get(maxGameVersion);

						return v.compareTo(vmin) >= 0 && v.compareTo(vmax) <= 0;
					} catch (NoSuchElementException e) {
						return false;
					}
				})
				.collect(Collectors.toList());

		return versions;
	}

	public static void fileDownload(LoaderType type, String path, String ext, BiFunction<Integer, LoaderInfoV3, String> fileNameFunction, BiFunction<Integer, LoaderInfoV3, CompletableFuture<InputStream>> streamSupplier) {
		jsonGetH("/" + type.getName() + "-loader/:game_version/:loader_version/" + path + "/" + ext, generation -> ctx -> {
			Object obj = getLoaderInfo(ctx, generation, type);

			if (obj instanceof String) {
				ctx.result((String) obj);
			} else if (obj instanceof LoaderInfoV3) {
				LoaderInfoV3 versionInfo = (LoaderInfoV3) obj;

				CompletableFuture<InputStream> streamFuture = streamSupplier.apply(generation, versionInfo);

				if (ext.equals("zip")) {
					//Set the filename to download
					ctx.header(Header.CONTENT_DISPOSITION, String.format("attachment; filename=\"%s\"", fileNameFunction.apply(generation, versionInfo)));

					ctx.contentType("application/zip");
				} else {
					ctx.contentType("application/json");
				}

				//Cache for a day
				ctx.header(Header.CACHE_CONTROL, "public, max-age=86400");

				ctx.result(streamFuture);
			} else {
				ctx.result("An internal error occurred");
			}
		});
	}
}
