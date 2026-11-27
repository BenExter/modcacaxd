package com.tuercasuelta.game;

import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Los 8 finales. Equipo (4): baila, persigue al impostor, confeti, viaje a la luna.
 * Saboteador (4): explota, se lo roba, se va volando, mata a todos.
 * Se elige uno al azar (o uno concreto con /tuerca probar).
 */
final class Endings {
	private Endings() {}

	static double gx, gy, gz;

	static final String ROBOT = "@e[tag=tuerca_robot,limit=1]";

	static void start(Game g, boolean team, int forced) {
		g.state = Game.State.ENDING;
		g.endingStart = g.tick;
		g.endingLoop = null;
		g.tasks.clear();
		spawnGolem(g);
		int kind = forced >= 0 ? forced : g.rnd.nextInt(4);
		for (String n : g.players) {
			Cmd.run("effect clear " + n);
			if (team) {
				Cmd.title(n, "¡ROBOT TERMINADO!", "Se esta moviendo...", "gold");
			} else {
				Cmd.title(n, "¡EL SABOTEADOR GANA!", "Algo se esta moviendo...", "dark_red");
			}
			Cmd.sound(n, "minecraft:entity.iron_golem.repair", 1f, 0.6f);
		}
		if (team) {
			switch (kind) {
				case 0 -> dance(g);
				case 1 -> chase(g);
				case 2 -> confetti(g);
				default -> moon(g);
			}
		} else {
			switch (kind) {
				case 0 -> explode(g);
				case 1 -> steal(g);
				case 2 -> fly(g);
				default -> slay(g);
			}
		}
	}

	// ---- Robot gigante (golem escalado) ----

	static void spawnGolem(Game g) {
		Parts.clearAll(g);
		gx = g.rx;
		gy = g.ry;
		gz = g.rz + 1;
		Cmd.run("kill " + ROBOT);
		Cmd.run("execute in minecraft:overworld run summon minecraft:iron_golem " + Cmd.f(gx) + " " + Cmd.f(gy) + " " + Cmd.f(gz)
				+ " {NoAI:1b,Invulnerable:1b,PersistenceRequired:1b,Tags:[\"tuerca_robot\"]}");
		Cmd.run("execute in minecraft:overworld run attribute " + ROBOT + " minecraft:scale base set 6");
	}

	static void place(double x, double y, double z, double yaw) {
		Cmd.run("execute in minecraft:overworld run tp " + ROBOT + " " + Cmd.f(x) + " " + Cmd.f(y) + " " + Cmd.f(z) + " " + Cmd.f(yaw) + " 0");
	}

	static void placeFacing(double x, double y, double z, double lx, double ly, double lz) {
		Cmd.run("execute in minecraft:overworld run tp " + ROBOT + " " + Cmd.f(x) + " " + Cmd.f(y) + " " + Cmd.f(z)
				+ " facing " + Cmd.f(lx) + " " + Cmd.f(ly) + " " + Cmd.f(lz));
	}

	/** Mueve el robot hacia un punto. Devuelve la distancia que habia antes de moverse. */
	static double step(double tx, double ty, double tz, double speed) {
		double dx = tx - gx, dy = ty - gy, dz = tz - gz;
		double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
		if (d > 0.001) {
			double k = Math.min(speed, d) / d;
			gx += dx * k;
			gy += dy * k;
			gz += dz * k;
		}
		placeFacing(gx, gy, gz, tx, ty + 1, tz);
		return d;
	}

	static void particles(String id, double x, double y, double z, double spread, int count) {
		Cmd.run("execute in minecraft:overworld run particle " + id + " " + Cmd.f(x) + " " + Cmd.f(y) + " " + Cmd.f(z)
				+ " " + Cmd.f(spread) + " " + Cmd.f(spread) + " " + Cmd.f(spread) + " 0.3 " + count + " force");
	}

	static void soundAll(Game g, String id, float vol, float pitch) {
		for (String n : g.players) Cmd.sound(n, id, vol, pitch);
	}

	// =====================================================================
	// Finales del equipo
	// =====================================================================

	static void dance(Game g) {
		g.broadcast("El robot se puso a BAILAR.");
		final double baseY = gy;
		g.endingLoop = t -> {
			double jump = (t % 8 < 4) ? 1.0 : 0.0;
			if (t % 2 == 0) place(gx, baseY + jump, gz, (t * 9) % 360);
			if (t % 5 == 0) {
				particles("minecraft:note", gx, baseY + 10, gz, 4, 6);
				particles("minecraft:totem_of_undying", gx, baseY + 6, gz, 5, 12);
			}
			if (t % 10 == 0) {
				float pitch = 0.6f + (float) ((t / 10) % 6) * 0.25f;
				soundAll(g, "minecraft:block.note_block.bell", 1f, pitch);
				soundAll(g, "minecraft:block.note_block.bass", 1f, pitch * 0.5f);
			}
			if (t >= 300) g.finishEnding(true);
		};
	}

