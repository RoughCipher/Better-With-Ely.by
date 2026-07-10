package ru.roughcipher.better_with_elyby.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class BWEB {
	private static final Logger LOGGER = LoggerFactory.getLogger(BWEB.class);
	private static final Gson GSON = new GsonBuilder()
		.setPrettyPrinting()
		.disableHtmlEscaping()
		.create();

	public static final String AUTH_SERVER = "https://authserver.ely.by";
	public static final String OLD_SESSION = "http://session.minecraft.net/game/joinserver.jsp?user=";

	public static String UUID_LOOKUP_URL = AUTH_SERVER + "/api/users/profiles/minecraft/%s";
	public static String SESSION_JOIN_URL = AUTH_SERVER + "/session/legacy/join?user=";
	public static String SESSION_HAS_JOINED_URL = AUTH_SERVER + "/session/legacy/hasJoined?user=";
	public static String SKIN_PROFILE_URL = "https://skinsystem.ely.by/profile/";

	public static boolean ENABLED = true;
	public static boolean AUTO_DISABLE = true;

	public static void load() {
		File file = getConfigFile();
		if (!file.exists()) initFile(file);
		try (FileReader reader = new FileReader(file)) {
			JsonObject obj = GSON.fromJson(reader, JsonObject.class);
			updateValues(obj);
		} catch (IOException e) {
			LOGGER.error("Failed to load config, using defaults", e);
		}
		save();
	}

	public static void save() {
		File file = getConfigFile();
		JsonObject obj = new JsonObject();
		obj.addProperty("enabled", ENABLED);
		obj.addProperty("autoDisable", AUTO_DISABLE);
		obj.addProperty("uuidLookupUrl", UUID_LOOKUP_URL);
		obj.addProperty("sessionJoinUrl", SESSION_JOIN_URL);
		obj.addProperty("sessionHasJoinedUrl", SESSION_HAS_JOINED_URL);
		obj.addProperty("skinProfileUrl", SKIN_PROFILE_URL);
		try (FileWriter writer = new FileWriter(file)) {
			writer.write(GSON.toJson(obj));
		} catch (IOException e) {
			LOGGER.error("Failed to save config", e);
		}
	}

	@SuppressWarnings("unchecked")
	private static <T> T get(JsonObject obj, String key, T defaultValue) {
		JsonElement el = obj.get(key);
		if (el == null) {
			obj.add(key, GSON.toJsonTree(defaultValue));
			return defaultValue;
		}
		return GSON.fromJson(el, (Class<T>) defaultValue.getClass());
	}

	private static void updateValues(JsonObject obj) {
		ENABLED = get(obj, "enabled", ENABLED);
		AUTO_DISABLE = get(obj, "autoDisable", AUTO_DISABLE);
		UUID_LOOKUP_URL = get(obj, "uuidLookupUrl", UUID_LOOKUP_URL);
		SESSION_JOIN_URL = get(obj, "sessionJoinUrl", SESSION_JOIN_URL);
		SESSION_HAS_JOINED_URL = get(obj, "sessionHasJoinedUrl", SESSION_HAS_JOINED_URL);
		SKIN_PROFILE_URL = get(obj, "skinProfileUrl", SKIN_PROFILE_URL);
	}

	public static void disable() {
		if (ENABLED && AUTO_DISABLE) {
			ENABLED = false;
			LOGGER.warn("Better With Ely.by mod has been automatically disabled.");
			save();
		}
	}

	private static void initFile(File file) {
		try {
			Path parent = file.getParentFile().toPath();
			if (!Files.exists(parent)) Files.createDirectories(parent);
			if (file.createNewFile()) {
				try (FileWriter writer = new FileWriter(file)) {
					writer.write("{}");
				}
			}
		} catch (IOException e) {
			LOGGER.error("Failed to initialize config file", e);
		}
	}

	private static File getConfigFile() {
		Path configDir = FabricLoader.getInstance().getConfigDir().resolve("bweb");
		try {
			Files.createDirectories(configDir);
		} catch (IOException e) {
			LOGGER.error("Failed to create config directory", e);
		}
		return configDir.resolve("bweb.json").toFile();
	}

	static {
		load();
	}

	private BWEB() {}
}
