package dev.aenco.tagvyn.network;

import dev.aenco.tagvyn.Tagvyn;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record AdminPlayerActionPayload(String action, String playerName, String value) implements CustomPacketPayload {
    public static final Type<AdminPlayerActionPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Tagvyn.MOD_ID, "admin_player_action")
    );

    public AdminPlayerActionPayload {
        action = action == null ? "" : action;
        playerName = playerName == null ? "" : playerName;
        value = value == null ? "" : value;
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, AdminPlayerActionPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeUtf(payload.action(), 32);
                buf.writeUtf(payload.playerName(), 32);
                buf.writeUtf(payload.value(), 128);
            },
            buf -> new AdminPlayerActionPayload(
                    buf.readUtf(32),
                    buf.readUtf(32),
                    buf.readUtf(128)
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
