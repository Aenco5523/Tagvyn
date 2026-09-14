package dev.aenco.tagvyn.api;

import java.util.Collection;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/** Stable public integration surface for Tagvyn. */
public interface TagvynApi {
    IdentitySnapshot getIdentity(Player player);

    default Optional<String> getNickname(Player player) {
        String nickname = getIdentity(player).nickname();
        return nickname.isBlank() ? Optional.empty() : Optional.of(nickname);
    }

    default String getEffectiveName(Player player) {
        return getNickname(player).orElseGet(() -> player.getGameProfile().getName());
    }

    int getRemainingNicknameChanges(Player player);

    NicknameUpdateResult setNickname(ServerPlayer player, String nickname, boolean bypassLimit);

    NicknameUpdateResult clearNickname(ServerPlayer player, boolean bypassLimit);

    boolean setTitle(ServerPlayer player, String titleId);

    void clearTitle(ServerPlayer player);

    Optional<TagvynTitle> getTitle(String id);

    Collection<TagvynTitle> getTitles();

    /** Registers a text title or an advanced externally-provided font/glyph title. */
    boolean registerTitle(TagvynTitle title, boolean overwrite);

    /**
     * Registers a normal PNG image as a title. Tagvyn stores and distributes the PNG and creates
     * the required client rendering resources automatically.
     */
    boolean registerImageTitle(String id, String text, int color, byte[] png, boolean overwrite);

    /** Returns the PNG backing an uploaded image title, when one exists. */
    Optional<byte[]> getTitleImage(String id);

    boolean unregisterTitle(String id);

    /** Refreshes online title snapshots and synchronizes uploaded image assets to clients. */
    void refreshTitles(MinecraftServer server);

    Component formatDisplayName(Player player, Component fallbackName, boolean includeTitle);
}
