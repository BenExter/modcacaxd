package com.tuercasuelta.game;

import net.minecraft.server.MinecraftServer;

import java.util.Locale;

/** Ayudas para ejecutar comandos de Minecraft desde el servidor (efectos, titulos, sonidos...). */
final class Cmd {
	private Cmd() {}

	/** Ejecuta un comando (sin la barra). Devuelve su resultado, o -1 si fallo. */
	static int run(String command) {
		MinecraftServer s = Game.server;
		if (s == null) return -1;
		try {
			return s.getCommands().getDispatcher().execute(command, s.createCommandSourceStack().withSuppressedOutput());
		} catch (Exception e) {
			return -1;
		}
	}

	static String f(double d) {
		return String.format(Locale.ROOT, "%.2f", d);
	}

	static String esc(String s) {
		return s.replace("\\", "\\\\").replace("\"", "\\\"");
	}

	static String text(String t, String color) {
		return "{\"text\":\"" + esc(t) + "\",\"color\":\"" + color + "\"}";
	}

	static void title(String name, String title, String sub, String color) {
		run("title " + name + " times 10 70 20");
		run("title " + name + " subtitle " + text(sub, "white"));
		run("title " + name + " title " + text(title, color));
	}

	static void actionbar(String name, String t, String color) {
		run("title " + name + " actionbar " + text(t, color));
	}

	static void sound(String name, String id, float vol, float pitch) {
		run("execute as " + name + " at @s run playsound " + id + " master @s ~ ~ ~ " + vol + " " + pitch);
	}
}
