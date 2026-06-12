package ru.roughcipher.better_with_elyby.mixin.server;

import com.mojang.logging.LogUtils;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import net.minecraft.core.net.packet.PacketLogin;
import net.minecraft.server.net.handler.PacketHandlerLogin;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = PacketHandlerLogin.class, remap = false)
public abstract class PacketHandlerLoginMixin {
	private static final Logger LOGGER = LogUtils.getLogger();

	@Redirect(
		method = "handleLogin",
		at = @At(
			value = "INVOKE",
			target = "Ljava/lang/Thread;start()V"
		)
	)
	private void redirectSessionCheck(Thread originalThread, PacketLogin loginPacket) {
		PacketHandlerLogin self = (PacketHandlerLogin) (Object) this;

		new Thread(() -> {
			try {
				String serverId = PacketHandlerLogin.getServerId(self);
				String encodedUser = URLEncoder.encode(loginPacket.username, "UTF-8");
				String encodedServerId = URLEncoder.encode(serverId, "UTF-8");

				URL url = new URL("https://authserver.ely.by/session/legacy/hasJoined?user=" + encodedUser + "&serverId=" + encodedServerId);

				BufferedReader bufferedreader = new BufferedReader(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8));
				String response = bufferedreader.readLine();
				bufferedreader.close();

				if (response.equals("YES")) {
					PacketHandlerLogin.setLoginPacket(self, loginPacket);
				} else {
					self.kickUser("Failed to verify username!");
				}
			} catch (Exception exception) {
				LOGGER.error("Exception while trying to verify user '{}', kicking!",
					self.getUserAndIPString(), exception);
				self.kickUser("Failed to verify username! [internal error " + exception + "]");
			}
		}).start();
	}
}
