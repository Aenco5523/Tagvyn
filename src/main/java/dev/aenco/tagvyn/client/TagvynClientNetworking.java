package dev.aenco.tagvyn.client;

import dev.aenco.tagvyn.network.OpenNicknameScreenPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class TagvynClientNetworking {
    private TagvynClientNetworking() {}

    public static void handleOpenNicknameScreen(OpenNicknameScreenPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> Minecraft.getInstance().setScreen(new NicknameScreen(payload)));
    }
}
