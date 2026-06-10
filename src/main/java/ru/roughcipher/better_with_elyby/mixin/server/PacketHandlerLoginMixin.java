package ru.roughcipher.better_with_elyby.mixin.server;

import net.minecraft.server.net.handler.PacketHandlerLogin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = PacketHandlerLogin.class, remap = false)
public class PacketHandlerLoginMixin {

    @ModifyConstant(
        method = "handleLogin",
        constant = @Constant(stringValue = "http://session.minecraft.net/game/checkserver.jsp?user=")
    )
    private String ElyByCheckServerUrl(String orig) {
        return "https://authserver.ely.by/session/legacy/hasJoined?user=";
    }
}
