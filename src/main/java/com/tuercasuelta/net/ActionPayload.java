package com.tuercasuelta.net;

import com.tuercasuelta.TuercaSuelta;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Cliente -> servidor: una accion del jugador (botones de las pantallas). */
public record ActionPayload(String action, String arg) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<ActionPayload> TYPE =
			new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(TuercaSuelta.MOD_ID, "action"));

	public static final StreamCodec<RegistryFriendlyByteBuf, ActionPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, ActionPayload::action,
			ByteBufCodecs.STRING_UTF8, ActionPayload::arg,
			ActionPayload::new
	);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
