package dev.aenco.tagvyn.network;

import dev.aenco.tagvyn.Tagvyn;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record UploadImageTitlePayload(String id, String text, int color, byte[] png) implements CustomPacketPayload {
    public static final int MAX_PACKET_BYTES = 512 * 1024;
    public static final Type<UploadImageTitlePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Tagvyn.MOD_ID, "upload_image_title")
    );

    public UploadImageTitlePayload {
        png = png.clone();
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, UploadImageTitlePayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeUtf(payload.id(), 64);
                buf.writeUtf(payload.text(), 128);
                buf.writeInt(payload.color());
                buf.writeByteArray(payload.png());
            },
            buf -> new UploadImageTitlePayload(
                    buf.readUtf(64),
                    buf.readUtf(128),
                    buf.readInt(),
                    buf.readByteArray(MAX_PACKET_BYTES)
            )
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
