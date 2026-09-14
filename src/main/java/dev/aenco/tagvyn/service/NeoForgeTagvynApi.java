package dev.aenco.tagvyn.service;

import dev.aenco.tagvyn.api.IdentitySnapshot;
import dev.aenco.tagvyn.api.NicknameUpdateResult;
import dev.aenco.tagvyn.api.TagvynApi;
import dev.aenco.tagvyn.api.TagvynTitle;
import dev.aenco.tagvyn.data.IdentityData;
import dev.aenco.tagvyn.data.TagvynAttachments;
import dev.aenco.tagvyn.display.TagvynDisplay;
import dev.aenco.tagvyn.network.TagvynNetwork;
import dev.aenco.tagvyn.title.TitleDefinition;
import dev.aenco.tagvyn.title.TitleImageStore;
import dev.aenco.tagvyn.title.TitleRegistry;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class NeoForgeTagvynApi implements TagvynApi {
    public static final NeoForgeTagvynApi INSTANCE = new NeoForgeTagvynApi();

    private NeoForgeTagvynApi() {}

    @Override
    public IdentitySnapshot getIdentity(Player player) {
        IdentityData data = player.getData(TagvynAttachments.IDENTITY);
        return new IdentitySnapshot(
                data.nickname(),
                data.nicknameChanges(),
                data.titleId(),
                data.titleText(),
                data.titleColor(),
                data.imageFont(),
                data.imageGlyph()
        );
    }

    @Override
    public int getRemainingNicknameChanges(Player player) {
        return TagvynService.remainingChanges(player.getData(TagvynAttachments.IDENTITY));
    }

    @Override
    public NicknameUpdateResult setNickname(ServerPlayer player, String nickname, boolean bypassLimit) {
        return map(TagvynService.setNickname(player, nickname, bypassLimit));
    }

    @Override
    public NicknameUpdateResult clearNickname(ServerPlayer player, boolean bypassLimit) {
        return map(TagvynService.clearNickname(player, bypassLimit));
    }

    @Override
    public boolean setTitle(ServerPlayer player, String titleId) {
        return TagvynService.setTitle(player, titleId);
    }

    @Override
    public void clearTitle(ServerPlayer player) {
        TagvynService.clearTitle(player);
    }

    @Override
    public Optional<TagvynTitle> getTitle(String id) {
        return TitleRegistry.get(id).map(NeoForgeTagvynApi::toApiTitle);
    }

    @Override
    public Collection<TagvynTitle> getTitles() {
        List<TagvynTitle> titles = TitleRegistry.all().stream().map(NeoForgeTagvynApi::toApiTitle).toList();
        return List.copyOf(titles);
    }

    @Override
    public boolean registerTitle(TagvynTitle title, boolean overwrite) {
        return TitleRegistry.register(new TitleDefinition(
                title.id(),
                title.text(),
                title.color(),
                title.imageFont(),
                title.imageGlyph()
        ), overwrite);
    }

    @Override
    public boolean registerImageTitle(String id, String text, int color, byte[] png, boolean overwrite) {
        if (!TitleRegistry.isValidId(id) || !TitleImageStore.validate(png).valid()) return false;
        if (!overwrite && TitleRegistry.get(id).isPresent()) return false;

        Optional<byte[]> previous = TitleImageStore.read(id);
        if (!TitleImageStore.save(id, png)) return false;
        if (!TitleRegistry.registerUploadedImage(id, text, color, overwrite)) {
            if (previous.isPresent()) TitleImageStore.save(id, previous.get());
            else TitleImageStore.delete(id);
            return false;
        }
        return true;
    }

    @Override
    public Optional<byte[]> getTitleImage(String id) {
        Optional<TitleDefinition> definition = TitleRegistry.get(id);
        if (definition.isEmpty() || !TitleRegistry.isUploadedImage(definition.get())) return Optional.empty();
        return TitleImageStore.read(id).map(byte[]::clone);
    }

    @Override
    public boolean unregisterTitle(String id) {
        return TitleRegistry.remove(id);
    }

    @Override
    public void refreshTitles(MinecraftServer server) {
        TagvynService.refreshAllTitleSnapshots(server);
        TagvynNetwork.syncUploadedTitleImagesToAll(server);
    }

    @Override
    public Component formatDisplayName(Player player, Component fallbackName, boolean includeTitle) {
        return TagvynDisplay.format(player, fallbackName, includeTitle);
    }

    private static TagvynTitle toApiTitle(TitleDefinition definition) {
        return new TagvynTitle(
                definition.id(),
                definition.text(),
                definition.color(),
                definition.imageFont(),
                definition.imageGlyph()
        );
    }

    private static NicknameUpdateResult map(TagvynService.NicknameResult result) {
        NicknameUpdateResult.Failure failure = switch (result.failure()) {
            case NONE -> NicknameUpdateResult.Failure.NONE;
            case INVALID -> NicknameUpdateResult.Failure.INVALID;
            case LIMIT_REACHED -> NicknameUpdateResult.Failure.LIMIT_REACHED;
        };
        return new NicknameUpdateResult(
                result.success(),
                failure,
                result.nickname(),
                result.data().nicknameChanges(),
                TagvynService.remainingChanges(result.data())
        );
    }
}
