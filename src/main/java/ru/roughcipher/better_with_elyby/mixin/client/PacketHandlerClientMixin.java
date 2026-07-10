package ru.roughcipher.better_with_elyby.mixin.client;

import java.net.URL;
import net.minecraft.client.net.handler.PacketHandlerClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import ru.roughcipher.better_with_elyby.config.BWEBUrls;

@Mixin(value = PacketHandlerClient.class, remap = false)
public class PacketHandlerClientMixin {

	@Redirect(
		method = "handleHandshake",
		at = @At(value = "NEW", target = "java/net/URL", args = "Ljava/lang/String;")
	)
	private URL redirectSessionUrl(String urlString) throws Exception {
		String newUrl = urlString.replace(BWEBUrls.OLD_SESSION, BWEBUrls.SESSION_JOIN_URL);
		return new URL(newUrl);
	}
}
