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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;

public class FileCache {

	private static final MessageDigest SHA1_DIGEST;

	static {
		try {
			SHA1_DIGEST = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("no bueno", e);
		}
	}

	public static final Duration ONE_HOUR = Duration.ofHours(1L);
	public static final Duration ONE_DAY = Duration.ofDays(1L);

	public static boolean shouldPrioritize(Path file, String sha1) {
		try {
			return Files.exists(file) && !isStale(file, sha1);
		} catch (IOException e) {
			try {
				Files.deleteIfExists(file);
			} catch (IOException de) {
				throw new IllegalStateException("unable to delete corrupt cache file", e);
			}

			return false;
		}
	}

	public static boolean isStale(Path file, String sha1) throws IOException {
		if (sha1 == null || sha1.isEmpty()) {
			return isStale(file, ONE_DAY);
		}

		byte[] bytes = Files.readAllBytes(file);
		byte[] hash = SHA1_DIGEST.digest(bytes);

		StringBuilder sb = new StringBuilder();

		for (byte b : hash) {
			sb.append(Integer.toString((b & 0xFF) + 0x100, 16).substring(1));
		}

		return sb.toString().equals(sha1);
	}

	public static boolean shouldPrioritize(Path file) {
		try {
			return Files.exists(file) && !isStale(file, ONE_DAY);
		} catch (IOException e) {
			try {
				Files.deleteIfExists(file);
			} catch (IOException de) {
				throw new IllegalStateException("unable to delete corrupt cache file", e);
			}

			return false;
		}
	}

	public static boolean isStale(Path file, Duration maxAge) throws IOException {
		Instant modifiedTime = Files.getLastModifiedTime(file).toInstant();
		Instant minModifiedTime = Instant.now().minus(maxAge);

		return modifiedTime.isBefore(minModifiedTime);
	}
}
