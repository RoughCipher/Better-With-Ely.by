package ru.roughcipher.better_with_elyby.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.ArgumentTypeString;
import com.mojang.brigadier.builder.ArgumentBuilderLiteral;
import com.mojang.brigadier.builder.ArgumentBuilderRequired;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.net.command.CommandManager;
import net.minecraft.core.net.command.CommandSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.entity.player.PlayerServer;
import net.minecraft.server.net.command.IServerCommandSource;
import ru.roughcipher.better_with_elyby.auth.AuthSource;
import ru.roughcipher.better_with_elyby.auth.UuidResolver;

import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

@Environment(EnvType.SERVER)
public final class BwebCommands implements CommandManager.CommandRegistry {

	private static final Predicate<CommandSource> ADMIN = CommandSource::hasAdmin;

	private static final SuggestionProvider<CommandSource> BACKEND_SUGGESTIONS = (ctx, builder) -> {
		String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
		for (String s : new String[]{"ely", "mojang"}) {
			if (s.startsWith(remaining)) {
				builder.suggest(s);
			}
		}
		return builder.buildFuture();
	};

	@Override
	@SuppressWarnings({"unchecked", "rawtypes"})
	public void register(CommandDispatcher<CommandSource> dispatcher) {
		dispatcher.register(
			(ArgumentBuilderLiteral) ArgumentBuilderLiteral.literal("ban")
				.requires((Predicate) ADMIN)
				.then(ArgumentBuilderLiteral.literal("username")
					.then(ArgumentBuilderRequired.argument("name", (ArgumentType) ArgumentTypeString.word())
						.then(ArgumentBuilderRequired.argument("backend", (ArgumentType) ArgumentTypeString.word())

							.suggests(BACKEND_SUGGESTIONS)
							.executes(c -> ban(
								(CommandSource) c.getSource(),
								(String) c.getArgument("name", String.class),
								(String) c.getArgument("backend", String.class))))))
		);
		dispatcher.register(
			(ArgumentBuilderLiteral) ArgumentBuilderLiteral.literal("unban")
				.requires((Predicate) ADMIN)
				.then(ArgumentBuilderLiteral.literal("username")
					.then(ArgumentBuilderRequired.argument("name", (ArgumentType) ArgumentTypeString.word())
						.then(ArgumentBuilderRequired.argument("backend", (ArgumentType) ArgumentTypeString.word())

							.suggests(BACKEND_SUGGESTIONS)
							.executes(c -> unban(
								(CommandSource) c.getSource(),
								(String) c.getArgument("name", String.class),
								(String) c.getArgument("backend", String.class))))))
		);
		dispatcher.register(
			(ArgumentBuilderLiteral) ArgumentBuilderLiteral.literal("whitelist")
				.requires((Predicate) ADMIN)
				.then(ArgumentBuilderLiteral.literal("add")
					.then(ArgumentBuilderRequired.argument("name", (ArgumentType) ArgumentTypeString.word())
						.then(ArgumentBuilderRequired.argument("backend", (ArgumentType) ArgumentTypeString.word())

							.suggests(BACKEND_SUGGESTIONS)
							.executes(c -> whitelistAdd(
								(CommandSource) c.getSource(),
								(String) c.getArgument("name", String.class),
								(String) c.getArgument("backend", String.class))))))
				.then(ArgumentBuilderLiteral.literal("remove")
					.then(ArgumentBuilderRequired.argument("name", (ArgumentType) ArgumentTypeString.word())
						.then(ArgumentBuilderRequired.argument("backend", (ArgumentType) ArgumentTypeString.word())

							.suggests(BACKEND_SUGGESTIONS)
							.executes(c -> whitelistRemove(
								(CommandSource) c.getSource(),
								(String) c.getArgument("name", String.class),
								(String) c.getArgument("backend", String.class))))))
		);
	}

	private static AuthSource parseBackend(String raw) {
		if (raw == null) return null;
		String s = raw.trim().toLowerCase(Locale.ROOT);
		if ("ely".equals(s)) return AuthSource.ELY;
		if ("mojang".equals(s)) return AuthSource.MOJANG;
		return null;
	}

	private static String extractId(String json) {
		int i = json.indexOf("\"id\"");
		if (i < 0) return json;
		int colon = json.indexOf(':', i);
		int q1 = json.indexOf('"', colon + 1);
		int q2 = json.indexOf('"', q1 + 1);
		if (q1 < 0 || q2 < 0) return json;
		return json.substring(q1 + 1, q2);
	}

	private static int ban(CommandSource source, String name, String backendRaw) {
		return act(source, name, backendRaw, (server, uuid, n) -> {
			server.playerList.banPlayer(uuid);
			PlayerServer online = server.playerList.getPlayerEntity(n);
			if (online != null && online.uuid.equals(uuid)) {
				online.playerNetServerHandler.kickPlayer("Banned by admin");
			}
			source.sendMessage("Banned " + n + " (" + uuid + ")");
		});
	}

	private static int unban(CommandSource source, String name, String backendRaw) {
		return act(source, name, backendRaw, (server, uuid, n) -> {
			server.playerList.pardonPlayer(uuid);
			source.sendMessage("Unbanned " + n + " (" + uuid + ")");
		});
	}

	private static int whitelistAdd(CommandSource source, String name, String backendRaw) {
		return act(source, name, backendRaw, (server, uuid, n) -> {
			server.playerList.addToWhiteList(uuid);
			source.sendMessage("Whitelisted " + n + " (" + uuid + ")");
		});
	}

	private static int whitelistRemove(CommandSource source, String name, String backendRaw) {
		return act(source, name, backendRaw, (server, uuid, n) -> {
			server.playerList.removeFromWhiteList(uuid);
			source.sendMessage("Removed " + n + " from whitelist (" + uuid + ")");
		});
	}

	@FunctionalInterface
	private interface UuidAction {
		void run(MinecraftServer server, UUID uuid, String name);
	}

	private static int act(CommandSource source, String name, String backendRaw, UuidAction action) {
		AuthSource backend = parseBackend(backendRaw);
		if (backend == null) {
			source.sendMessage("Backend must be ely or mojang.");
			return 0;
		}
		MinecraftServer server = ((IServerCommandSource) source).getServer();
		CompletableFuture.runAsync(() -> {
			try {
				UuidResolver.setForcedSource(backend);
				String json = UuidResolver.resolveForLookup(name);
				if (json == null) {
					source.sendMessage("No UUID for '" + name + "' on " + backend);
					return;
				}
				String id = extractId(json);
				UUID uuid = UUID.fromString(UuidResolver.formatUuid(id));
				action.run(server, uuid, name);
			} catch (Exception e) {
				source.sendMessage("Failed: " + e.getMessage());
			} finally {
				UuidResolver.clearForcedSource();
			}
		});
		return 1;
	}
}
