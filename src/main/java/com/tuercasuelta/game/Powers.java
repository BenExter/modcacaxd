package com.tuercasuelta.game;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Poderes del saboteador y acciones de los fantasmas. */
final class Powers {
	private Powers() {}

	static String label(String key) {
		return switch (key) {
			case "calavera" -> "Calavera y meteoritos";
			case "esconder" -> "Esconder una pieza";
			case "cambiar" -> "Cambiar objetos del bingo";
			case "apagon" -> "Apagon de 20 s";
			case "intercambio" -> "Intercambiar a todos de lugar";
			case "mobs" -> "Invocar mobs al azar";
			default -> key;
		};
	}

	static void use(Game g, ServerPlayer p, String key) {
		if (g.state != Game.State.GATHER) {
			g.say(p, "Los poderes solo se usan mientras consiguen las piezas (sin votacion en curso).");
			return;
		}
		if (!g.isSab(p) || !g.sabActive()) {
			g.say(p, "No tienes poderes.");
			return;
		}
		Integer left = g.charges.get(key);
		if (left == null || left <= 0) {
			g.say(p, "No te quedan cargas de ese poder.");
			return;
		}
		if (g.tick < g.powerReadyAt) {
			g.say(p, "Debes esperar " + ((g.powerReadyAt - g.tick) / 20 + 1) + " s para usar un poder.");
			return;
		}
		boolean ok = switch (key) {
			case "calavera" -> calavera(g);
			case "esconder" -> esconder(g, p);
			case "cambiar" -> cambiar(g, p);
			case "apagon" -> apagon(g);
			case "intercambio" -> intercambio(g, p);
			case "mobs" -> mobs(g);
			default -> false;
		};
		if (ok) {
			g.charges.put(key, left - 1);
			g.powerReadyAt = g.tick + Game.POWER_COOLDOWN;
			g.say(p, "Poder usado: " + label(key) + ". Cargas restantes: " + (left - 1));
		}
	}

	static String off(double r) {
		return Cmd.f(r);
	}

	static boolean calavera(Game g) {
		for (String n : g.players) {
			ServerPlayer q = g.sp(n);
			if (q != null) g.ui(q, "skull", "");
			Cmd.title(n, "¡LLUEVEN METEORITOS!", "", "dark_red");
			Cmd.sound(n, "minecraft:entity.wither.spawn", 1f, 0.6f);
		}
		for (int k = 0; k < 14; k++) {
			final int kk = k;
			g.schedule(70L + k * 14L, () -> {
				for (String n : g.alive()) {
					if (n.equals(g.saboteur)) continue;
					double ox = (g.rnd.nextDouble() - 0.5) * 12;
					double oz = (g.rnd.nextDouble() - 0.5) * 12;
					Cmd.run("execute as " + n + " at @s run summon minecraft:fireball ~" + off(ox) + " ~24 ~" + off(oz)
							+ " {Motion:[0.0d,-1.4d,0.0d],power:[0.0d,-0.1d,0.0d],ExplosionPower:1b}");
					if (kk % 4 == 0) {
						Cmd.run("execute as " + n + " at @s run summon minecraft:creeper ~4 ~ ~4 {powered:1b}");
					}
				}
			});
		}
		return true;
	}

	static boolean esconder(Game g, ServerPlayer p) {
		List<Integer> done = new ArrayList<>();
		for (int i = 0; i < Game.SLOTS; i++) if (g.delivered[i]) done.add(i);
		if (done.isEmpty()) {
			g.say(p, "Todavia no hay piezas entregadas para esconder (no gastaste la carga).");
			return false;
		}
		int i = done.get(g.rnd.nextInt(done.size()));
		g.delivered[i] = false;
		Parts.place(g, i, false);
		g.broadcast("¡Una pieza del robot desaparecio! Hay que volver a entregar: " + g.card[i].name());
		for (String n : g.players) Cmd.sound(n, "minecraft:entity.enderman.teleport", 1f, 0.6f);
		return true;
	}

	static boolean cambiar(Game g, ServerPlayer p) {
		List<Integer> pend = new ArrayList<>();
		for (int i = 0; i < Game.SLOTS; i++) if (!g.delivered[i] && g.card[i] != null) pend.add(i);
		if (pend.size() < 2) {
			g.say(p, "No hay suficientes piezas pendientes para cambiar (no gastaste la carga).");
			return false;
		}
		int a = pend.remove(g.rnd.nextInt(pend.size()));
		int b = pend.get(g.rnd.nextInt(pend.size()));
		Items.Item tmp = g.card[a];
		g.card[a] = g.card[b];
		g.card[b] = tmp;
		g.broadcast("¡La cartilla fue alterada! Dos casillas cambiaron de objeto. Revisen /tuerca cartilla.");
		for (String n : g.players) Cmd.sound(n, "minecraft:block.note_block.bass", 1f, 0.5f);
		return true;
	}

