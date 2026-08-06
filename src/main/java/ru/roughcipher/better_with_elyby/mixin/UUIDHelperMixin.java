package ru.roughcipher.better_with_elyby.mixin;

import com.b100.utils.StringUtils;
import net.minecraft.core.util.helper.UUIDHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import ru.roughcipher.better_with_elyby.auth.UuidResolver;

@Mixin(value = UUIDHelper.class, remap = false)
public class UUIDHelperMixin {

	@Redirect(
		method = "getUUIDFromName",
		at = @At(
			value = "INVOKE",
			target = "Lcom/b100/utils/StringUtils;getWebsiteContentAsString(Ljava/lang/String;)Ljava/lang/String;"
		)
	)
	private static String redirectUuidLookup(String url) {
		String playerName = url.substring(url.lastIndexOf('/') + 1);
		int q = playerName.indexOf('?');
		if (q >= 0) playerName = playerName.substring(0, q);

		try {
			String resolved = UuidResolver.resolveForLookup(playerName);
			if (resolved != null && !resolved.isEmpty()) {
				return resolved;
			}
		} catch (Exception ignored) {
		}
		return StringUtils.getWebsiteContentAsString(url);
	}
}
