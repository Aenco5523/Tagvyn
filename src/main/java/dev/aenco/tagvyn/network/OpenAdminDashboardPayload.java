package dev.aenco.tagvyn.network;

import dev.aenco.tagvyn.Tagvyn;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record OpenAdminDashboardPayload() implements CustomPacketPayload {
    public static final Type<OpenAdminDashboardPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Tagvyn.MOD_ID, "open_admin_dashboard")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenAdminDashboardPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {},
            buf -> new OpenAdminDashboardPayload()
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
