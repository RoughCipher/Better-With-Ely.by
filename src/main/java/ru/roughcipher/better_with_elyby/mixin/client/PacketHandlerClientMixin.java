package ru.roughcipher.better_with_elyby.mixin.client;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.net.handler.PacketHandlerClient;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.net.packet.Packet;
import net.minecraft.core.net.packet.PacketCustomPayload;
import net.minecraft.core.net.packet.PacketLogin;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.roughcipher.better_with_elyby.auth.UuidResolver;
import ru.roughcipher.better_with_elyby.config.BWEB;
import ru.roughcipher.better_with_elyby.network.ClientPresence;
import ru.roughcipher.better_with_elyby.network.TextureSync;
import ru.roughcipher.better_with_elyby.skin.PlayerTextures;
import ru.roughcipher.better_with_elyby.skin.TextureIndex;

import java.net.URL;

@Mixin(value = PacketHandlerClient.class, remap = false)
public class PacketHandlerClientMixin {
	@Unique
	private static final Logger LOGGER = LogUtils.getLogger();

	@Shadow @Final private Minecraft mc;

	@Shadow
	public void addToSendQueue(Packet packet) {
		throw new AssertionError();
	}

	@Inject(method = "handleLogin", at = @At("TAIL"), remap = false)
	private void bweb$sendHello(PacketLogin packetLogin, CallbackInfo ci) {
		try {
			this.addToSendQueue(new PacketCustomPayload(ClientPresence.CHANNEL, ClientPresence.HELLO_PAYLOAD));
		} catch (Exception e) {
			LOGGER.debug("Failed to send BWEB hello: {}", e.getMessage());
		}
	}

	@Redirect(
		method = "handleHandshake",
		at = @At(value = "NEW", target = "java/net/URL", args = "Ljava/lang/String;")
	)
	private URL redirectSessionUrl(String spec) throws Exception {
		if (!BWEB.IS_ELY_ACCOUNT) {
			return new URL(spec);
		}
		String newUrl = spec.replace(BWEB.OLD_SESSION, BWEB.SESSION_JOIN_URL);
		return new URL(newUrl);
	}

	@Inject(method = "handleCustomPayload", at = @At("HEAD"), remap = false)
	private void bweb$handleTextureSync(PacketCustomPayload packetCustomPayload, CallbackInfo ci) {
		if (packetCustomPayload == null || !TextureSync.CHANNEL.equals(packetCustomPayload.channel)) {
			return;
		}
		try {
			TextureSync.Snapshot snap = TextureSync.decode(packetCustomPayload.data);
			if (snap.isDrop()) {
				TextureIndex.remove(snap.name(), snap.uuid());
				LOGGER.info("[BWEB] Cleared textures for {}", snap.name());
				return;
			}
			TextureIndex.put(snap.name(), snap.uuid(), snap.textures());
			applyTextures(snap);
			LOGGER.info("[BWEB] Applied textures for {} uuid={} ({})",
				snap.name(), UuidResolver.formatUuid(snap.uuid()), snap.textures().getSource());
		} catch (Exception e) {
			LOGGER.warn("[BWEB] Bad texture payload: {}", e.getMessage());
		}
	}

	@Unique
	private void applyTextures(TextureSync.Snapshot snap) {
		Player player = findPlayer(snap.entityId(), snap.name());
		if (player == null) return;
		PlayerTextures tex = snap.textures();
		String oldSkin = player.skinURL;
		String oldCape = player.capeURL;
		player.skinURL = tex.getSkinUrl();
		player.capeURL = tex.getCapeUrl();
		player.slimModel = tex.isSlim();
		try {
			if (oldSkin != null) this.mc.textureManager.downloadedTextures.remove(oldSkin);
			if (oldCape != null) this.mc.textureManager.downloadedTextures.remove(oldCape);
			if (player.skinURL != null) this.mc.textureManager.downloadedTextures.remove(player.skinURL);
			if (player.capeURL != null) this.mc.textureManager.downloadedTextures.remove(player.capeURL);
		} catch (Exception e) {
			LOGGER.debug("Could not clear texture cache: {}", e.getMessage());
		}
	}

	@Unique
	private Player findPlayer(int entityId, String expectedUsername) {
		Player player = null;
		if (this.mc.thePlayer != null && this.mc.thePlayer.id == entityId) {
			player = this.mc.thePlayer;
		}
		if (player == null && this.mc.currentWorld != null) {
			Entity entity = this.mc.currentWorld.getEntityByID(entityId);
			if (entity instanceof Player) {
				player = (Player) entity;
			}
		}
		if (player != null && expectedUsername != null
			&& !expectedUsername.equalsIgnoreCase(player.username)) {
			LOGGER.debug("BWEB texture entity {} username {} vs expected {}", entityId, player.username, expectedUsername);
		}
		return player;
	}
}
