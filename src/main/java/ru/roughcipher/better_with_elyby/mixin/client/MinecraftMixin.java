package ru.roughcipher.better_with_elyby.mixin.client;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = Minecraft.class, remap = false)
public class MinecraftMixin {

    @Overwrite
    private void startCheckPaidThread() {
    }
}
