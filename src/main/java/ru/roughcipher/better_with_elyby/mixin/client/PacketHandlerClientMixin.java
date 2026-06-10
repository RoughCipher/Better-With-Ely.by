package ru.roughcipher.better_with_elyby.mixin.client;

import net.minecraft.client.net.handler.PacketHandlerClient;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = PacketHandlerClient.class, remap = false)
public class PacketHandlerClientMixin {

    @ModifyConstant(
        method = "handleHandshake",
        constant = @Constant(stringValue = "http://session.minecraft.net/game/joinserver.jsp?user=")
    )
    private String ElyBySessionUrl(String orig) {
        return "https://authserver.ely.by/session/legacy/join?user=";
    }
}
