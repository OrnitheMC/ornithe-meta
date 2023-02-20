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

package net.ornithemc.meta;

import net.ornithemc.meta.data.VersionDatabase;
import net.ornithemc.meta.data.VersionDatabaseOld;
import net.ornithemc.meta.web.WebServer;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class OrnitheMeta
{

	public static volatile VersionDatabaseOld databaseOld;
	public static volatile VersionDatabase database;

	public static void main(String[] args) {

		update();

		ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
		executorService.scheduleAtFixedRate(OrnitheMeta::update, 1, 1, TimeUnit.MINUTES);

		WebServer.start();
	}

	private static void update(){
		try {
			databaseOld = VersionDatabaseOld.generate();
			database = VersionDatabase.generate();
		} catch (IOException | XMLStreamException e) {
			if(databaseOld == null || database == null){
				throw new RuntimeException(e);
			} else {
				e.printStackTrace();
			}
		}
	}

}
