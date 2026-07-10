package ru.roughcipher.better_with_elyby.mixin;

import com.b100.utils.StringUtils;
import net.minecraft.core.util.helper.UUIDHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import ru.roughcipher.better_with_elyby.config.BWEBUrls;

@Mixin(value = UUIDHelper.class, remap = false)
public class UUIDHelperMixin {

	@Redirect(
		method = "getUUIDFromName",
		at = @At(
			value = "INVOKE",
			target = "Lcom/b100/utils/StringUtils;getWebsiteContentAsString(Ljava/lang/String;)Ljava/lang/String;"
		)
	)
	private static String redirectUuidLookup(String originalUrl) {
		String playerName = originalUrl.substring(originalUrl.lastIndexOf('/') + 1);
		String elyUrl = String.format(BWEBUrls.UUID_LOOKUP_URL, playerName);
		return StringUtils.getWebsiteContentAsString(elyUrl);
	}
}