	static void chase(Game g) {
		g.broadcast("El robot cobro vida... ¡y va tras el impostor!");
		final boolean[] caught = {false};
		final long[] caughtAt = {0};
		g.endingLoop = t -> {
			ServerPlayer s = g.sp(g.saboteur);
			if (s == null || t > 600) {
				g.finishEnding(true);
				return;
			}
			if (!caught[0]) {
				double d = step(s.getX(), s.getY(), s.getZ(), 0.55);
				if (t % 6 == 0) soundAll(g, "minecraft:entity.iron_golem.step", 1f, 0.5f);
				if (d < 5) {
					caught[0] = true;
					caughtAt[0] = t;
					Cmd.title(g.saboteur, "¡ATRAPADO!", "El robot te encontro", "gold");
					Cmd.run("effect give " + g.saboteur + " minecraft:levitation 4 1 true");
					Cmd.run("effect give " + g.saboteur + " minecraft:slow_falling 12 0 true");
					particles("minecraft:explosion_emitter", s.getX(), s.getY() + 1, s.getZ(), 1, 3);
					soundAll(g, "minecraft:entity.generic.explode", 1f, 1f);
				}
			} else if (t - caughtAt[0] > 70) {
				g.finishEnding(true);
			}
		};
	}

	static void confetti(Game g) {
		g.broadcast("¡CONFETI! El robot lanzo una fiesta.");
		g.endingLoop = t -> {
			if (t % 2 == 0) {
				for (String n : g.players) {
					if (g.sp(n) == null) continue;
					Cmd.run("execute as " + n + " at @s run particle minecraft:totem_of_undying ~ ~2 ~ 2 2 2 0.5 20 force");
					Cmd.run("execute as " + n + " at @s run particle minecraft:firework ~ ~3 ~ 2 1 2 0.2 10 force");
				}
				particles("minecraft:totem_of_undying", gx, gy + 12, gz, 6, 30);
			}
			if (t % 10 == 0) soundAll(g, "minecraft:entity.firework_rocket.blast", 1f, 1f);
			if (t % 20 == 0) soundAll(g, "minecraft:entity.firework_rocket.twinkle", 1f, 1f);
			if (t >= 300) g.finishEnding(true);
		};
	}

	static void moon(Game g) {
		g.broadcast("El robot los lleva a TODOS a ver la luna...");
		final int px = g.rx, pz = g.rz, py = 300;
		final int cx = px, cy = 308, cz = pz - 30, rad = 11;
		final Map<String, double[]> orig = new HashMap<>();
		for (String n : g.players) {
			ServerPlayer q = g.sp(n);
			if (q != null) orig.put(n, new double[]{q.getX(), q.getY(), q.getZ()});
		}
		Cmd.run("execute in minecraft:overworld run fill " + (px - 6) + " " + py + " " + (pz - 6) + " " + (px + 6) + " " + py + " " + (pz + 6) + " minecraft:glass");
		moonSphere(cx, cy, cz, rad, "minecraft:white_concrete");
		Cmd.run("time set midnight");
		int i = 0;
		for (String n : g.players) {
			if (g.sp(n) == null) continue;
			Cmd.run("execute in minecraft:overworld run tp " + n + " " + Cmd.f(px - 2 + (i % 3) * 2) + " " + (py + 1) + " " + Cmd.f(pz - 2 + (i / 3) * 2 + 0.5));
			Cmd.run("effect give " + n + " minecraft:slow_falling 40 0 true");
			Cmd.run("effect give " + n + " minecraft:jump_boost 40 4 true");
			Cmd.title(n, "A LA LUNA", "Disfruten la vista", "white");
			i++;
		}
		g.endingLoop = t -> {
			if (t % 40 == 0) soundAll(g, "minecraft:block.beacon.ambient", 1f, 1f);
			if (t >= 500) {
				for (Map.Entry<String, double[]> en : orig.entrySet()) {
					double[] o = en.getValue();
					Cmd.run("execute in minecraft:overworld run tp " + en.getKey() + " " + Cmd.f(o[0]) + " " + Cmd.f(o[1]) + " " + Cmd.f(o[2]));
				}
				Cmd.run("execute in minecraft:overworld run fill " + (px - 6) + " " + py + " " + (pz - 6) + " " + (px + 6) + " " + py + " " + (pz + 6) + " minecraft:air");
				moonSphere(cx, cy, cz, rad, "minecraft:air");
				Cmd.run("time set day");
				g.finishEnding(true);
			}
		};
	}

