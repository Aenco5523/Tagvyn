package dev.aenco.tagvyn.network;

import dev.aenco.tagvyn.item.TitleItemService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class TitleItemNetwork {
    private static final String PROTOCOL_VERSION = "4";

    private TitleItemNetwork() {}

    public static void register(IEventBus modBus) {
        modBus.addListener(TitleItemNetwork::registerPayloadHandlers);
    }

    private static void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToServer(
                GiveTitleItemPayload.TYPE,
                GiveTitleItemPayload.STREAM_CODEC,
                TitleItemNetwork::handleGiveTitleItem
        );
    }

    private static void handleGiveTitleItem(GiveTitleItemPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (!player.createCommandSourceStack().hasPermission(2)) return;

            String titleId = payload.titleId().trim().toLowerCase();
            if (!TitleItemService.give(player, titleId)) {
                player.sendSystemMessage(Component.translatable("tagvyn.message.title_missing", titleId));
                return;
            }

            player.sendSystemMessage(Component.translatable("tagvyn.message.title_item_given", titleId));
        });
    }
}
