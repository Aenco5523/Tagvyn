package dev.aenco.tagvyn.api;

import java.util.Collection;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Stable public integration surface for Tagvyn.
 *
 * <p>The interface deliberately depends only on Minecraft/Java types so the same API package can be
 * retained when Tagvyn is ported to Fabric, Forge, or newer Minecraft versions.</p>
 */
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

    /**
     * Registers a title and persists it to Tagvyn's title configuration.
     *
     * @param title title definition
     * @param overwrite whether an existing title with the same id may be replaced
     * @return true when the title was stored
     */
    boolean registerTitle(TagvynTitle title, boolean overwrite);

    /** Removes a title from the registry and persisted title configuration. */
    boolean unregisterTitle(String id);

    /** Refreshes currently-online players after title definitions were changed through the API. */
    void refreshTitles(MinecraftServer server);

    Component formatDisplayName(Player player, Component fallbackName, boolean includeTitle);
}
