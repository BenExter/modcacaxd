package com.tuercasuelta.client;

import com.tuercasuelta.TuercaSuelta;
import com.tuercasuelta.net.UiPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

public class TuercaSueltaClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(UiPayload.TYPE, (payload, context) -> {
			Minecraft mc = context.client();
			switch (payload.kind()) {
				case "lobby" -> mc.gui.setScreen(new LobbyScreen(payload.data()));
				case "panel" -> mc.gui.setScreen(new PanelScreen(payload.data()));
				case "role" -> mc.gui.setScreen(new RoleScreen("saboteur".equals(payload.data())));
				case "skull" -> mc.gui.setScreen(new SkullScreen());
				default -> { }
			}
		});
		TuercaSuelta.LOGGER.info("Tuercasuelta (cliente) listo.");
	}
}
