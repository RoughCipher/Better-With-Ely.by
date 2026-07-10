package ru.roughcipher.better_with_elyby.mixin;

import com.b100.utils.StringUtils;
import com.mojang.logging.LogUtils;
import net.minecraft.core.util.helper.GetMonsterSkinUrlThread;
import net.minecraft.core.util.helper.GetSkinUrlThread;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.roughcipher.better_with_elyby.config.BWEB;

@Mixin(value = {GetSkinUrlThread.class, GetMonsterSkinUrlThread.class}, remap = false)
public class SkinUrlThreadMixin {

	@Unique
	private static final Logger LOGGER = LogUtils.getLogger();

	@Inject(method = "getSkinObject", at = @At("HEAD"), cancellable = true, remap = false)
	private void onGetSkinObject(String name, CallbackInfoReturnable<String> cir) {
		if (!BWEB.ENABLED) {
			return;
		}

		try {
			String response = StringUtils.getWebsiteContentAsString(
				BWEB.SKIN_PROFILE_URL + name
			);
			if (response != null && !response.isEmpty()) {
				cir.setReturnValue(response);
				cir.cancel();
			}
		} catch (Exception e) {
			LOGGER.warn("Can't connect to Ely.by API for {}. Using default skin.", name);
		}
	}
}
