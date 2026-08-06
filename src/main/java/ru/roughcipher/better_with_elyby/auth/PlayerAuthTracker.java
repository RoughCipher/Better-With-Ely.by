package ru.roughcipher.better_with_elyby.auth;

import ru.roughcipher.better_with_elyby.skin.PlayerTextures;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerAuthTracker {
	private static final Map<String, AuthSource> SOURCE_BY_UUID = new ConcurrentHashMap<>();
	private static final Map<String, PlayerTextures> PROFILE_BY_UUID = new ConcurrentHashMap<>();
	private static final Map<String, String> NAME_BY_UUID = new ConcurrentHashMap<>();
	private static final Map<String, String> UUID_BY_NAME = new ConcurrentHashMap<>();
	private static final Map<Integer, String> UUID_BY_ENTITY = new ConcurrentHashMap<>();

	private PlayerAuthTracker() {}

	public static void put(String username, String uuid, AuthSource source) {
		if (uuid == null) return;
		String u = keyUuid(uuid);
		SOURCE_BY_UUID.put(u, source);
		if (username != null) {
			NAME_BY_UUID.put(u, username);
			UUID_BY_NAME.put(keyName(username), u);
		}
	}

	public static void bindEntity(int entityId, String uuid) {
		if (uuid == null) return;
		UUID_BY_ENTITY.put(entityId, keyUuid(uuid));
	}

	public static void setUuid(String username, String uuid) {
		if (username == null || uuid == null) return;
		String u = keyUuid(uuid);
		NAME_BY_UUID.put(u, username);
		UUID_BY_NAME.put(keyName(username), u);
	}

	public static AuthSource getSourceByUuid(String uuid) {
		if (uuid == null) return null;
		return SOURCE_BY_UUID.get(keyUuid(uuid));
	}

	public static AuthSource getSource(String username) {
		String uuid = getUuidByName(username);
		return uuid == null ? null : SOURCE_BY_UUID.get(keyUuid(uuid));
	}

	public static String getUuidByEntity(int entityId) {
		return UUID_BY_ENTITY.get(entityId);
	}

	public static String getUuidByName(String username) {
		if (username == null) return null;
		return UUID_BY_NAME.get(keyName(username));
	}

	public static void setProfile(String username, PlayerTextures profile) {
		String uuid = getUuidByName(username);
		if (uuid != null) setProfileByUuid(uuid, profile);
	}

	public static void setProfileByUuid(String uuid, PlayerTextures profile) {
		if (uuid == null) return;
		String u = keyUuid(uuid);
		if (profile == null || profile.isEmpty()) {
			PROFILE_BY_UUID.remove(u);
		} else {
			PROFILE_BY_UUID.put(u, profile);
		}
	}

	public static PlayerTextures getProfileByUuid(String uuid) {
		if (uuid == null) return null;
		return PROFILE_BY_UUID.get(keyUuid(uuid));
	}

	public static void removeEntity(int entityId) {
		String uuid = UUID_BY_ENTITY.remove(entityId);
		if (uuid != null) {
			removeByUuid(uuid);
		}
	}

	public static void removeByUuid(String uuid) {
		if (uuid == null) return;
		String u = keyUuid(uuid);
		SOURCE_BY_UUID.remove(u);
		PROFILE_BY_UUID.remove(u);
		String name = NAME_BY_UUID.remove(u);
		if (name != null) {
			String mapped = UUID_BY_NAME.get(keyName(name));
			if (u.equals(mapped)) {
				UUID_BY_NAME.remove(keyName(name));
			}
		}
		UUID_BY_ENTITY.entrySet().removeIf(e -> u.equals(e.getValue()));
	}

	private static String keyUuid(String uuid) {
		return uuid.replace("-", "").toLowerCase(Locale.ROOT);
	}

	private static String keyName(String name) {
		return name.toLowerCase(Locale.ROOT);
	}
}
