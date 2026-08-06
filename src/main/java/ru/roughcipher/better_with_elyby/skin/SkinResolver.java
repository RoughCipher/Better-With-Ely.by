package ru.roughcipher.better_with_elyby.skin;

import ru.roughcipher.better_with_elyby.auth.AuthSource;
import ru.roughcipher.better_with_elyby.config.BWEB;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class SkinResolver {
	private static final Logger LOGGER = LoggerFactory.getLogger("bweb-skin");
	private static final int TIMEOUT_MS = 5000;
	private static final long CACHE_TTL_MS = 10 * 60 * 1000L;

	private static final Map<String, CacheEntry> BY_UUID = new ConcurrentHashMap<>();
	private static final Map<String, CacheEntry> BY_NAME = new ConcurrentHashMap<>();

	private static final class CacheEntry {
		final PlayerTextures profile;
		final long at;

		CacheEntry(PlayerTextures profile) {
			this.profile = profile;
			this.at = System.currentTimeMillis();
		}

		boolean valid() {
			return System.currentTimeMillis() - at < CACHE_TTL_MS;
		}
	}

	public static PlayerTextures resolve(String username, AuthSource source, String knownUuid) {
		if (username == null || username.isEmpty()) return null;

		PlayerTextures cached = fromCache(username, knownUuid);
		if (cached != null) {
			return cached.isEmpty() ? null : cached;
		}

		try {
			PlayerTextures profile;
			if (source == AuthSource.MOJANG) {
				profile = fromMojang(username, knownUuid);
				if (profile == null || profile.isEmpty()) {
					profile = fromEly(username);
				}
			} else {
				profile = fromEly(username);
				if (profile == null || profile.isEmpty()) {
					profile = fromMojang(username, knownUuid);
				}
			}
			putCache(username, knownUuid, profile);
			return profile == null || profile.isEmpty() ? null : profile;
		} catch (Exception e) {
			LOGGER.warn("Failed to resolve skin for {} ({}): {}", username, source, e.getMessage());
			return null;
		}
	}

	private static PlayerTextures fromCache(String username, String knownUuid) {
		if (knownUuid != null) {
			CacheEntry e = BY_UUID.get(keyUuid(knownUuid));
			if (e != null && e.valid()) return e.profile;
		}
		CacheEntry e = BY_NAME.get(keyName(username));
		if (e != null && e.valid()) return e.profile;
		return null;
	}

	private static void putCache(String username, String knownUuid, PlayerTextures profile) {
		PlayerTextures stored = profile != null ? profile : PlayerTextures.empty(AuthSource.ELY);
		CacheEntry entry = new CacheEntry(stored);
		if (username != null) {
			BY_NAME.put(keyName(username), entry);
		}
		if (knownUuid != null) {
			BY_UUID.put(keyUuid(knownUuid), entry);
		}
	}

	private static PlayerTextures fromEly(String username) throws Exception {
		String url = "https://skinsystem.ely.by/textures/"
			+ URLEncoder.encode(username, StandardCharsets.UTF_8);
		String body = httpGet(url);
		if (body == null || body.isEmpty()) return null;

		JsonObject textures = JsonParser.parseString(body).getAsJsonObject();
		String skinUrl = null;
		String capeUrl = null;
		String model = "default";

		if (textures.has("SKIN")) {
			JsonObject skin = textures.getAsJsonObject("SKIN");
			skinUrl = skin.get("url").getAsString();
			if (skinUrl != null && skinUrl.startsWith("http://ely.by/")) {
				skinUrl = skinUrl.replace("http://", "https://");
			}
			if (skin.has("metadata")
				&& skin.getAsJsonObject("metadata").has("model")
				&& "slim".equals(skin.getAsJsonObject("metadata").get("model").getAsString())) {
				model = "slim";
			}
		}
		if (textures.has("CAPE")) {
			capeUrl = textures.getAsJsonObject("CAPE").get("url").getAsString();
			if (capeUrl != null && capeUrl.startsWith("http://ely.by/")) {
				capeUrl = capeUrl.replace("http://", "https://");
			}
		}
		PlayerTextures profile = PlayerTextures.of(skinUrl, capeUrl, model, AuthSource.ELY);
		return profile.isEmpty() ? null : profile;
	}

	private static PlayerTextures fromMojang(String username, String knownUuid) throws Exception {
		String uuid = compactUuid(knownUuid);
		if (uuid == null) {
			String uuidBody = httpGet("https://api.mojang.com/users/profiles/minecraft/"
				+ URLEncoder.encode(username, StandardCharsets.UTF_8));
			if (uuidBody == null || uuidBody.isEmpty()) return null;
			JsonObject uuidJson = JsonParser.parseString(uuidBody).getAsJsonObject();
			if (!uuidJson.has("id")) return null;
			uuid = uuidJson.get("id").getAsString().replace("-", "").toLowerCase(Locale.ROOT);
		}

		String profileBody = httpGet("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid);
		if (profileBody == null || profileBody.isEmpty()) return null;

		JsonObject profileJson = JsonParser.parseString(profileBody).getAsJsonObject();
		String skinUrl = null;
		String capeUrl = null;
		String model = "default";

		if (profileJson.has("properties")) {
			for (var el : profileJson.getAsJsonArray("properties")) {
				JsonObject prop = el.getAsJsonObject();
				if (!"textures".equals(prop.get("name").getAsString())) continue;
				String decoded = new String(
					Base64.getDecoder().decode(prop.get("value").getAsString()),
					StandardCharsets.UTF_8
				);
				JsonObject texturesRoot = JsonParser.parseString(decoded).getAsJsonObject();
				if (!texturesRoot.has("textures")) continue;
				JsonObject textures = texturesRoot.getAsJsonObject("textures");
				if (textures.has("SKIN")) {
					JsonObject skin = textures.getAsJsonObject("SKIN");
					skinUrl = skin.get("url").getAsString();
					if (skin.has("metadata")
						&& skin.getAsJsonObject("metadata").has("model")
						&& "slim".equals(skin.getAsJsonObject("metadata").get("model").getAsString())) {
						model = "slim";
					}
				}
				if (textures.has("CAPE")) {
					capeUrl = textures.getAsJsonObject("CAPE").get("url").getAsString();
				}
			}
		}
		PlayerTextures profile = PlayerTextures.of(skinUrl, capeUrl, model, AuthSource.MOJANG);
		return profile.isEmpty() ? null : profile;
	}

	private static String compactUuid(String raw) {
		if (raw == null || raw.isEmpty()) return null;
		String s = raw.replace("-", "").toLowerCase(Locale.ROOT);
		return s.length() == 32 ? s : null;
	}

	private static String keyUuid(String uuid) {
		return uuid.replace("-", "").toLowerCase(Locale.ROOT);
	}

	private static String keyName(String name) {
		return name.toLowerCase(Locale.ROOT);
	}

	private static String httpGet(String urlStr) throws Exception {
		HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
		conn.setRequestMethod("GET");
		conn.setConnectTimeout(TIMEOUT_MS);
		conn.setReadTimeout(TIMEOUT_MS);
		conn.setRequestProperty("User-Agent", BWEB.USER_AGENT);
		int code = conn.getResponseCode();
		if (code != 200) {
			conn.disconnect();
			return null;
		}
		try (BufferedReader reader = new BufferedReader(
			new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
			StringBuilder sb = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) sb.append(line);
			return sb.toString();
		} finally {
			conn.disconnect();
		}
	}
}
