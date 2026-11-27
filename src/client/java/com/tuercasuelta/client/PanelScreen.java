package com.tuercasuelta.client;

import com.tuercasuelta.net.ActionPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Panel generico que arma el servidor: linea 1 = titulo, linea 2 = subtitulo,
 * resto = botones con formato "texto|accion|argumento".
 */
public class PanelScreen extends Screen {
	private final String title;
	private final String sub;
	private final List<String[]> entries = new ArrayList<>();
	private final long start = System.currentTimeMillis();

	public PanelScreen(String data) {
		super(Component.empty());
		String[] lines = data.split("\n");
		this.title = lines.length > 0 ? lines[0] : "";
		this.sub = lines.length > 1 ? lines[1] : "";
		for (int i = 2; i < lines.length; i++) {
			String[] parts = lines[i].split("\\|", -1);
			if (parts.length >= 2) entries.add(parts);
		}
	}

	@Override
	protected void init() {
		int cols = entries.size() > 7 ? 2 : 1;
		int bw = 230, bh = 20, gap = 6, colGap = 10;
		int rows = (int) Math.ceil(entries.size() / (double) cols);
		int totalW = cols * bw + (cols - 1) * colGap;
		int x0 = (this.width - totalW) / 2;
		int y0 = Math.max(74, (this.height - rows * (bh + gap)) / 2 + 20);
		for (int i = 0; i < entries.size(); i++) {
			final String[] en = entries.get(i);
			int col = i / rows;
			int row = i % rows;
			Button b = Button.builder(Component.literal(en[0]), btn -> click(en))
					.bounds(x0 + col * (bw + colGap), y0 + row * (bh + gap), bw, bh)
					.build();
			this.addRenderableWidget(b);
		}
	}

	private void click(String[] en) {
		String action = en[1];
		String arg = en.length > 2 ? en[2] : "";
		Minecraft.getInstance().gui.setScreen(null);
		if (!action.equals("close") && !action.isEmpty()) {
			ClientPlayNetworking.send(new ActionPayload(action, arg));
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractRenderState(g, mouseX, mouseY, delta);
		float t = (System.currentTimeMillis() - start) / 1000f;

		int bannerH = Math.min(60, (int) (t * 300));
		g.fill(0, 0, this.width, bannerH, 0xE0100810);
		g.fill(0, bannerH, this.width, bannerH + 2, 0xFF402010);

		if (bannerH >= 40) {
			int pulse = 215 + (int) (40 * Math.sin(t * 3.0));
			Gfx.center(g, this.font, title, this.width / 2, 16, Gfx.argb(255, pulse, 170, 30), true);
			Gfx.center(g, this.font, sub, this.width / 2, 34, 0xFFCCCCCC, false);
		}

		// barra de luz que recorre el borde del banner
		int sx = (int) ((t * 160) % (this.width + 120)) - 120;
		g.fill(sx, bannerH, sx + 120, bannerH + 2, 0xFFFFAA00);
	}
}
