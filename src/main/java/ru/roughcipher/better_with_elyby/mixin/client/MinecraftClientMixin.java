package ru.roughcipher.better_with_elyby.mixin.client;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Version;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.roughcipher.better_with_elyby.config.BWEB;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Mixin(value = Minecraft.class, remap = false)
public class MinecraftClientMixin {
	@Unique
	private static final Logger LOGGER = LogUtils.getLogger();

	@Unique
	private static final String BTA_VERSION;

	@Unique
	private static final String USER_AGENT;

	static {
		String btaVersion = "unknown";
		try {
			java.lang.reflect.Field field = Version.class.getField("VERSION");
			btaVersion = (String) field.get(null);
		} catch (Exception ignored) {
		}
		BTA_VERSION = btaVersion;

		USER_AGENT = "BetterWithElyBy (BetterThanAdventure/" + BTA_VERSION + "; Java " +
			System.getProperty("java.version") + "; " +
			System.getProperty("os.name") + " " +
			System.getProperty("os.arch") + ")";
	}

	@Inject(method = "startGame", at = @At("HEAD"), remap = false)
	private void onStartGame(CallbackInfo ci) {
		if (!BWEB.AUTO_DISABLE) {
			return;
		}

		Minecraft mc = (Minecraft) (Object) this;
		UUID uuidObj = mc.session != null ? mc.session.uuid : null;
		if (uuidObj == null) {
			LOGGER.warn("No UUID in session. Disabling mod. User-Agent: {}", USER_AGENT);
			BWEB.disable();
			return;
		}
		String uuid = uuidObj.toString();

		try {
			String apiUrl = BWEB.AUTH_SERVER + "/api/user/profiles/" + uuid + "/names";
			URL url = new URL(apiUrl);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			connection.setConnectTimeout(5000);
			connection.setReadTimeout(5000);
			connection.setRequestProperty("User-Agent", USER_AGENT);

			int responseCode = connection.getResponseCode();
			boolean accountExists;

			if (responseCode == 200) {
				try (BufferedReader reader = new BufferedReader(
					new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
					StringBuilder response = new StringBuilder();
					String line;
					while ((line = reader.readLine()) != null) {
						response.append(line);
					}
					String body = response.toString();
					accountExists = body != null && !body.isEmpty() && !body.equals("[]");
				}
			} else if (responseCode == 204) {
				accountExists = false;
			} else {
				LOGGER.warn("Ely.by API code {} for UUID {}. User-Agent: {}. Disabling mod.", responseCode, uuid, USER_AGENT);
				BWEB.disable();
				connection.disconnect();
				return;
			}
			connection.disconnect();

			if (accountExists) {
				if (!BWEB.ENABLED) {
					BWEB.ENABLED = true;
					BWEB.save();
				}
				LOGGER.info("Ely.by account verified. Mod enabled. User-Agent: {}", USER_AGENT);
			} else {
				LOGGER.warn("Ely.by account not found for UUID {}. User-Agent: {}. Disabling mod.", uuid, USER_AGENT);
				BWEB.disable();
			}
		} catch (Exception e) {
			LOGGER.warn("Ely.by check failed: {}. User-Agent: {}. Disabling mod.", e.getMessage(), USER_AGENT);
			BWEB.disable();
		}
	}
}
