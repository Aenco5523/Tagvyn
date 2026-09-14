package dev.aenco.tagvyn.network;

import dev.aenco.tagvyn.Tagvyn;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record TitleImageDataPayload(String id, byte[] png) implements CustomPacketPayload {
    public static final int MAX_PACKET_BYTES = 512 * 1024;
    public static final Type<TitleImageDataPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Tagvyn.MOD_ID, "title_image_data")
    );

    public TitleImageDataPayload {
        png = png.clone();
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, TitleImageDataPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeUtf(payload.id(), 64);
                buf.writeByteArray(payload.png());
            },
            buf -> new TitleImageDataPayload(buf.readUtf(64), buf.readByteArray(MAX_PACKET_BYTES))
    );

    @Override
    public byte[] png() {
        return png.clone();
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
