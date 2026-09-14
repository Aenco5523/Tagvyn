package dev.aenco.tagvyn.network;

import dev.aenco.tagvyn.Tagvyn;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record TitleImageSyncCompletePayload() implements CustomPacketPayload {
    public static final Type<TitleImageSyncCompletePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Tagvyn.MOD_ID, "title_image_sync_complete")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, TitleImageSyncCompletePayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {},
            buf -> new TitleImageSyncCompletePayload()
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
