package ru.roughcipher.better_with_elyby.mixin;

import net.minecraft.core.util.helper.UUIDHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = UUIDHelper.class, remap = false)
public class UUIDHelperMixin {

    @Shadow
    public static String urlUUID;

    static {
        urlUUID = "https://authserver.ely.by/api/users/profiles/minecraft/%s";
    }
}
