package dev.aenco.tagvyn.event;

import dev.aenco.tagvyn.Tagvyn;
import dev.aenco.tagvyn.command.TagvynCommands;
import dev.aenco.tagvyn.config.TagvynConfig;
import dev.aenco.tagvyn.data.TagvynAttachments;
import dev.aenco.tagvyn.display.TagvynDisplay;
import dev.aenco.tagvyn.network.TagvynNetwork;
import dev.aenco.tagvyn.service.TagvynMessages;
import dev.aenco.tagvyn.service.TagvynService;
import dev.aenco.tagvyn.title.TitleRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

@EventBusSubscriber(modid = Tagvyn.MOD_ID)
public final class TagvynEvents {
    private TagvynEvents() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        TagvynCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        TitleRegistry.load();
    }

    @SubscribeEvent
    public static void onNameFormat(PlayerEvent.NameFormat event) {
        event.setDisplayname(TagvynDisplay.format(
                event.getEntity(),
                event.getUsername(),
                TagvynConfig.VALUES.showTitleInDisplayName.get()
        ));
    }

    @SubscribeEvent
    public static void onTabListNameFormat(PlayerEvent.TabListNameFormat event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Component baseName = Component.literal(player.getGameProfile().getName());
        event.setDisplayName(TagvynDisplay.format(
                player,
                baseName,
                TagvynConfig.VALUES.showTitleInTab.get()
        ));
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        TagvynService.refreshTitleSnapshot(player);
        TagvynNetwork.syncUploadedTitleImages(player);
        if (!player.getData(TagvynAttachments.IDENTITY).hasNickname()) {
            TagvynMessages.sendNicknamePrompt(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            TagvynService.refresh(player);
        }
    }
}
