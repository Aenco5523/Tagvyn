package dev.aenco.tagvyn.network;

import dev.aenco.tagvyn.Tagvyn;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record DeleteTitlePayload(String id) implements CustomPacketPayload {
    public static final Type<DeleteTitlePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Tagvyn.MOD_ID, "delete_title")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, DeleteTitlePayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeUtf(payload.id(), 64),
            buf -> new DeleteTitlePayload(buf.readUtf(64))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
