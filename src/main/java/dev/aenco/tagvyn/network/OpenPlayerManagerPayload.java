package dev.aenco.tagvyn.network;

import dev.aenco.tagvyn.Tagvyn;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record OpenPlayerManagerPayload(
        List<PlayerSummary> players,
        List<String> titles,
        String selectedUsername
) implements CustomPacketPayload {
    public static final Type<OpenPlayerManagerPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Tagvyn.MOD_ID, "open_player_manager")
    );

    public OpenPlayerManagerPayload {
        players = List.copyOf(players);
        titles = List.copyOf(titles);
        selectedUsername = selectedUsername == null ? "" : selectedUsername;
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenPlayerManagerPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.players().size());
                for (PlayerSummary player : payload.players()) {
                    buf.writeUtf(player.username(), 32);
                    buf.writeUtf(player.nickname(), 64);
                    buf.writeUtf(player.titleId(), 64);
                    buf.writeVarInt(player.nicknameChanges());
                    buf.writeVarInt(player.remainingChanges());
                }
                buf.writeVarInt(payload.titles().size());
                for (String title : payload.titles()) {
                    buf.writeUtf(title, 64);
                }
                buf.writeUtf(payload.selectedUsername(), 32);
            },
            buf -> {
                int playerCount = buf.readVarInt();
                if (playerCount < 0 || playerCount > 1024) throw new IllegalArgumentException("Too many Tagvyn players");
                List<PlayerSummary> players = new ArrayList<>(playerCount);
                for (int i = 0; i < playerCount; i++) {
                    players.add(new PlayerSummary(
                            buf.readUtf(32),
                            buf.readUtf(64),
                            buf.readUtf(64),
                            buf.readVarInt(),
                            buf.readVarInt()
                    ));
                }
                int titleCount = buf.readVarInt();
                if (titleCount < 0 || titleCount > 512) throw new IllegalArgumentException("Too many Tagvyn titles");
                List<String> titles = new ArrayList<>(titleCount);
                for (int i = 0; i < titleCount; i++) {
                    titles.add(buf.readUtf(64));
                }
                return new OpenPlayerManagerPayload(players, titles, buf.readUtf(32));
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record PlayerSummary(
            String username,
            String nickname,
            String titleId,
            int nicknameChanges,
            int remainingChanges
    ) {}
}
