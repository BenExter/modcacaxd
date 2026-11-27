package com.tuercasuelta;

import com.tuercasuelta.game.Game;
import com.tuercasuelta.game.TuercaCommands;
import com.tuercasuelta.net.ActionPayload;
import com.tuercasuelta.net.UiPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TuercaSuelta implements ModInitializer {
	public static final String MOD_ID = "tuercasuelta";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		PayloadTypeRegistry.clientboundPlay().register(UiPayload.TYPE, UiPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(ActionPayload.TYPE, ActionPayload.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(ActionPayload.TYPE, (payload, context) ->
				Game.I.handleAction(context.player(), payload.action(), payload.arg()));

		ServerTickEvents.END_SERVER_TICK.register(Game.I::tick);

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				TuercaCommands.register(dispatcher));

		LOGGER.info("Tuercasuelta cargado: hay una tuerca suelta en algun lado...");
	}
}
