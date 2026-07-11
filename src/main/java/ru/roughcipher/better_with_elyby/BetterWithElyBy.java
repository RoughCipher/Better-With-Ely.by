package ru.roughcipher.better_with_elyby;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BetterWithElyBy implements ModInitializer {
	public static final String MOD_ID = "bweb";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Better With Ely.By initialized.");
	}
}
