package ru.roughcipher.better_with_elyby.mixin.server;

import net.minecraft.core.net.packet.PacketCustomPayload;
import net.minecraft.server.entity.player.PlayerServer;
import net.minecraft.server.net.handler.PacketHandlerServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.roughcipher.better_with_elyby.network.ClientPresence;

@Mixin(value = PacketHandlerServer.class, remap = false)
public class PacketHandlerServerMixin {
	@Shadow private PlayerServer playerEntity;

	@Inject(method = "handleCustomPayload", at = @At("HEAD"), remap = false)
	private void bweb$onHello(PacketCustomPayload packetCustomPayload, CallbackInfo ci) {
		if (packetCustomPayload == null || !ClientPresence.CHANNEL.equals(packetCustomPayload.channel)) return;
		if (this.playerEntity != null) ClientPresence.markHasMod(this.playerEntity.id);
	}
}
