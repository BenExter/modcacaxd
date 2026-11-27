package com.tuercasuelta.game;

import java.util.List;

/** Objetos que pueden salir en el bingo-draft. */
final class Items {
	private Items() {}

	record Item(String id, String name) {}

	static final List<Item> POOL = List.of(
			new Item("minecraft:iron_ingot", "Lingote de hierro"),
			new Item("minecraft:copper_ingot", "Lingote de cobre"),
			new Item("minecraft:coal", "Carbon"),
			new Item("minecraft:redstone", "Polvo de redstone"),
			new Item("minecraft:gold_ingot", "Lingote de oro"),
			new Item("minecraft:lapis_lazuli", "Lapislazuli"),
			new Item("minecraft:string", "Hilo"),
			new Item("minecraft:bone", "Hueso"),
			new Item("minecraft:gunpowder", "Polvora"),
			new Item("minecraft:spider_eye", "Ojo de arana"),
			new Item("minecraft:rotten_flesh", "Carne podrida"),
			new Item("minecraft:slime_ball", "Bola de slime"),
			new Item("minecraft:ender_pearl", "Perla de ender"),
			new Item("minecraft:glowstone_dust", "Polvo de piedra luminosa"),
			new Item("minecraft:quartz", "Cuarzo del Nether"),
			new Item("minecraft:diamond", "Diamante"),
			new Item("minecraft:honeycomb", "Panal"),
			new Item("minecraft:amethyst_shard", "Fragmento de amatista"),
			new Item("minecraft:ink_sac", "Saco de tinta"),
			new Item("minecraft:arrow", "Flecha"),
			new Item("minecraft:feather", "Pluma"),
			new Item("minecraft:leather", "Cuero"),
			new Item("minecraft:clay_ball", "Bola de arcilla"),
			new Item("minecraft:sugar_cane", "Cana de azucar"),
			new Item("minecraft:cactus", "Cactus"),
			new Item("minecraft:pumpkin", "Calabaza"),
			new Item("minecraft:melon_slice", "Rodaja de sandia"),
			new Item("minecraft:sweet_berries", "Bayas dulces"),
			new Item("minecraft:cocoa_beans", "Granos de cacao"),
			new Item("minecraft:flint", "Pedernal"),
			new Item("minecraft:glass_bottle", "Botella de cristal"),
			new Item("minecraft:bucket", "Cubeta")
	);

	static final String[] MOBS = {
			"zombie", "skeleton", "spider", "creeper", "slime", "witch",
			"chicken", "pig", "cow", "sheep", "bee", "silverfish", "cave_spider", "husk"
	};
}
