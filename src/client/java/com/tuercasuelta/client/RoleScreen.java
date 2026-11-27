package com.tuercasuelta.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Revelacion dramatica del rol. Se cierra sola a los 6 segundos. */
public class RoleScreen extends Screen {
	private final boolean saboteur;
	private final long start = System.currentTimeMillis();

	public RoleScreen(boolean saboteur) {
		super(Component.empty());
		this.saboteur = saboteur;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractRenderState(g, mouseX, mouseY, delta);
		float t = (System.currentTimeMillis() - start) / 1000f;

		int dim = (int) (Gfx.clamp01(t / 0.8f) * 240);
		g.fill(0, 0, this.width, this.height, Gfx.argb(dim, 0, 0, 0));

		int cx = this.width / 2, cy = this.height / 2;

		if (t < 1.4f) {
			int a = (int) (255 * Gfx.clamp01(t / 0.7f));
			Gfx.center(g, this.font, "Tu rol es...", cx, cy - 4, Gfx.argb(a, 200, 200, 200), true);
		} else {
			float k = Gfx.clamp01((t - 1.4f) / 0.6f);
			int a = (int) (255 * k);
			if (saboteur) {
				// parpadeo rojo y lineas de escaneo
				int flick = (Math.sin(t * 40) > 0.6) ? 255 : 190;
				g.fill(0, 0, this.width, this.height, Gfx.argb((int) (60 * k), 120, 0, 0));
				for (int y = 0; y < this.height; y += 6) {
					g.fill(0, y, this.width, y + 1, Gfx.argb(40, 0, 0, 0));
				}
				Gfx.center(g, this.font, "ERES EL SABOTEADOR", cx, cy - 14, Gfx.argb(a, flick, 20, 20), true);
				Gfx.center(g, this.font, "Desarma el robot sin que te descubran.", cx, cy + 4, Gfx.argb(a, 230, 200, 200), false);
				Gfx.center(g, this.font, "Nadie debe saberlo.", cx, cy + 18, Gfx.argb(a, 160, 160, 160), false);
			} else {
				int glow = 190 + (int) (60 * Math.sin(t * 4));
				g.fill(0, 0, this.width, this.height, Gfx.argb((int) (40 * k), 0, 80, 0));
				Gfx.center(g, this.font, "ERES DEL EQUIPO", cx, cy - 14, Gfx.argb(a, 40, glow, 60), true);
				Gfx.center(g, this.font, "Construyan el robot entre todos.", cx, cy + 4, Gfx.argb(a, 200, 230, 200), false);
				Gfx.center(g, this.font, "Uno de ustedes es un traidor.", cx, cy + 18, Gfx.argb(a, 160, 160, 160), false);
			}
		}

		if (t > 6f) {
			Minecraft mc = Minecraft.getInstance();
			mc.execute(() -> mc.gui.setScreen(null));
		}
	}
}
