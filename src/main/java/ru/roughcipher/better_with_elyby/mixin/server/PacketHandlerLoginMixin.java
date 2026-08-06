package ru.roughcipher.better_with_elyby.mixin.server;

import com.mojang.logging.LogUtils;
import net.minecraft.core.net.packet.PacketLogin;
import net.minecraft.server.net.handler.PacketHandlerLogin;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import ru.roughcipher.better_with_elyby.config.BWEB;
import ru.roughcipher.better_with_elyby.auth.AuthSource;
import ru.roughcipher.better_with_elyby.auth.PlayerAuthTracker;
import ru.roughcipher.better_with_elyby.auth.UuidResolver;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Mixin(value = PacketHandlerLogin.class, remap = false)
public abstract class PacketHandlerLoginMixin {
	@Unique
	private static final Logger LOGGER = LogUtils.getLogger();

	@Redirect(
		method = "handleLogin",
		at = @At(
			value = "INVOKE",
			target = "Ljava/lang/Thread;start()V"
		)
	)
	private void redirectSessionCheck(Thread originalThread, PacketLogin packetLogin) {
		PacketHandlerLogin self = (PacketHandlerLogin) (Object) this;

		new Thread(() -> {
			try {
				String serverId = PacketHandlerLogin.getServerId(self);
				String encodedUser = URLEncoder.encode(packetLogin.username, StandardCharsets.UTF_8);
				String encodedServerId = URLEncoder.encode(serverId, StandardCharsets.UTF_8);
				String query = encodedUser + "&serverId=" + encodedServerId;

				AuthSource source = null;
				if (checkHasJoined(BWEB.SESSION_HAS_JOINED_URL + query)) {
					source = AuthSource.ELY;
					LOGGER.info("Player '{}' verified via Ely.by", packetLogin.username);
				} else if (checkHasJoined(BWEB.OLD_HAS_JOINED + query)) {
					source = AuthSource.MOJANG;
					LOGGER.info("Player '{}' verified via Mojang", packetLogin.username);
				}

				if (source == null) {
					self.kickUser("Failed to verify username!");
					return;
				}

				String uuid = UuidResolver.resolve(packetLogin.username, source);
				if (uuid == null) {
					LOGGER.warn("Could not resolve UUID for '{}' via {} — login may use offline UUID", packetLogin.username, source);
				} else {
					LOGGER.info("Bound UUID for '{}' via {}: {}", packetLogin.username, source, uuid);
				}
				PlayerAuthTracker.put(packetLogin.username, uuid, source);

				PacketHandlerLogin.setLoginPacket(self, packetLogin);
			} catch (Exception exception) {
				LOGGER.error("Exception while trying to verify user '{}', kicking!",
					self.getUserAndIPString(), exception);
				self.kickUser("Failed to verify username! [internal error " + exception + "]");
			}
		}).start();
	}

	@Unique
	private static boolean checkHasJoined(String fullUrl) {
		try {
			URL url = new URL(fullUrl);
			try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(url.openStream(), StandardCharsets.UTF_8))) {
				String response = reader.readLine();
				return "YES".equals(response);
			}
		} catch (Exception e) {
			LOGGER.debug("hasJoined check failed for {}: {}", fullUrl, e.getMessage());
			return false;
		}
	}
}
