package ru.roughcipher.better_with_elyby;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.net.command.CommandManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.roughcipher.better_with_elyby.command.BwebCommands;
import ru.roughcipher.better_with_elyby.config.BWEB;

public class BetterWithElyBy implements ModInitializer, DedicatedServerModInitializer {
	public static final String MOD_ID = "bweb";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		BWEB.load();
		LOGGER.info("Better With Ely.by initialized.");
	}

	@Override
	public void onInitializeServer() {
		CommandManager.registerServerCommand(new BwebCommands());
		LOGGER.info("BWEB server commands registered.");
	}
}
