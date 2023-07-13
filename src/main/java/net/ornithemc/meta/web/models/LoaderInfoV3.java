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

import com.google.gson.JsonObject;
import net.ornithemc.meta.utils.LoaderMetaV3;
import org.jetbrains.annotations.Nullable;

public class LoaderInfoV3 implements LoaderInfoBase {

	LoaderType type;
	MavenBuildVersion loader;
	MavenVersion intermediary;

	@Nullable
	JsonObject launcherMeta;

	public LoaderInfoV3(LoaderType type, MavenBuildVersion loader, MavenVersion intermediary) {
		this.type = type;
		this.loader = loader;
		this.intermediary = intermediary;
	}

	public LoaderInfoV3 populateMeta() {
		launcherMeta = LoaderMetaV3.getMeta(this);
		return this;
	}

	@Override
	public LoaderType getLoaderType() {
		return type;
	}

	@Override
	public MavenBuildVersion getLoader() {
		return loader;
	}

	public MavenVersion getIntermediary() {
		return intermediary;
	}

	public String getGame(String side) {
		String version = intermediary.getVersion();
		if (version.endsWith(side)) {
			version = version.substring(0, version.length() - (side.length() + 1));
		}

		return version;
	}

	@Nullable
	public JsonObject getLauncherMeta() {
		return launcherMeta;
	}
}