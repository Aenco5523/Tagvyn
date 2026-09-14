package dev.aenco.tagvyn.network;

import dev.aenco.tagvyn.Tagvyn;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record OpenNicknameScreenPayload(
        String currentNickname,
        int remainingChanges,
        int minLength,
        int maxLength,
        boolean allowSpaces
) implements CustomPacketPayload {
    public static final Type<OpenNicknameScreenPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Tagvyn.MOD_ID, "open_nickname_screen")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenNicknameScreenPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeUtf(payload.currentNickname(), 64);
                buf.writeVarInt(payload.remainingChanges());
                buf.writeVarInt(payload.minLength());
                buf.writeVarInt(payload.maxLength());
                buf.writeBoolean(payload.allowSpaces());
            },
            buf -> new OpenNicknameScreenPayload(
                    buf.readUtf(64),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readBoolean()
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