	static boolean apagon(Game g) {
		g.broadcast("¡SE APAGARON LAS LUCES!");
		for (String n : g.players) {
			if (n.equals(g.saboteur)) {
				Cmd.run("effect give " + n + " minecraft:night_vision 20 0 true");
			} else {
				Cmd.run("effect give " + n + " minecraft:blindness 20 0 true");
				Cmd.run("effect give " + n + " minecraft:darkness 20 0 true");
			}
			Cmd.sound(n, "minecraft:entity.warden.heartbeat", 1f, 1f);
		}
		return true;
	}

	static boolean intercambio(Game g, ServerPlayer p) {
		List<String> names = new ArrayList<>();
		List<double[]> pos = new ArrayList<>();
		for (String n : g.alive()) {
			ServerPlayer q = g.sp(n);
			if (q == null) continue;
			names.add(n);
			pos.add(new double[]{q.getX(), q.getY(), q.getZ()});
		}
		if (names.size() < 2) {
			g.say(p, "Se necesitan al menos 2 jugadores en pie (no gastaste la carga).");
			return false;
		}
		for (int i = 0; i < names.size(); i++) {
			double[] d = pos.get((i + 1) % names.size());
			Cmd.run("tp " + names.get(i) + " " + Cmd.f(d[0]) + " " + Cmd.f(d[1]) + " " + Cmd.f(d[2]));
			Cmd.sound(names.get(i), "minecraft:entity.enderman.teleport", 1f, 1f);
		}
		g.broadcast("¡Todos cambiaron de lugar!");
		return true;
	}

	static boolean mobs(Game g) {
		for (String n : g.alive()) {
			if (n.equals(g.saboteur)) continue;
			for (int k = 0; k < 2; k++) {
				String mob = Items.MOBS[g.rnd.nextInt(Items.MOBS.length)];
				double ox = (g.rnd.nextDouble() - 0.5) * 6;
				double oz = (g.rnd.nextDouble() - 0.5) * 6;
				Cmd.run("execute as " + n + " at @s run summon minecraft:" + mob + " ~" + off(ox) + " ~ ~" + off(oz));
			}
			Cmd.sound(n, "minecraft:entity.evoker.prepare_summon", 1f, 1f);
		}
		g.broadcast("¡Aparecieron mobs de la nada!");
		return true;
	}

	// ---- Fantasmas ----

	static void ghost(Game g, ServerPlayer p, String arg) {
		String me = Game.name(p);
		if (!g.ghosts.contains(me)) {
			g.say(p, "Solo los fantasmas pueden hacer esto.");
			return;
		}
		if (g.state != Game.State.GATHER && g.state != Game.State.VOTING) return;
		long ready = g.ghostReadyAt.getOrDefault(me, 0L);
		if (g.tick < ready) {
			g.say(p, "Espera " + ((ready - g.tick) / 20 + 1) + " s para volver a molestar.");
			return;
		}
		String kind = arg;
		String target = "";
		int idx = arg.indexOf(':');
		if (idx >= 0) {
			kind = arg.substring(0, idx);
			target = arg.substring(idx + 1);
		}
		List<String> pool = g.alive();
		if (kind.equals("robar")) {
			ServerPlayer t = g.sp(target);
			if (t == null || !pool.contains(target)) {
				g.say(p, "Ese jugador no esta disponible.");
				return;
			}
			Inventory inv = t.getInventory();
			List<Integer> slots = new ArrayList<>();
			for (int i = 0; i < inv.getContainerSize(); i++) {
				if (!inv.getItem(i).isEmpty()) slots.add(i);
			}
			if (slots.isEmpty()) {
				g.say(p, target + " no tiene nada que robar.");
				return;
			}
			int s = slots.get(g.rnd.nextInt(slots.size()));
			ItemStack st = inv.getItem(s);
			String nm = st.getHoverName().getString();
			inv.setItem(s, ItemStack.EMPTY);
			g.say(t, "¡Un fantasma te robo un objeto: " + nm + "!");
			g.say(p, "Le robaste " + nm + " a " + target + ".");
			Cmd.sound(target, "minecraft:entity.ghast.warn", 1f, 1.4f);
		} else if (kind.equals("susto")) {
			if (pool.isEmpty()) return;
			String t = target.isEmpty() ? pool.get(g.rnd.nextInt(pool.size())) : target;
			Cmd.title(t, "¡BUUU!", "", "dark_red");
			Cmd.sound(t, "minecraft:entity.ghast.scream", 1f, 0.8f);
			Cmd.run("effect give " + t + " minecraft:darkness 4 0 true");
			g.say(p, "Asustaste a " + t + ".");
		} else {
			return;
		}
		g.ghostReadyAt.put(me, g.tick + Game.GHOST_COOLDOWN);
	}
}
