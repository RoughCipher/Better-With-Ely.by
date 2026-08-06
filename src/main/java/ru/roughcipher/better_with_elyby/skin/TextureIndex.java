package ru.roughcipher.better_with_elyby.skin;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class TextureIndex {
	private static final long NO_SKIN_TTL_MS = 5 * 60 * 1000L;
	private static final Map<String, PlayerTextures> BY_UUID = new ConcurrentHashMap<>();
	private static final Map<String, String> NAME_TO_UUID = new ConcurrentHashMap<>();
	private static final Map<String, PlayerTextures> BY_NAME = new ConcurrentHashMap<>();
	private static final Map<String, Long> NO_SKIN_AT = new ConcurrentHashMap<>();

	public static void put(String username, String uuid, PlayerTextures textures) {
		if (textures == null || textures.isEmpty()) {
			remove(username, uuid);
			return;
		}
		if (username != null) NO_SKIN_AT.remove(nameKey(username));
		if (uuid != null) {
			String u = uuidKey(uuid);
			BY_UUID.put(u, textures);
			if (username != null) {
				NAME_TO_UUID.put(nameKey(username), u);
				BY_NAME.put(nameKey(username), textures);
			}
		} else if (username != null) {
			BY_NAME.put(nameKey(username), textures);
		}
	}

	public static void remove(String username, String uuid) {
		if (uuid != null) BY_UUID.remove(uuidKey(uuid));
		if (username != null) {
			String k = nameKey(username);
			BY_NAME.remove(k);
			NAME_TO_UUID.remove(k);
			NO_SKIN_AT.remove(k);
		}
	}

	public static void markMissing(String username) {
		if (username != null) NO_SKIN_AT.put(nameKey(username), System.currentTimeMillis());
	}

	public static boolean isMissing(String username) {
		if (username == null) return false;
		String k = nameKey(username);
		Long at = NO_SKIN_AT.get(k);
		if (at == null) return false;
		if (System.currentTimeMillis() - at > NO_SKIN_TTL_MS) {
			NO_SKIN_AT.remove(k);
			return false;
		}
		return true;
	}

	public static PlayerTextures get(String username) {
		if (username == null) return null;
		String k = nameKey(username);
		String u = NAME_TO_UUID.get(k);
		if (u != null) {
			PlayerTextures p = BY_UUID.get(u);
			if (p != null) return p;
		}
		return BY_NAME.get(k);
	}

	private static String uuidKey(String uuid) {
		return uuid.replace("-", "").toLowerCase(Locale.ROOT);
	}

	private static String nameKey(String name) {
		return name.toLowerCase(Locale.ROOT);
	}
}
