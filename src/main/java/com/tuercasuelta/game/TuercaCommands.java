package com.tuercasuelta.game;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class TuercaCommands {
	private TuercaCommands() {}

	public static void register(CommandDispatcher<CommandSourceStack> d) {
		d.register(Commands.literal("tuerca")
				.executes(c -> act(c, "menu", ""))
				.then(Commands.literal("menu").executes(c -> act(c, "menu", "")))
				.then(Commands.literal("panel").executes(c -> act(c, "panel", "")))
				.then(Commands.literal("ayuda").executes(c -> act(c, "help", "")))
				.then(Commands.literal("parar").executes(c -> act(c, "stop", "")))
				.then(Commands.literal("cartilla").executes(c -> act(c, "cartilla", "")))
				.then(Commands.literal("entregar").executes(c -> act(c, "deliver", "")))
				.then(Commands.literal("votar").executes(c -> act(c, "callvote", "")))
				.then(Commands.literal("start")
						.executes(c -> act(c, "start", ""))
						.then(Commands.argument("nombres", StringArgumentType.greedyString())
								.executes(c -> act(c, "start", StringArgumentType.getString(c, "nombres")))))
				.then(Commands.literal("voto")
						.then(Commands.argument("nombre", StringArgumentType.word())
								.executes(c -> act(c, "vote", StringArgumentType.getString(c, "nombre")))))
				.then(Commands.literal("pick")
						.then(Commands.argument("n", StringArgumentType.word())
								.executes(c -> act(c, "pick", StringArgumentType.getString(c, "n")))))
				.then(Commands.literal("poder")
						.then(Commands.argument("nombre", StringArgumentType.word())
								.executes(c -> act(c, "power", StringArgumentType.getString(c, "nombre")))))
				.then(Commands.literal("probar")
						.then(Commands.argument("n", StringArgumentType.word())
								.executes(c -> act(c, "test", StringArgumentType.getString(c, "n")))))
		);
	}

	private static int act(CommandContext<CommandSourceStack> c, String action, String arg) {
		ServerPlayer p = c.getSource().getPlayer();
		if (p == null) {
			c.getSource().sendFailure(Component.literal("Este comando es solo para jugadores."));
			return 0;
		}
		Game.server = c.getSource().getServer();
		Game.I.handleAction(p, action, arg);
		return Command.SINGLE_SUCCESS;
	}
}
