package ru.roughcipher.better_with_elyby;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import turniplabs.halplibe.HalpLibe;

public class BetterWithElyby implements ModInitializer {
	public static final String MOD_ID = HalpLibe.registerMod("better_with_elyby", true);
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Better With Ely.by initialized.");
	}
}
