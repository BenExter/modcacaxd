package com.tuercasuelta.net;

import com.tuercasuelta.TuercaSuelta;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Servidor -> cliente: abre una pantalla. kind = lobby | panel | role | skull */
public record UiPayload(String kind, String data) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<UiPayload> TYPE =
			new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(TuercaSuelta.MOD_ID, "ui"));

	public static final StreamCodec<RegistryFriendlyByteBuf, UiPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, UiPayload::kind,
			ByteBufCodecs.STRING_UTF8, UiPayload::data,
			UiPayload::new
	);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
