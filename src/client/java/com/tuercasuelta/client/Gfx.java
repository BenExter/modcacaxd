package com.tuercasuelta.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** Ayudas de dibujo para las pantallas del mod. Los colores son ARGB (0xAARRGGBB). */
final class Gfx {
	private Gfx() {}

	static void center(GuiGraphicsExtractor g, Font font, String s, int cx, int y, int color, boolean bold) {
		Component c = bold ? Component.literal(s).withStyle(ChatFormatting.BOLD) : Component.literal(s);
		int w = font.width(c);
		g.text(font, c, cx - w / 2, y, color, true);
	}

	static int argb(int alpha, int r, int g, int b) {
		int a = Math.max(0, Math.min(255, alpha));
		return (a << 24) | ((r & 255) << 16) | ((g & 255) << 8) | (b & 255);
	}

	static float clamp01(float v) {
		return Math.max(0f, Math.min(1f, v));
	}
}
