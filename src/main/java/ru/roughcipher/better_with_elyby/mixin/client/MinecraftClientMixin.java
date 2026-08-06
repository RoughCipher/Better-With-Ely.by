package ru.roughcipher.better_with_elyby.mixin.client;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.roughcipher.better_with_elyby.auth.UuidResolver;
import ru.roughcipher.better_with_elyby.config.BWEB;

import java.util.UUID;

@Mixin(value = Minecraft.class, remap = false)
public class MinecraftClientMixin {
	@Unique
	private static final Logger LOGGER = LogUtils.getLogger();

	@Inject(method = "startGame", at = @At("HEAD"), remap = false)
	private void onStartGame(CallbackInfo ci) {
		BWEB.IS_ELY_ACCOUNT = false;
		BWEB.IS_OFFLINE_ACCOUNT = false;

		Minecraft mc = (Minecraft) (Object) this;
		if (mc.session == null) {
			BWEB.IS_OFFLINE_ACCOUNT = true;
			LOGGER.info("[BWEB] Offline (no session)");
			return;
		}

		UUID uuidObj = mc.session.uuid;
		String username = mc.session.username;

		if (uuidObj == null) {
			BWEB.IS_OFFLINE_ACCOUNT = true;
			LOGGER.info("[BWEB] Offline (no UUID)");
			return;
		}

		if (uuidObj.version() == 3) {
			BWEB.IS_OFFLINE_ACCOUNT = true;
			LOGGER.info("[BWEB] Offline");
			return;
		}

		if (username == null || username.isEmpty()) {
			LOGGER.info("[BWEB] Mojang (no username)");
			return;
		}

		try {
			String elyUuid = UuidResolver.fromEly(username);
			boolean isEly = elyUuid != null
				&& elyUuid.replace("-", "").equalsIgnoreCase(uuidObj.toString().replace("-", ""));
			BWEB.IS_ELY_ACCOUNT = isEly;
			if (isEly) {
				LOGGER.info("[BWEB] Ely.by ({})", username);
			} else {
				LOGGER.info("[BWEB] Mojang ({})", username);
			}
		} catch (Exception e) {
			LOGGER.warn("[BWEB] Mojang (Ely.by check failed: {})", e.getMessage());
		}
	}
}
