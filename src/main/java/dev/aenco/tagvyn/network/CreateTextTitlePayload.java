package dev.aenco.tagvyn.network;

import dev.aenco.tagvyn.Tagvyn;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record CreateTextTitlePayload(String id, String text, int color) implements CustomPacketPayload {
    public static final Type<CreateTextTitlePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Tagvyn.MOD_ID, "create_text_title")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, CreateTextTitlePayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeUtf(payload.id(), 64);
                buf.writeUtf(payload.text(), 128);
                buf.writeInt(payload.color());
            },
            buf -> new CreateTextTitlePayload(buf.readUtf(64), buf.readUtf(128), buf.readInt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
