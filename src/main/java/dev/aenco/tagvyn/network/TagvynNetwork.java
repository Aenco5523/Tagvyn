package dev.aenco.tagvyn.network;

import dev.aenco.tagvyn.client.TagvynClientNetworking;
import dev.aenco.tagvyn.config.TagvynConfig;
import dev.aenco.tagvyn.data.IdentityData;
import dev.aenco.tagvyn.data.TagvynAttachments;
import dev.aenco.tagvyn.service.TagvynService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class TagvynNetwork {
    private static final String PROTOCOL_VERSION = "1";

    private TagvynNetwork() {}

    public static void register(IEventBus modBus) {
        modBus.addListener(TagvynNetwork::registerPayloadHandlers);
    }

    public static void openNicknameScreen(ServerPlayer player) {
        IdentityData data = player.getData(TagvynAttachments.IDENTITY);
        boolean operator = player.createCommandSourceStack().hasPermission(2);
        int remaining = operator ? -1 : TagvynService.remainingChanges(data);
        int minLength = Math.min(
                TagvynConfig.VALUES.nicknameMinLength.get(),
                TagvynConfig.VALUES.nicknameMaxLength.get()
        );
        int maxLength = Math.max(
                TagvynConfig.VALUES.nicknameMinLength.get(),
                TagvynConfig.VALUES.nicknameMaxLength.get()
        );
        PacketDistributor.sendToPlayer(player, new OpenNicknameScreenPayload(
                data.nickname(),
                remaining,
                minLength,
                maxLength,
                TagvynConfig.VALUES.allowSpaces.get()
        ));
    }

    private static void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        if (FMLEnvironment.dist.isClient()) {
            registrar.playToClient(
                    OpenNicknameScreenPayload.TYPE,
                    OpenNicknameScreenPayload.STREAM_CODEC,
                    TagvynClientNetworking::handleOpenNicknameScreen
            );
        } else {
            registrar.playToClient(
                    OpenNicknameScreenPayload.TYPE,
                    OpenNicknameScreenPayload.STREAM_CODEC,
                    (payload, context) -> {}
            );
        }
        registrar.playToServer(
                SubmitNicknamePayload.TYPE,
                SubmitNicknamePayload.STREAM_CODEC,
                TagvynNetwork::handleSubmitNickname
        );
    }

    private static void handleSubmitNickname(SubmitNicknamePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            boolean bypass = player.createCommandSourceStack().hasPermission(2);
            TagvynService.NicknameResult result = TagvynService.setNickname(player, payload.nickname(), bypass);
            if (result.success()) {
                player.sendSystemMessage(Component.translatable("tagvyn.message.nickname_set", result.nickname()));
                return;
            }

            if (result.failure() == TagvynService.NicknameFailure.LIMIT_REACHED) {
                int limit = TagvynConfig.VALUES.nicknameChangeLimit.get();
                player.sendSystemMessage(Component.translatable(
                        "tagvyn.message.nickname_limit",
                        result.data().nicknameChanges(),
                        limit
                ));
            } else {
                player.sendSystemMessage(Component.translatable("tagvyn.message.nickname_invalid"));
            }
        });
    }
}
