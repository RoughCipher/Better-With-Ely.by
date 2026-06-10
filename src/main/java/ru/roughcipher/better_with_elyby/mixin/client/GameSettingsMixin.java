package ru.roughcipher.better_with_elyby.mixin.client;

import net.minecraft.client.option.GameSettings;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = GameSettings.class, remap = false)
public class GameSettingsMixin {

    @ModifyConstant(
        method = "<clinit>",
        constant = @Constant(stringValue = "https://api.minecraftservices.com/minecraft/profile/lookup/name/%s")
    )
    private static String ElyByUUIDServerUrl(String orig) {
        return "https://authserver.ely.by/api/users/profiles/minecraft/%s";
    }
}
