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
	public static final String USER_AGENT = buildUserAgent();

	private static String buildUserAgent() {
		String bta = "unknown";
		try {
			Class<?> versionClass = Class.forName("net.minecraft.core.Version");
			Object val = versionClass.getField("VERSION").get(null);
			if (val != null) bta = val.toString();
		} catch (Exception ignored) {
		}
		return "BWEB (BTA " + bta + "; Java " +
			System.getProperty("java.version") + "; " +
			System.getProperty("os.name") + " " +
			System.getProperty("os.arch") + ")";
	}

	public static final String OLD_SESSION = "http://session.minecraft.net/game/joinserver.jsp?user=";
	public static final String OLD_HAS_JOINED = "http://session.minecraft.net/game/checkserver.jsp?user=";

	public static String UUID_LOOKUP_URL = AUTH_SERVER + "/api/users/profiles/minecraft/%s";
	public static String SESSION_JOIN_URL = AUTH_SERVER + "/session/legacy/join?user=";
	public static String SESSION_HAS_JOINED_URL = AUTH_SERVER + "/session/legacy/hasJoined?user=";
	public static String SKIN_PROFILE_URL = "https://skinsystem.ely.by/profile/";

	public static boolean IS_ELY_ACCOUNT = false;
	public static boolean IS_OFFLINE_ACCOUNT = false;
	public static String UUID_PREFERENCE = "ely";
	public static boolean ELY_ONLY = false;
	public static boolean WARN_MISSING_MOD = true;
	public static final int WARN_MISSING_MOD_DELAY_SEC = 3;
	public static final String WARN_MISSING_MOD_MESSAGE =
		"[BWEB] Install Better With Ely.by to see skins of Ely.by players.";

	public static void load() {
		File file = getConfigFile();
		if (!file.exists()) initFile(file);
		IS_ELY_ACCOUNT = false;
		IS_OFFLINE_ACCOUNT = false;
		try (FileReader reader = new FileReader(file)) {
			JsonObject obj = GSON.fromJson(reader, JsonObject.class);
			if (obj != null) updateValues(obj);
		} catch (IOException e) {
			LOGGER.error("Failed to load config, using defaults", e);
		}
		save();
	}

	public static void save() {
		File file = getConfigFile();
		JsonObject obj = new JsonObject();
		obj.addProperty("uuidPreference", UUID_PREFERENCE);
		obj.addProperty("elyOnly", ELY_ONLY);
		obj.addProperty("warnMissingMod", WARN_MISSING_MOD);
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
		String pref = get(obj, "uuidPreference", UUID_PREFERENCE);
		UUID_PREFERENCE = "mojang".equals(pref) ? "mojang" : "ely";
		ELY_ONLY = get(obj, "elyOnly", ELY_ONLY);
		WARN_MISSING_MOD = get(obj, "warnMissingMod", WARN_MISSING_MOD);
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
}
