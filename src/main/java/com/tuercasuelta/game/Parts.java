package com.tuercasuelta.game;

/** El robot gigante de bloques: 9 piezas, una por cada casilla de la cartilla. */
final class Parts {
	private Parts() {}

	record Box(int x1, int y1, int z1, int x2, int y2, int z2, String block) {}

	static final Box[][] PARTS = {
			// 0 pie izquierdo
			{new Box(-3, 0, 0, -2, 1, 2, "minecraft:iron_block")},
			// 1 pie derecho
			{new Box(2, 0, 0, 3, 1, 2, "minecraft:iron_block")},
			// 2 piernas
			{new Box(-3, 2, 0, -2, 5, 2, "minecraft:copper_block"),
					new Box(2, 2, 0, 3, 5, 2, "minecraft:copper_block")},
			// 3 cintura
			{new Box(-3, 6, 0, 3, 7, 2, "minecraft:polished_andesite")},
			// 4 torso + nucleo
			{new Box(-3, 8, 0, 3, 12, 2, "minecraft:light_gray_concrete"),
					new Box(0, 10, 0, 0, 10, 0, "minecraft:sea_lantern")},
			// 5 brazo izquierdo
			{new Box(-5, 7, 1, -4, 12, 1, "minecraft:copper_block"),
					new Box(-5, 6, 1, -4, 6, 1, "minecraft:gold_block")},
			// 6 brazo derecho
			{new Box(4, 7, 1, 5, 12, 1, "minecraft:copper_block"),
					new Box(4, 6, 1, 5, 6, 1, "minecraft:gold_block")},
			// 7 cabeza
			{new Box(-2, 13, 0, 2, 16, 2, "minecraft:iron_block"),
					new Box(-1, 15, 0, -1, 15, 0, "minecraft:sea_lantern"),
					new Box(1, 15, 0, 1, 15, 0, "minecraft:sea_lantern"),
					new Box(-1, 14, 0, 1, 14, 0, "minecraft:black_concrete")},
			// 8 antena
			{new Box(0, 17, 1, 0, 19, 1, "minecraft:lightning_rod"),
					new Box(0, 20, 1, 0, 20, 1, "minecraft:redstone_block")}
	};

	static final String[] NAMES = {
			"pie izquierdo", "pie derecho", "piernas", "cintura", "torso",
			"brazo izquierdo", "brazo derecho", "cabeza", "antena"
	};

	static void place(Game g, int part, boolean on) {
		for (Box b : PARTS[part]) {
			Cmd.run("execute in minecraft:overworld run fill "
					+ (g.rx + b.x1()) + " " + (g.ry + b.y1()) + " " + (g.rz + b.z1()) + " "
					+ (g.rx + b.x2()) + " " + (g.ry + b.y2()) + " " + (g.rz + b.z2()) + " "
					+ (on ? b.block() : "minecraft:air"));
		}
	}

	static void clearAll(Game g) {
		for (int i = 0; i < PARTS.length; i++) place(g, i, false);
	}

	/** Plataforma de piedra y espacio libre para el robot. */
	static void arena(Game g) {
		Cmd.run("execute in minecraft:overworld run fill " + (g.rx - 9) + " " + g.ry + " " + (g.rz - 3) + " "
				+ (g.rx + 9) + " " + (g.ry + 26) + " " + (g.rz + 6) + " minecraft:air");
		Cmd.run("execute in minecraft:overworld run fill " + (g.rx - 9) + " " + (g.ry - 1) + " " + (g.rz - 3) + " "
				+ (g.rx + 9) + " " + (g.ry - 1) + " " + (g.rz + 6) + " minecraft:stone_bricks");
	}
}
