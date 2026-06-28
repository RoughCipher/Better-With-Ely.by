package ru.roughcipher.better_with_elyby.mixin.client;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Minecraft.class, remap = false)
public class MinecraftMixin {

	@Inject(method = "startCheckPaidThread", at = @At("HEAD"), cancellable = true, remap = false)
	private void onStartCheckPaidThread(CallbackInfo ci) {
		ci.cancel();
	}
}
