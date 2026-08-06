package ru.roughcipher.better_with_elyby.auth;

import net.minecraft.core.util.helper.UUIDHelper;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.roughcipher.better_with_elyby.config.BWEB;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class UuidResolver {
	private static final Logger LOGGER = LoggerFactory.getLogger("bweb-uuid");
	private static final int TIMEOUT_MS = 5000;

	private static final ThreadLocal<AuthSource> FORCED_SOURCE = new ThreadLocal<>();

	public static void setForcedSource(AuthSource source) {
		if (source == null) FORCED_SOURCE.remove();
		else FORCED_SOURCE.set(source);
	}

	public static void clearForcedSource() {
		FORCED_SOURCE.remove();
	}


	private UuidResolver() {}

	public static String resolve(String username, AuthSource source) {
		if (username == null || username.isEmpty()) return null;
		try {
			if (BWEB.ELY_ONLY || source == AuthSource.ELY || source == AuthSource.OFFLINE) {
				String ely = fromEly(username);
				if (ely != null || BWEB.ELY_ONLY) return ely;
				return fromMojang(username);
			}
			if (source == AuthSource.MOJANG) {
				String mojang = fromMojang(username);
				if (mojang != null) return mojang;
				return fromEly(username);
			}
			String ely = fromEly(username);
			if (ely != null) return ely;
			return fromMojang(username);
		} catch (Exception e) {
			LOGGER.warn("UUID resolve failed for {} ({}): {}", username, source, e.getMessage());
			return null;
		}
	}

	public static String resolveForLookup(String username) {
		AuthSource forced = FORCED_SOURCE.get();
		if (forced != null) {
			try {
				String uuid = resolve(username, forced);
				return uuid == null ? null : toLookupJson(username, uuid);
			} catch (Exception e) {
				LOGGER.warn("Forced UUID resolve failed for {} ({}): {}", username, forced, e.getMessage());
				return null;
			}
		}

		AuthSource online = PlayerAuthTracker.getSource(username);
		if (online != null) {
			String uuid = resolve(username, online);
			if (uuid != null) {
				return toLookupJson(username, uuid);
			}
		}

		String elyUuid = null;
		String mojangUuid = null;
		try { elyUuid = fromEly(username); } catch (Exception ignored) {}
		try { mojangUuid = fromMojang(username); } catch (Exception ignored) {}

		if (elyUuid != null && mojangUuid != null && !elyUuid.equalsIgnoreCase(mojangUuid)) {
			LOGGER.warn(
				"Dual registration for '{}': Ely.by UUID={} Mojang UUID={}. Using preference '{}'.",
				username, elyUuid, mojangUuid, BWEB.UUID_PREFERENCE
			);
		}

		String chosen;
		if ("mojang".equals(BWEB.UUID_PREFERENCE)) {
			chosen = mojangUuid != null ? mojangUuid : elyUuid;
		} else {
			chosen = elyUuid != null ? elyUuid : mojangUuid;
		}
		return chosen == null ? null : toLookupJson(username, chosen);
	}

	public static String fromEly(String username) throws Exception {
		String url = String.format(BWEB.UUID_LOOKUP_URL, URLEncoder.encode(username, StandardCharsets.UTF_8));
		String body = httpGet(url);
		if (body == null || body.isEmpty() || "{}".equals(body)) return null;
		JsonObject json = JsonParser.parseString(body).getAsJsonObject();
		if (!json.has("id")) return null;
		return normalizeUuid(json.get("id").getAsString());
	}

	public static String fromMojang(String username) throws Exception {
		String encoded = URLEncoder.encode(username, StandardCharsets.UTF_8);
		String template = UUIDHelper.urlUUID;
		if (template == null || template.isEmpty()) {
			template = "https://api.minecraftservices.com/minecraft/profile/lookup/name/%s";
		}
		String endpoint = template.contains("%s")
			? String.format(template, encoded)
			: (template.endsWith("/") ? template + encoded : template + "/" + encoded);
		String body = httpGet(endpoint);
		if (body == null || body.isEmpty()) return null;
		JsonObject json = JsonParser.parseString(body).getAsJsonObject();
		if (!json.has("id")) return null;
		return normalizeUuid(json.get("id").getAsString());
	}

	public static String normalizeUuid(String raw) {
		if (raw == null) return null;
		String s = raw.trim().replace("-", "").toLowerCase();
		if (s.length() != 32) {
			try {
				return UUID.fromString(raw.trim()).toString();
			} catch (Exception e) {
				return raw.trim();
			}
		}
		return s.substring(0, 8) + "-" + s.substring(8, 12) + "-" + s.substring(12, 16)
			+ "-" + s.substring(16, 20) + "-" + s.substring(20, 32);
	}

	public static String compactUuid(String hyphenated) {
		if (hyphenated == null) return null;
		return hyphenated.replace("-", "").toLowerCase();
	}

	private static String toLookupJson(String username, String hyphenatedUuid) {
		JsonObject obj = new JsonObject();
		obj.addProperty("id", compactUuid(hyphenatedUuid));
		obj.addProperty("name", username);
		return obj.toString();
	}

	private static String httpGet(String urlStr) throws Exception {
		HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
		conn.setRequestMethod("GET");
		conn.setConnectTimeout(TIMEOUT_MS);
		conn.setReadTimeout(TIMEOUT_MS);
		conn.setRequestProperty("User-Agent", BWEB.USER_AGENT);
		conn.setInstanceFollowRedirects(true);
		int code = conn.getResponseCode();
		if (code != 200) {
			conn.disconnect();
			return null;
		}
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
			StringBuilder sb = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) sb.append(line);
			return sb.toString();
		} finally {
			conn.disconnect();
		}
	}

	public static String formatUuid(String raw) {
		if (raw == null) return null;
		String s = raw.trim().replace("-", "").toLowerCase();
		if (s.length() != 32) return raw.trim();
		return s.substring(0, 8) + "-" + s.substring(8, 12) + "-" + s.substring(12, 16)
			+ "-" + s.substring(16, 20) + "-" + s.substring(20, 32);
	}
}
