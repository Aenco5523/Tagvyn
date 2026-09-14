package dev.aenco.tagvyn.service;

import dev.aenco.tagvyn.config.TagvynConfig;
import dev.aenco.tagvyn.data.IdentityData;
import dev.aenco.tagvyn.data.TagvynAttachments;
import dev.aenco.tagvyn.title.TitleDefinition;
import dev.aenco.tagvyn.title.TitleRegistry;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class TagvynService {
    private TagvynService() {}

    public enum NicknameFailure {
        NONE,
        INVALID,
        LIMIT_REACHED
    }

    public record NicknameResult(boolean success, NicknameFailure failure, String nickname, IdentityData data) {}

    public static NicknameResult setNickname(ServerPlayer player, String rawNickname, boolean bypassLimit) {
        String nickname = normalizeNickname(rawNickname);
        if (!isValidNickname(nickname)) {
            return new NicknameResult(false, NicknameFailure.INVALID, nickname, player.getData(TagvynAttachments.IDENTITY));
        }

        IdentityData current = player.getData(TagvynAttachments.IDENTITY);
        if (!bypassLimit && isAtLimit(current)) {
            return new NicknameResult(false, NicknameFailure.LIMIT_REACHED, nickname, current);
        }

        boolean count = !bypassLimit && !nickname.equals(current.nickname());
        IdentityData updated = current.withNickname(nickname, count);
        apply(player, updated);
        return new NicknameResult(true, NicknameFailure.NONE, nickname, updated);
    }

    public static NicknameResult clearNickname(ServerPlayer player, boolean bypassLimit) {
        IdentityData current = player.getData(TagvynAttachments.IDENTITY);
        if (!current.hasNickname()) {
            return new NicknameResult(true, NicknameFailure.NONE, "", current);
        }
        if (!bypassLimit && isAtLimit(current)) {
            return new NicknameResult(false, NicknameFailure.LIMIT_REACHED, "", current);
        }

        IdentityData updated = current.withNickname("", !bypassLimit);
        apply(player, updated);
        return new NicknameResult(true, NicknameFailure.NONE, "", updated);
    }

    public static void resetNicknameChanges(ServerPlayer player) {
        apply(player, player.getData(TagvynAttachments.IDENTITY).withNicknameChanges(0));
    }

    public static boolean setTitle(ServerPlayer player, String titleId) {
        Optional<TitleDefinition> definition = TitleRegistry.get(titleId);
        if (definition.isEmpty()) return false;

        TitleDefinition title = definition.get();
        IdentityData current = player.getData(TagvynAttachments.IDENTITY);
        apply(player, current.withTitle(
                title.id(),
                title.text(),
                title.color(),
                title.imageFont(),
                title.imageGlyph()
        ));
        return true;
    }

    public static void clearTitle(ServerPlayer player) {
        apply(player, player.getData(TagvynAttachments.IDENTITY).clearTitle());
    }

    public static void reloadTitles(MinecraftServer server) {
        TitleRegistry.load();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            refreshTitleSnapshot(player);
        }
    }

    public static void refreshTitleSnapshot(ServerPlayer player) {
        IdentityData current = player.getData(TagvynAttachments.IDENTITY);
        if (current.titleId().isBlank()) {
            refresh(player);
            return;
        }

        Optional<TitleDefinition> definition = TitleRegistry.get(current.titleId());
        if (definition.isEmpty()) {
            apply(player, current.clearTitle());
            return;
        }

        TitleDefinition title = definition.get();
        apply(player, current.withTitle(
                title.id(),
                title.text(),
                title.color(),
                title.imageFont(),
                title.imageGlyph()
        ));
    }

    public static void refresh(ServerPlayer player) {
        player.refreshDisplayName();
        player.refreshTabListName();
        player.syncData(TagvynAttachments.IDENTITY.get());
    }

    public static int remainingChanges(IdentityData data) {
        int limit = TagvynConfig.VALUES.nicknameChangeLimit.get();
        if (limit < 0) return -1;
        return Math.max(0, limit - data.nicknameChanges());
    }

    private static void apply(ServerPlayer player, IdentityData data) {
        player.setData(TagvynAttachments.IDENTITY, data);
        refresh(player);
    }

    private static boolean isAtLimit(IdentityData data) {
        int limit = TagvynConfig.VALUES.nicknameChangeLimit.get();
        return limit >= 0 && data.nicknameChanges() >= limit;
    }

    private static String normalizeNickname(String raw) {
        return raw == null ? "" : raw.trim().replaceAll(" +", " ");
    }

    private static boolean isValidNickname(String nickname) {
        int min = Math.min(TagvynConfig.VALUES.nicknameMinLength.get(), TagvynConfig.VALUES.nicknameMaxLength.get());
        int max = Math.max(TagvynConfig.VALUES.nicknameMinLength.get(), TagvynConfig.VALUES.nicknameMaxLength.get());
        int length = nickname.codePointCount(0, nickname.length());
        if (length < min || length > max) return false;
        if (!TagvynConfig.VALUES.allowSpaces.get() && nickname.codePoints().anyMatch(Character::isWhitespace)) return false;
        if (nickname.indexOf('§') >= 0) return false;
        return nickname.codePoints().noneMatch(Character::isISOControl);
    }
}