	static void moonSphere(int cx, int cy, int cz, int r, String block) {
		for (int dy = -r; dy <= r; dy++) {
			for (int dz = -r; dz <= r; dz++) {
				int r2 = r * r - dy * dy - dz * dz;
				if (r2 < 0) continue;
				int w = (int) Math.floor(Math.sqrt(r2));
				Cmd.run("execute in minecraft:overworld run fill " + (cx - w) + " " + (cy + dy) + " " + (cz + dz) + " "
						+ (cx + w) + " " + (cy + dy) + " " + (cz + dz) + " " + block);
			}
		}
	}

	// =====================================================================
	// Finales del saboteador
	// =====================================================================

	static void explode(Game g) {
		g.broadcast("El robot esta fuera de control... ¡VA A EXPLOTAR!");
		g.endingLoop = t -> {
			if (t % 40 == 0 && t < 200) {
				long left = 5 - t / 40;
				for (String n : g.players) Cmd.title(n, String.valueOf(left), "", "red");
				soundAll(g, "minecraft:block.note_block.pling", 1f, 2f);
			}
			if (t % 4 == 0 && t < 200) particles("minecraft:smoke", gx, gy + 8, gz, 3, 15);
			if (t == 200) {
				Cmd.run("kill " + ROBOT);
				for (int k = 0; k < 25; k++) {
					double ox = (g.rnd.nextDouble() - 0.5) * 14;
					double oy = g.rnd.nextDouble() * 16;
					double oz = (g.rnd.nextDouble() - 0.5) * 8;
					Cmd.run("execute in minecraft:overworld run summon minecraft:tnt " + Cmd.f(gx + ox) + " " + Cmd.f(gy + oy) + " " + Cmd.f(gz + oz) + " {fuse:20}");
				}
				soundAll(g, "minecraft:entity.generic.explode", 1f, 0.7f);
			}
			if (t >= 320) g.finishEnding(false);
		};
	}

	static void steal(Game g) {
		g.broadcast("El saboteador se llevo el robot... a cualquier lado.");
		g.endingLoop = t -> {
			gx += 0.45;
			place(gx, gy, gz, 270);
			if (t % 6 == 0) soundAll(g, "minecraft:entity.iron_golem.step", 1f, 0.5f);
			if (t % 10 == 0) particles("minecraft:cloud", gx, gy, gz, 2, 10);
			if (t >= 360) {
				Cmd.run("kill " + ROBOT);
				g.finishEnding(false);
			}
		};
	}

	static void fly(Game g) {
		g.broadcast("El saboteador y el robot se van volando...");
		final double sx, sy, sz;
		ServerPlayer s0 = g.sp(g.saboteur);
		if (s0 != null) {
			sx = s0.getX();
			sy = s0.getY();
			sz = s0.getZ();
		} else {
			sx = g.rx;
			sy = g.ry;
			sz = g.rz;
		}
		g.endingLoop = t -> {
			gy += 0.6;
			place(gx, gy, gz, (t * 6) % 360);
			ServerPlayer s = g.sp(g.saboteur);
			if (s != null) {
				Cmd.run("execute in minecraft:overworld run tp " + g.saboteur + " " + Cmd.f(gx) + " " + Cmd.f(gy + 11) + " " + Cmd.f(gz));
			}
			if (t % 3 == 0) particles("minecraft:firework", gx, gy, gz, 2, 12);
			if (t % 20 == 0) soundAll(g, "minecraft:entity.firework_rocket.launch", 1f, 1f);
			if (t >= 300) {
				Cmd.run("kill " + ROBOT);
				Cmd.run("effect give " + g.saboteur + " minecraft:slow_falling 40 0 true");
				Cmd.run("execute in minecraft:overworld run tp " + g.saboteur + " " + Cmd.f(sx) + " " + Cmd.f(sy) + " " + Cmd.f(sz));
				g.finishEnding(false);
			}
		};
	}

	static void slay(Game g) {
		g.broadcast("El saboteador controla el robot... ¡y viene por todos!");
		g.endingLoop = t -> {
			List<String> victims = new java.util.ArrayList<>();
			for (String n : g.players) {
				if (n.equals(g.saboteur) || g.ghosts.contains(n)) continue;
				ServerPlayer q = g.sp(n);
				if (q != null && q.isAlive()) victims.add(n);
			}
			if (victims.isEmpty() || t > 500) {
				g.finishEnding(false);
				return;
			}
			ServerPlayer target = g.sp(victims.get(0));
			double d = step(target.getX(), target.getY(), target.getZ(), 0.6);
			if (t % 6 == 0) soundAll(g, "minecraft:entity.iron_golem.step", 1f, 0.5f);
			if (d < 5) {
				String name = victims.get(0);
				Cmd.run("execute in minecraft:overworld run summon minecraft:lightning_bolt " + Cmd.f(target.getX()) + " " + Cmd.f(target.getY()) + " " + Cmd.f(target.getZ()));
				Cmd.run("kill " + name);
			}
		};
	}
}
