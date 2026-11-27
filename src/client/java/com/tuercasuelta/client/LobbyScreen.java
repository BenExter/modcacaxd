package com.tuercasuelta.client;

import com.tuercasuelta.net.ActionPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Menu para iniciar partida: eliges quien juega y le das a INICIAR. */
public class LobbyScreen extends Screen {
	private final List<String> names = new ArrayList<>();
	private final Set<String> selected = new LinkedHashSet<>();
	private final long start = System.currentTimeMillis();
	private int listLeft, listRight;

	public LobbyScreen(String data) {
		super(Component.empty());
		for (String n : data.split(",")) {
			if (!n.isBlank()) {
				names.add(n);
				selected.add(n);
			}
		}
	}

	private String label(String n) {
		return (selected.contains(n) ? "[X]  " : "[   ]  ") + n;
	}

	@Override
	protected void init() {
		int bw = 220, bh = 20, gap = 6;
		int x = (this.width - bw) / 2;
		int y = 96;
		listLeft = x;
		listRight = x + bw;
		for (String n : names) {
			Button b = Button.builder(Component.literal(label(n)), btn -> {
				if (selected.contains(n)) selected.remove(n);
				else selected.add(n);
				btn.setMessage(Component.literal(label(n)));
			}).bounds(x, y, bw, bh).build();
			this.addRenderableWidget(b);
			y += bh + gap;
		}
		y += 8;
		Button go = Button.builder(Component.literal(">>>  INICIAR PARTIDA  <<<"), btn -> {
			if (selected.isEmpty()) return;
			Minecraft.getInstance().gui.setScreen(null);
			ClientPlayNetworking.send(new ActionPayload("start", String.join(" ", selected)));
		}).bounds(x, y, bw, 24).build();
		this.addRenderableWidget(go);
		y += 30;
		Button close = Button.builder(Component.literal("Cerrar"), btn -> Minecraft.getInstance().gui.setScreen(null))
				.bounds(x, y, bw, bh).build();
		this.addRenderableWidget(close);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractRenderState(g, mouseX, mouseY, delta);
		float t = (System.currentTimeMillis() - start) / 1000f;

		// tuercas que caen a los lados
		for (int i = 0; i < 28; i++) {
			boolean left = i % 2 == 0;
			int span = left ? Math.max(10, listLeft - 30) : Math.max(10, this.width - listRight - 30);
			int baseX = left ? 10 : listRight + 20;
			int x = baseX + (i * 37) % span;
			float speed = 30 + (i % 7) * 14;
			int y = (int) ((t * speed + i * 53) % (this.height + 20)) - 10;
			int size = 3 + (i % 3) * 2;
			int shade = 120 + (i % 5) * 25;
			g.fill(x, y, x + size, y + size, Gfx.argb(200, shade, shade, shade + 20));
			g.fill(x + 1, y + 1, x + size - 1, y + size - 1, Gfx.argb(200, 30, 30, 40));
		}

		int bannerH = Math.min(78, (int) (t * 360));
		g.fill(0, 0, this.width, bannerH, 0xE0100810);
		g.fill(0, bannerH, this.width, bannerH + 2, 0xFF402010);
		if (bannerH >= 60) {
			int wob = (int) (3 * Math.sin(t * 2.5));
			int pulse = 215 + (int) (40 * Math.sin(t * 3.0));
			Gfx.center(g, this.font, "T U E R C A S U E L T A", this.width / 2, 18 + wob, Gfx.argb(255, pulse, 170, 30), true);
			Gfx.center(g, this.font, "Un robot. Un traidor. Cero confianza.", this.width / 2, 38, 0xFFCCCCCC, false);
			Gfx.center(g, this.font, "Elige quien juega (3 o 4 amigos)", this.width / 2, 56, 0xFF999999, false);
		}
		int sx = (int) ((t * 200) % (this.width + 160)) - 160;
		g.fill(sx, bannerH, sx + 160, bannerH + 2, 0xFFFFAA00);
	}
}
