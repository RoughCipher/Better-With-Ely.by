package ru.roughcipher.better_with_elyby.mixin.server;

import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = MinecraftServer.class, remap = false)
public class MinecraftServerMixin {

	@ModifyConstant(
		method = "startServer",
		constant = @Constant(stringValue = "https://api.minecraftservices.com/minecraft/profile/lookup/name/%s"),
		require = 0
	)
	private String elyByUuidServiceUrl(String orig) {
		return "https://authserver.ely.by/api/users/profiles/minecraft/%s";
	}
}
