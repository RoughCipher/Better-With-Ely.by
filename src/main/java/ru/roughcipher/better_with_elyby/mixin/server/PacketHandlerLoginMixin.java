package ru.roughcipher.better_with_elyby.mixin.server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import net.minecraft.core.net.packet.PacketLogin;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.net.handler.PacketHandlerLogin;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = PacketHandlerLogin.class, remap = false)
public abstract class PacketHandlerLoginMixin {

	@Shadow
	private static Logger LOGGER;
	@Shadow
	private String username;
	@Shadow
	private MinecraftServer mcServer;
	@Shadow
	private String serverId;
	@Shadow
	public abstract void kickUser(String message);
	@Shadow
	public abstract void doLogin(PacketLogin packetLogin);
	@Shadow
	public abstract String getUserAndIPString();
	@Shadow
	public static String getServerId(PacketHandlerLogin handler) {
		return null;
	}
	@Shadow
	public static PacketLogin setLoginPacket(PacketHandlerLogin handler, PacketLogin packet) {
		return null;
	}

	@Overwrite
	public void handleLogin(PacketLogin packetLogin) {
		this.username = packetLogin.username;

		if (packetLogin.playerEntityIdAndProtocolVersion != 32786) {
			if (packetLogin.playerEntityIdAndProtocolVersion > 32786) {
				this.kickUser("Outdated server!");
			} else {
				this.kickUser("Outdated client!");
			}
			return;
		}

		if (!this.mcServer.onlineMode) {
			this.doLogin(packetLogin);
		} else {
			new Thread(() -> {
				try {
					String serverHash = getServerId((PacketHandlerLogin) (Object) this);
					String encodedUser = URLEncoder.encode(packetLogin.username, "UTF-8");
					String encodedServerId = URLEncoder.encode(serverHash, "UTF-8");

					URL url = new URL("https://authserver.ely.by/session/legacy/hasJoined?user="
						+ encodedUser + "&serverId=" + encodedServerId);
					BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
					String response = reader.readLine();
					reader.close();

					if ("YES".equals(response)) {
						setLoginPacket((PacketHandlerLogin) (Object) this, packetLogin);
					} else {
						this.kickUser("Failed to verify username!");
					}
				} catch (Exception e) {
					LOGGER.error("Exception while trying to verify user '{}', kicking!", this.getUserAndIPString(), e);
					this.kickUser("Failed to verify username! [internal error " + e + "]");
				}
			}).start();
		}
	}
}
