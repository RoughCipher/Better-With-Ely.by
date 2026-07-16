package ru.roughcipher.better_with_elyby;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import turniplabs.halplibe.HalpLibe;
import ru.roughcipher.better_with_elyby.config.BWEB;

public class BetterWithElyBy implements ModInitializer {
	public static final String MOD_ID = HalpLibe.registerMod("bweb", true);
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		BWEB.load();
		LOGGER.info("Better With Ely.By initialized.");
	}
}
