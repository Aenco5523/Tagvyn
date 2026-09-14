package dev.aenco.tagvyn.network;

import dev.aenco.tagvyn.Tagvyn;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record AdminDashboardActionPayload(String action) implements CustomPacketPayload {
    public static final Type<AdminDashboardActionPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Tagvyn.MOD_ID, "admin_dashboard_action")
    );

    public AdminDashboardActionPayload {
        action = action == null ? "" : action;
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, AdminDashboardActionPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeUtf(payload.action(), 32),
            buf -> new AdminDashboardActionPayload(buf.readUtf(32))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
