package dev.aenco.tagvyn.client;

import dev.aenco.tagvyn.network.OpenAdminDashboardPayload;
import dev.aenco.tagvyn.network.OpenNicknameScreenPayload;
import dev.aenco.tagvyn.network.OpenPlayerManagerPayload;
import dev.aenco.tagvyn.network.OpenTitleManagerPayload;
import dev.aenco.tagvyn.network.TitleImageDataPayload;
import dev.aenco.tagvyn.network.TitleImageManifestPayload;
import dev.aenco.tagvyn.network.TitleImageSyncCompletePayload;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class TagvynClientNetworking {
    private TagvynClientNetworking() {}

    public static void handleOpenNicknameScreen(OpenNicknameScreenPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> Minecraft.getInstance().setScreen(new NicknameScreen(payload)));
    }

    public static void handleOpenAdminDashboard(OpenAdminDashboardPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> Minecraft.getInstance().setScreen(new AdminDashboardScreen()));
    }

    public static void handleOpenPlayerManager(OpenPlayerManagerPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> Minecraft.getInstance().setScreen(new PlayerManagerScreen(payload)));
    }

    public static void handleOpenTitleManager(OpenTitleManagerPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> Minecraft.getInstance().setScreen(new TitleManagerScreen(payload)));
    }

    public static void handleTitleImageManifest(TitleImageManifestPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> TagvynClientResourcePack.beginSync(payload));
    }

    public static void handleTitleImageData(TitleImageDataPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> TagvynClientResourcePack.acceptImage(payload));
    }

    public static void handleTitleImageSyncComplete(TitleImageSyncCompletePayload payload, IPayloadContext context) {
        context.enqueueWork(TagvynClientResourcePack::finishSync);
    }
}
