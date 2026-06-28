package ru.roughcipher.better_with_elyby.mixin;

import com.b100.utils.StringUtils;
import com.mojang.logging.LogUtils;
import net.minecraft.core.util.helper.GetMonsterSkinUrlThread;
import net.minecraft.core.util.helper.GetSkinUrlThread;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.roughcipher.better_with_elyby.config.BWEBUrls;

@Mixin(value = {GetSkinUrlThread.class, GetMonsterSkinUrlThread.class}, remap = false)
public class SkinUrlThreadMixin {

	private static final Logger LOGGER = LogUtils.getLogger();

	@Inject(method = "getSkinObject", at = @At("HEAD"), cancellable = true, remap = false)
	private void onGetSkinObject(String name, CallbackInfoReturnable<String> cir) {
		LOGGER.info("Loading Skin for {} from Ely.by...", name);
		try {
			String response = StringUtils.getWebsiteContentAsString(
				BWEBUrls.SKIN_PROFILE_URL + name
			);
			cir.setReturnValue(response);
		} catch (Exception e) {
			LOGGER.warn("Can't connect to Ely.by API for {}.", name);
			cir.setReturnValue(null);
		}
		cir.cancel();
	}
}
