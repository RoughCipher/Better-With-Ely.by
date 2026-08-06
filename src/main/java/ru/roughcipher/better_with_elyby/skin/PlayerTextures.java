package ru.roughcipher.better_with_elyby.skin;

import ru.roughcipher.better_with_elyby.auth.AuthSource;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;

public final class PlayerTextures {
	private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

	private final String skin;
	private final String cape;
	private final boolean slim;
	private final AuthSource backend;

	private PlayerTextures(String skin, String cape, boolean slim, AuthSource backend) {
		this.skin = blankToNull(skin);
		this.cape = blankToNull(cape);
		this.slim = slim;
		this.backend = backend == null ? AuthSource.ELY : backend;
	}

	public static PlayerTextures of(String skinUrl, String capeUrl, String model, AuthSource source) {
		return new PlayerTextures(skinUrl, capeUrl, "slim".equals(model), source);
	}

	public static PlayerTextures empty(AuthSource source) {
		return new PlayerTextures(null, null, false, source);
	}

	public String getSkinUrl() { return skin; }
	public String getCapeUrl() { return cape; }
	public boolean isSlim() { return slim; }
	public AuthSource getSource() { return backend; }
	public boolean isEmpty() { return skin == null && cape == null; }

	public String toSessionProfileJson(String username) {
		return sessionJson(username, texturesObject());
	}

	public static String noTexturesSessionJson(String username) {
		return sessionJson(username, new JsonObject());
	}

	private JsonObject texturesObject() {
		JsonObject textures = new JsonObject();
		if (skin != null) {
			JsonObject skinObj = new JsonObject();
			skinObj.addProperty("url", skin);
			if (slim) {
				JsonObject meta = new JsonObject();
				meta.addProperty("model", "slim");
				skinObj.add("metadata", meta);
			}
			textures.add("SKIN", skinObj);
		}
		if (cape != null) {
			JsonObject capeObj = new JsonObject();
			capeObj.addProperty("url", cape);
			textures.add("CAPE", capeObj);
		}
		return textures;
	}

	private static String sessionJson(String username, JsonObject textures) {
		JsonObject wrapper = new JsonObject();
		wrapper.add("textures", textures);
		String b64 = Base64.getEncoder().encodeToString(GSON.toJson(wrapper).getBytes(StandardCharsets.UTF_8));
		JsonObject prop = new JsonObject();
		prop.addProperty("name", "textures");
		prop.addProperty("value", b64);
		JsonObject session = new JsonObject();
		session.addProperty("id", UUID.nameUUIDFromBytes(username.getBytes(StandardCharsets.UTF_8)).toString().replace("-", ""));
		session.addProperty("name", username);
		JsonArray properties = new JsonArray();
		properties.add(prop);
		session.add("properties", properties);
		return GSON.toJson(session);
	}

	private static String blankToNull(String value) {
		return value == null || value.trim().isEmpty() ? null : value.trim();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof PlayerTextures that)) return false;
		return slim == that.slim && Objects.equals(skin, that.skin)
			&& Objects.equals(cape, that.cape) && backend == that.backend;
	}

	@Override
	public int hashCode() {
		return Objects.hash(skin, cape, slim, backend);
	}
}
