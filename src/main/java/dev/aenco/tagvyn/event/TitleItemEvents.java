package dev.aenco.tagvyn.event;

import dev.aenco.tagvyn.Tagvyn;
import dev.aenco.tagvyn.item.TitleItemService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = Tagvyn.MOD_ID)
public final class TitleItemEvents {
    private TitleItemEvents() {}

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!TitleItemService.isTitleItem(event.getItemStack())) return;

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        TitleItemService.RedeemResult result = TitleItemService.redeem(player, event.getItemStack());
        switch (result) {
            case SUCCESS -> player.sendSystemMessage(Component.translatable(
                    "tagvyn.message.title_item_used",
                    TitleItemService.titleId(event.getItemStack())
            ));
            case ALREADY_EQUIPPED -> player.sendSystemMessage(Component.translatable("tagvyn.message.title_item_already"));
            case MISSING_TITLE -> player.sendSystemMessage(Component.translatable("tagvyn.message.title_item_invalid"));
            case NOT_TITLE_ITEM -> {}
        }
    }

    @SubscribeEvent
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (!TitleItemService.isTitleItem(event.getItemStack())) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!TitleItemService.isTitleItem(event.getItemStack())) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }
}
