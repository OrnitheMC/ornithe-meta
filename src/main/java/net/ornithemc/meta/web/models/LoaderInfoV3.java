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

import com.fasterxml.jackson.databind.JsonNode;
import net.ornithemc.meta.utils.LoaderMetaV3;
import org.jetbrains.annotations.Nullable;

public class LoaderInfoV3 implements LoaderInfoBase {

	public final LoaderType type;
	public final MavenVersion loader;

	@Nullable
	JsonNode launcherMeta;

	public LoaderInfoV3(LoaderType type, MavenVersion loader) {
		this.type = type;
		this.loader = loader;
	}

	public LoaderInfoV3 populateMeta() {
		launcherMeta = LoaderMetaV3.getMeta(this);
		return this;
	}

	@Override
	public LoaderType loaderType() {
		return type;
	}

	@Override
	public MavenVersion loaderVersion() {
		return loader;
	}

	@Nullable
	public JsonNode getLauncherMeta() {
		return launcherMeta;
	}
}