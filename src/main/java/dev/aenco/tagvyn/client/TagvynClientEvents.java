package dev.aenco.tagvyn.client;

import dev.aenco.tagvyn.Tagvyn;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = Tagvyn.MOD_ID, value = Dist.CLIENT)
public final class TagvynClientEvents {
    private static int ticks;

    private TagvynClientEvents() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (++ticks % 10 != 0) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;

        for (AbstractClientPlayer player : minecraft.level.players()) {
            player.refreshDisplayName();
        }
    }
}
