package ru.roughcipher.better_with_elyby.mixin;

import com.b100.json.element.JsonArray;
import com.b100.json.element.JsonObject;
import com.b100.json.JsonParser;
import com.b100.utils.StringUtils;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.util.helper.GetSkinUrlThread;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = GetSkinUrlThread.class, remap = false)
public abstract class GetSkinUrlThreadMixin {

	@Shadow
	private static Logger LOGGER;
	@Shadow
	private static JsonParser jsonParser;
	@Shadow
	private Player player;

	private static final String urlSkin = "http://skinsystem.ely.by/profile/";

	@Overwrite
	private String getSkinObject(String name) {
		LOGGER.info("Loading Skin for Player {}...", name);
		try {
			return StringUtils.getWebsiteContentAsString(urlSkin + this.player.username);
		} catch (Exception e) {
			LOGGER.warn("Invalid name {}, or can't connect to the ElyBy API.", this.player.username);
			return null;
		}
	}

	@Overwrite
	public void run() {
		String name = this.player.username;
		if (name == null || name.isEmpty()) {
			return;
		}

		String string = null;
		for (int i = 0; i < 3 && ((string = this.getSkinObject(name)) == null || string.isEmpty()); ++i) {
			try {
				Thread.sleep(5000L);
			} catch (InterruptedException e) {
				break;
			}
		}

		if (string == null || string.isEmpty()) {
			return;
		}

		JsonObject object = jsonParser.parse(string);
		JsonArray properties = object.getArray("properties");
		JsonObject textureProperty = properties.query(
			e -> e.getAsObject().getString("name").equalsIgnoreCase("textures")
		).getAsObject();

		JsonObject texturesObject = jsonParser.parse(
			GetSkinUrlThread.decodeBase64(textureProperty.getString("value"))
		).getObject("textures");

		if (texturesObject.has("SKIN")) {
			this.player.skinURL = texturesObject.getObject("SKIN").getString("url");
			if (texturesObject.getObject("SKIN").has("metadata") &&
				texturesObject.getObject("SKIN").getObject("metadata").getString("model").equals("slim")) {
				this.player.slimModel = true;
			}
		}

		if (texturesObject.has("CAPE")) {
			this.player.capeURL = texturesObject.getObject("CAPE").getString("url");
		}

		LOGGER.info("Skin URL: {}", this.player.skinURL);
		LOGGER.info("Cape URL: {}", this.player.capeURL);
	}
}
