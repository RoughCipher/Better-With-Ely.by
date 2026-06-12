package ru.roughcipher.better_with_elyby.mixin;

import com.b100.utils.StringUtils;
import com.mojang.logging.LogUtils;
import net.minecraft.core.util.helper.GetSkinUrlThread;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = GetSkinUrlThread.class, remap = false)
public class GetSkinUrlThreadMixin {

	private static final Logger LOGGER = LogUtils.getLogger();

	@Overwrite
	private String getSkinObject(String name) {
		LOGGER.info("Loading Skin for Player {} from Ely.by...", name);
		try {
			String string = StringUtils.getWebsiteContentAsString(
				"https://skinsystem.ely.by/profile/" + name
			);
			return string;
		} catch (Exception e) {
			LOGGER.warn("Can't connect to Ely.by API for player {}.", name);
			return null;
		}
	}
}
