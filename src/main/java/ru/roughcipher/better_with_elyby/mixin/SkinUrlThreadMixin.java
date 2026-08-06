package ru.roughcipher.better_with_elyby.mixin;

import com.b100.utils.StringUtils;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.util.helper.GetMonsterSkinUrlThread;
import net.minecraft.core.util.helper.GetSkinUrlThread;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.roughcipher.better_with_elyby.config.BWEB;
import ru.roughcipher.better_with_elyby.skin.PlayerTextures;
import ru.roughcipher.better_with_elyby.skin.TextureIndex;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(value = {GetSkinUrlThread.class, GetMonsterSkinUrlThread.class}, remap = false)
public class SkinUrlThreadMixin {

	@Unique
	private static final Logger LOGGER = LogUtils.getLogger();

	@Unique
	private static final Map<String, Integer> MP_WAIT_COUNT = new ConcurrentHashMap<>();

	@Unique
	private static final int MP_WAIT_MAX = 2;

	@Unique
	private static final String ELY_TEXTURES = "https://skinsystem.ely.by/textures/";

	@Inject(method = "getSkinObject", at = @At("HEAD"), cancellable = true, remap = false)
	private void onGetSkinObject(String name, CallbackInfoReturnable<String> cir) {
		if (name == null || name.isEmpty()) {
			return;
		}

		PlayerTextures serverProfile = TextureIndex.get(name);
		if (serverProfile != null && !serverProfile.isEmpty()) {
			MP_WAIT_COUNT.remove(key(name));
			cir.setReturnValue(serverProfile.toSessionProfileJson(name));
			cir.cancel();
			return;
		}

		if (TextureIndex.isMissing(name)) {
			cir.setReturnValue(PlayerTextures.noTexturesSessionJson(name));
			cir.cancel();
			return;
		}

		if (isMultiplayer()) {
			int waits = MP_WAIT_COUNT.merge(key(name), 1, Integer::sum);
			if (waits <= MP_WAIT_MAX) {
				cir.setReturnValue(null);
				cir.cancel();
				return;
			}
			MP_WAIT_COUNT.remove(key(name));
		}

		if (isLocalPlayer(name) && !BWEB.IS_ELY_ACCOUNT && !BWEB.IS_OFFLINE_ACCOUNT) {
			return;
		}

		try {
			String session = fetchElySessionJson(name);
			if (session != null) {
				cir.setReturnValue(session);
				cir.cancel();
				return;
			}

			TextureIndex.markMissing(name);
			LOGGER.info("No skin on Ely.by for '{}'; using default.", name);
			cir.setReturnValue(PlayerTextures.noTexturesSessionJson(name));
			cir.cancel();
		} catch (Exception e) {
			LOGGER.warn("Can't connect to Ely.by API for {}. Using default skin.", name);
			TextureIndex.markMissing(name);
			cir.setReturnValue(PlayerTextures.noTexturesSessionJson(name));
			cir.cancel();
		}
	}

	@Unique
	private static String fetchElySessionJson(String name) {
		String encoded = URLEncoder.encode(name, StandardCharsets.UTF_8);

		String texturesBody = StringUtils.getWebsiteContentAsString(ELY_TEXTURES + encoded);
		PlayerTextures fromTextures = parseTexturesBody(texturesBody);
		if (fromTextures != null && !fromTextures.isEmpty()) {
			return fromTextures.toSessionProfileJson(name);
		}

		String profileBody = StringUtils.getWebsiteContentAsString(BWEB.SKIN_PROFILE_URL + encoded);
		if (isSessionProfile(profileBody)) {
			return profileBody;
		}

		return null;
	}

	@Unique
	private static PlayerTextures parseTexturesBody(String body) {
		if (body == null || body.isBlank() || "{}".equals(body.trim())) {
			return null;
		}
		try {
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
			return PlayerTextures.of(skinUrl, capeUrl, model, ru.roughcipher.better_with_elyby.auth.AuthSource.ELY);
		} catch (Exception e) {
			return null;
		}
	}

	@Unique
	private static boolean isSessionProfile(String response) {
		return response != null
			&& !response.isBlank()
			&& response.contains("\"properties\"")
			&& response.contains("\"textures\"");
	}

	@Unique
	private static String key(String name) {
		return name.toLowerCase(Locale.ROOT);
	}

	@Unique
	private static boolean isLocalPlayer(String name) {
		try {
			Minecraft mc = Minecraft.getMinecraft();
			return mc.thePlayer != null
				&& mc.thePlayer.username != null
				&& mc.thePlayer.username.equalsIgnoreCase(name);
		} catch (Exception e) {
			return false;
		}
	}

	@Unique
	private static boolean isMultiplayer() {
		try {
			return Minecraft.getMinecraft().getSendQueue() != null;
		} catch (Exception e) {
			return false;
		}
	}
}
