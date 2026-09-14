package dev.aenco.tagvyn.network;

import dev.aenco.tagvyn.Tagvyn;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record GiveTitleItemPayload(String titleId) implements CustomPacketPayload {
    public static final Type<GiveTitleItemPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Tagvyn.MOD_ID, "give_title_item")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, GiveTitleItemPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeUtf(payload.titleId(), 64),
            buf -> new GiveTitleItemPayload(buf.readUtf(64))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
