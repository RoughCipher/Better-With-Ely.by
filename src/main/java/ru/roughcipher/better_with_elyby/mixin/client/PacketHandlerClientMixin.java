package ru.roughcipher.better_with_elyby.mixin.client;

import net.minecraft.client.net.handler.PacketHandlerClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import ru.roughcipher.better_with_elyby.config.BWEB;

import java.net.URL;

@Mixin(value = PacketHandlerClient.class, remap = false)
public class PacketHandlerClientMixin {

	@Redirect(
		method = "handleHandshake",
		at = @At(value = "NEW", target = "java/net/URL", args = "Ljava/lang/String;")
	)
	private URL redirectSessionUrl(String urlString) throws Exception {
		if (!BWEB.ENABLED) {
			return new URL(urlString);
		}
		String newUrl = urlString.replace(BWEB.OLD_SESSION, BWEB.SESSION_JOIN_URL);
		return new URL(newUrl);
	}
}
