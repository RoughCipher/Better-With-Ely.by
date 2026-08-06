package ru.roughcipher.better_with_elyby.mixin.server;

import com.mojang.logging.LogUtils;
import net.minecraft.core.net.packet.PacketMessage;
import net.minecraft.server.entity.player.PlayerServer;
import net.minecraft.server.net.PlayerList;
import net.minecraft.server.net.handler.PacketHandlerLogin;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.roughcipher.better_with_elyby.network.ClientPresence;
import ru.roughcipher.better_with_elyby.network.TextureSync;
import ru.roughcipher.better_with_elyby.auth.AuthSource;
import ru.roughcipher.better_with_elyby.auth.PlayerAuthTracker;
import ru.roughcipher.better_with_elyby.config.BWEB;
import ru.roughcipher.better_with_elyby.auth.UuidResolver;
import ru.roughcipher.better_with_elyby.skin.PlayerTextures;
import ru.roughcipher.better_with_elyby.skin.SkinResolver;

import java.util.UUID;

@Mixin(value = PlayerList.class, remap = false)
public abstract class PlayerListMixin {
	@Unique
	private static final Logger LOGGER = LogUtils.getLogger();

	@Unique
	private static final String MSG_ELY_JOINING =
		"Nick in use (Mojang). Mojang may reclaim it via Ely.by rules.";

	@Unique
	private static final String MSG_MOJANG_JOINING =
		"Nick in use (Ely.by). You may reclaim it via Ely.by rules.";

	@Inject(method = "getPlayerForLogin", at = @At("HEAD"), cancellable = true, remap = false)
	private void bweb$rejectDualNickname(
		PacketHandlerLogin handler,
		String username,
		UUID uuid,
		CallbackInfoReturnable<PlayerServer> cir
	) {
		if (username == null) return;

		PlayerList self = (PlayerList) (Object) this;
		String joiningUuid = uuid != null ? uuid.toString() : PlayerAuthTracker.getUuidByName(username);
		AuthSource joiningSource = joiningUuid != null
			? PlayerAuthTracker.getSourceByUuid(joiningUuid)
			: PlayerAuthTracker.getSource(username);

		for (PlayerServer online : self.playerEntities) {
			if (online.username == null || !online.username.equalsIgnoreCase(username)) continue;

			String onlineUuid = online.uuid != null ? online.uuid.toString() : PlayerAuthTracker.getUuidByEntity(online.id);
			boolean sameUuid = joiningUuid != null && onlineUuid != null
				&& joiningUuid.replace("-", "").equalsIgnoreCase(onlineUuid.replace("-", ""));

			if (sameUuid) {
				return;
			}

			AuthSource onlineSource = onlineUuid != null
				? PlayerAuthTracker.getSourceByUuid(onlineUuid)
				: PlayerAuthTracker.getSource(online.username);

			String message = joiningSource == AuthSource.MOJANG
				? MSG_MOJANG_JOINING
				: MSG_ELY_JOINING;

			LOGGER.warn(
				"Rejected dual-nickname join for '{}': online={} ({}) joining={} ({})",
				username, onlineSource, onlineUuid, joiningSource, joiningUuid
			);

			handler.kickUser(message);
			cir.setReturnValue(null);
			cir.cancel();
			return;
		}
	}

	@Inject(method = "playerLoggedIn", at = @At("TAIL"), remap = false)
	private void bweb$onPlayerLoggedIn(PlayerServer player, CallbackInfo ci) {
		if (player == null) return;

		PlayerList self = (PlayerList) (Object) this;

		UUID playerUuid = player.uuid;
		String uuidStr = playerUuid != null ? playerUuid.toString() : PlayerAuthTracker.getUuidByName(player.username);
		if (uuidStr != null) {
			PlayerAuthTracker.bindEntity(player.id, uuidStr);
			if (player.username != null) {
				AuthSource src = PlayerAuthTracker.getSourceByUuid(uuidStr);
				if (src == null) src = PlayerAuthTracker.getSource(player.username);
				if (src != null) {
					PlayerAuthTracker.put(player.username, uuidStr, src);
				} else {
					PlayerAuthTracker.setUuid(player.username, uuidStr);
				}
			}
		}

		AuthSource source = uuidStr != null
			? PlayerAuthTracker.getSourceByUuid(uuidStr)
			: PlayerAuthTracker.getSource(player.username);
		if (source == null) source = AuthSource.ELY;

		AuthSource finalSource = source;

		new Thread(() -> {
			try {
				PlayerTextures profile = SkinResolver.resolve(player.username, finalSource, uuidStr);
				if (profile == null || profile.isEmpty()) {
					LOGGER.info("No skin resolved for '{}' via {}", player.username, finalSource);
					return;
				}
				String resolvedUuid = uuidStr;
				if (resolvedUuid != null) {
					PlayerAuthTracker.setProfileByUuid(resolvedUuid, profile);
					PlayerAuthTracker.bindEntity(player.id, resolvedUuid);
				} else {
					PlayerAuthTracker.setProfile(player.username, profile);
					resolvedUuid = PlayerAuthTracker.getUuidByName(player.username);
				}

				LOGGER.info("Resolved {} skin for '{}' uuid={} entity={} (skin={})",
					finalSource, player.username, UuidResolver.formatUuid(resolvedUuid), player.id, profile.getSkinUrl());

				if (resolvedUuid != null) {
					TextureSync.pushToAll(self, player.id, player.username, resolvedUuid, profile);
				}

				for (PlayerServer other : self.playerEntities) {
					if (other == player) continue;
					String otherUuid = PlayerAuthTracker.getUuidByEntity(other.id);
					if (otherUuid == null && other.uuid != null) {
						otherUuid = other.uuid.toString();
					}
					PlayerTextures otherProfile = otherUuid != null
						? PlayerAuthTracker.getProfileByUuid(otherUuid)
						: null;
					if (otherProfile != null && !otherProfile.isEmpty() && otherUuid != null) {
						TextureSync.pushTo(player, other.id, other.username, otherUuid, otherProfile);
					}
				}
			} catch (Exception e) {
				LOGGER.warn("Failed to broadcast skins for {}: {}", player.username, e.getMessage());
			}
		}, "bweb-skin-" + player.username).start();

		if (BWEB.WARN_MISSING_MOD) {
			final int entityId = player.id;
			final long delayMs = BWEB.WARN_MISSING_MOD_DELAY_SEC * 1000L;
			new Thread(() -> {
				try { Thread.sleep(delayMs); } catch (InterruptedException e) { return; }
				if (ClientPresence.hasMod(entityId)) return;
				try {
					player.playerNetServerHandler.sendPacket(new PacketMessage(BWEB.WARN_MISSING_MOD_MESSAGE));
				} catch (Exception e) {
					LOGGER.debug("Could not send mod warning to {}: {}", player.username, e.getMessage());
				}
			}, "bweb-warn-" + player.username).start();
		}
	}

	@Inject(method = "playerLoggedOut", at = @At("HEAD"), remap = false)
	private void bweb$onPlayerLoggedOut(PlayerServer entityplayermp, CallbackInfo ci) {
		if (entityplayermp == null) return;
		ClientPresence.remove(entityplayermp.id);
		PlayerAuthTracker.removeEntity(entityplayermp.id);
	}
}
