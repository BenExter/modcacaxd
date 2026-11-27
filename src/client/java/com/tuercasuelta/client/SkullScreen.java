package com.tuercasuelta.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** La calavera epica del poder del saboteador. Dura unos 4 segundos. */
public class SkullScreen extends Screen {
	private static final String[] SKULL = {
			"....########....",
			"..############..",
			".##############.",
			".##############.",
			".###ee####ee###.",
			".##eeee##eeee##.",
			".##eeee##eeee##.",
			".###ee#nn#ee###.",
			".######nn######.",
			"..############..",
			"...##########...",
			"...#t#t##t#t#...",
			"...#t#t##t#t#...",
			"....########...."
	};

	private final long start = System.currentTimeMillis();

	public SkullScreen() {
		super(Component.empty());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractRenderState(g, mouseX, mouseY, delta);
		float t = (System.currentTimeMillis() - start) / 1000f;

		// fondo oscuro rojizo
		int dim = (int) (Gfx.clamp01(t / 0.25f) * 215);
		g.fill(0, 0, this.width, this.height, Gfx.argb(dim, 25, 0, 0));

		// destello inicial
		if (t < 0.35f) {
			g.fill(0, 0, this.width, this.height, Gfx.argb((int) (255 * (1f - t / 0.35f)), 255, 255, 255));
		}

		// meteoritos cayendo
		for (int i = 0; i < 34; i++) {
			int x = (i * 97) % Math.max(1, this.width);
			float speed = 380 + (i % 5) * 70;
			int y = (int) ((t * speed + i * 61) % (this.height + 80)) - 60;
			g.fill(x, y, x + 5, y + 22, Gfx.argb(210, 255, 120, 20));
			g.fill(x + 1, y + 14, x + 4, y + 30, Gfx.argb(150, 255, 60, 0));
			g.fill(x, y + 18, x + 5, y + 24, Gfx.argb(255, 255, 230, 120));
		}

		// la calavera (pixel art)
		int rows = SKULL.length;
		int cols = SKULL[0].length();
		int px = Math.max(4, Math.min(this.width / (cols + 6), this.height / (rows + 10)));
		float grow = Gfx.clamp01(t / 0.5f);
		int size = Math.max(1, (int) (px * (0.4f + 0.6f * grow)));
		int shakeX = (int) (4 * Math.sin(t * 55) * (1f - Gfx.clamp01((t - 1.6f) / 1.5f)));
		int shakeY = (int) (3 * Math.cos(t * 47) * (1f - Gfx.clamp01((t - 1.6f) / 1.5f)));
		int x0 = (this.width - cols * size) / 2 + shakeX;
		int y0 = (this.height - rows * size) / 2 - 12 + shakeY;

		int eye = (Math.sin(t * 14) > 0) ? 0xFFFF2A2A : 0xFFB01010;
		int alpha = (int) (255 * Gfx.clamp01(t / 0.3f));
		for (int r = 0; r < rows; r++) {
			for (int c = 0; c < cols; c++) {
				char ch = SKULL[r].charAt(c);
				int color = 0;
				if (ch == '#') color = Gfx.argb(alpha, 236, 236, 230);
				else if (ch == 'e') color = (alpha << 24) | (eye & 0xFFFFFF);
				else if (ch == 'n') color = Gfx.argb(alpha, 30, 30, 30);
				else if (ch == 't') color = Gfx.argb(alpha, 70, 70, 70);
				if (color == 0) continue;
				g.fill(x0 + c * size, y0 + r * size, x0 + (c + 1) * size, y0 + (r + 1) * size, color);
			}
		}

		if (t > 0.6f) {
			int a = (int) (255 * Gfx.clamp01((t - 0.6f) / 0.4f));
			int pulse = 200 + (int) (55 * Math.sin(t * 12));
			Gfx.center(g, this.font, "¡LLUEVEN METEORITOS!", this.width / 2, y0 + rows * size + 14, Gfx.argb(a, pulse, 40, 20), true);
		}

		if (t > 4f) {
			Minecraft mc = Minecraft.getInstance();
			mc.execute(() -> mc.gui.setScreen(null));
		}
	}
}
