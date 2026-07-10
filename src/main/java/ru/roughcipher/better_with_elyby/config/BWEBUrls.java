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

public final class BWEBUrls {
	private static final Logger LOGGER = LoggerFactory.getLogger(BWEBUrls.class);
	private static final Gson GSON = new GsonBuilder()
		.setPrettyPrinting()
		.disableHtmlEscaping()
		.create();

	public static final String OLD_UUID_LOOKUP = "https://api.minecraftservices.com/minecraft/profile/lookup/name/%s";
	public static final String OLD_SESSION = "http://session.minecraft.net/game/joinserver.jsp?user=";

	public static String UUID_LOOKUP_URL = "https://authserver.ely.by/api/users/profiles/minecraft/%s";
	public static String SESSION_JOIN_URL = "https://authserver.ely.by/session/legacy/join?user=";
	public static String SESSION_HAS_JOINED_URL = "https://authserver.ely.by/session/legacy/hasJoined?user=";
	public static String SKIN_PROFILE_URL = "https://skinsystem.ely.by/profile/";

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

	private static String get(JsonObject obj, String key, String defaultValue) {
		JsonElement el = obj.get(key);
		if (el == null) {
			obj.addProperty(key, defaultValue);
			return defaultValue;
		}
		return el.getAsString();
	}

	private static void updateValues(JsonObject obj) {
		UUID_LOOKUP_URL = get(obj, "uuidLookupUrl", UUID_LOOKUP_URL);
		SESSION_JOIN_URL = get(obj, "sessionJoinUrl", SESSION_JOIN_URL);
		SESSION_HAS_JOINED_URL = get(obj, "sessionHasJoinedUrl", SESSION_HAS_JOINED_URL);
		SKIN_PROFILE_URL = get(obj, "skinProfileUrl", SKIN_PROFILE_URL);
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

	private BWEBUrls() {}
}
