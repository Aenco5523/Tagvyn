package dev.aenco.tagvyn.network;

import dev.aenco.tagvyn.client.TagvynClientNetworking;
import dev.aenco.tagvyn.config.TagvynConfig;
import dev.aenco.tagvyn.data.IdentityData;
import dev.aenco.tagvyn.data.TagvynAttachments;
import dev.aenco.tagvyn.service.TagvynService;
import dev.aenco.tagvyn.title.TitleDefinition;
import dev.aenco.tagvyn.title.TitleImageStore;
import dev.aenco.tagvyn.title.TitleRegistry;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class TagvynNetwork {
    private static final String PROTOCOL_VERSION = "2";

    private TagvynNetwork() {}

    public static void register(IEventBus modBus) {
        modBus.addListener(TagvynNetwork::registerPayloadHandlers);
    }

    public static void openNicknameScreen(ServerPlayer player) {
        IdentityData data = player.getData(TagvynAttachments.IDENTITY);
        boolean operator = player.createCommandSourceStack().hasPermission(2);
        int remaining = operator ? -1 : TagvynService.remainingChanges(data);
        int minLength = Math.min(TagvynConfig.VALUES.nicknameMinLength.get(), TagvynConfig.VALUES.nicknameMaxLength.get());
        int maxLength = Math.max(TagvynConfig.VALUES.nicknameMinLength.get(), TagvynConfig.VALUES.nicknameMaxLength.get());
        PacketDistributor.sendToPlayer(player, new OpenNicknameScreenPayload(
                data.nickname(),
                remaining,
                minLength,
                maxLength,
                TagvynConfig.VALUES.allowSpaces.get()
        ));
    }

    public static void openTitleManagerScreen(ServerPlayer player) {
        if (!player.createCommandSourceStack().hasPermission(2)) return;
        List<OpenTitleManagerPayload.TitleSummary> summaries = TitleRegistry.all().stream()
                .map(title -> new OpenTitleManagerPayload.TitleSummary(
                        title.id(),
                        title.text(),
                        title.color(),
                        title.hasImage()
                ))
                .toList();
        PacketDistributor.sendToPlayer(player, new OpenTitleManagerPayload(summaries));
    }

    public static void syncUploadedTitleImages(ServerPlayer player) {
        List<TitleDefinition> images = TitleRegistry.uploadedImages();
        PacketDistributor.sendToPlayer(player, new TitleImageManifestPayload(
                images.stream()
                        .map(title -> new TitleImageManifestPayload.Entry(title.id(), title.imageGlyph()))
                        .toList()
        ));
        for (TitleDefinition title : images) {
            TitleImageStore.read(title.id()).ifPresent(bytes ->
                    PacketDistributor.sendToPlayer(player, new TitleImageDataPayload(title.id(), bytes))
            );
        }
        PacketDistributor.sendToPlayer(player, new TitleImageSyncCompletePayload());
    }

    public static void syncUploadedTitleImagesToAll(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            syncUploadedTitleImages(player);
        }
    }

    private static void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        if (FMLEnvironment.dist.isClient()) {
            registrar.playToClient(OpenNicknameScreenPayload.TYPE, OpenNicknameScreenPayload.STREAM_CODEC, TagvynClientNetworking::handleOpenNicknameScreen);
            registrar.playToClient(OpenTitleManagerPayload.TYPE, OpenTitleManagerPayload.STREAM_CODEC, TagvynClientNetworking::handleOpenTitleManager);
            registrar.playToClient(TitleImageManifestPayload.TYPE, TitleImageManifestPayload.STREAM_CODEC, TagvynClientNetworking::handleTitleImageManifest);
            registrar.playToClient(TitleImageDataPayload.TYPE, TitleImageDataPayload.STREAM_CODEC, TagvynClientNetworking::handleTitleImageData);
            registrar.playToClient(TitleImageSyncCompletePayload.TYPE, TitleImageSyncCompletePayload.STREAM_CODEC, TagvynClientNetworking::handleTitleImageSyncComplete);
        } else {
            registrar.playToClient(OpenNicknameScreenPayload.TYPE, OpenNicknameScreenPayload.STREAM_CODEC, (payload, context) -> {});
            registrar.playToClient(OpenTitleManagerPayload.TYPE, OpenTitleManagerPayload.STREAM_CODEC, (payload, context) -> {});
            registrar.playToClient(TitleImageManifestPayload.TYPE, TitleImageManifestPayload.STREAM_CODEC, (payload, context) -> {});
            registrar.playToClient(TitleImageDataPayload.TYPE, TitleImageDataPayload.STREAM_CODEC, (payload, context) -> {});
            registrar.playToClient(TitleImageSyncCompletePayload.TYPE, TitleImageSyncCompletePayload.STREAM_CODEC, (payload, context) -> {});
        }

        registrar.playToServer(SubmitNicknamePayload.TYPE, SubmitNicknamePayload.STREAM_CODEC, TagvynNetwork::handleSubmitNickname);
        registrar.playToServer(CreateTextTitlePayload.TYPE, CreateTextTitlePayload.STREAM_CODEC, TagvynNetwork::handleCreateTextTitle);
        registrar.playToServer(UploadImageTitlePayload.TYPE, UploadImageTitlePayload.STREAM_CODEC, TagvynNetwork::handleUploadImageTitle);
        registrar.playToServer(DeleteTitlePayload.TYPE, DeleteTitlePayload.STREAM_CODEC, TagvynNetwork::handleDeleteTitle);
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

    private static void handleCreateTextTitle(CreateTextTitlePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || !isOperator(player)) return;
            if (TitleRegistry.get(payload.id()).isPresent()) {
                player.sendSystemMessage(Component.translatable("tagvyn.message.title_exists", payload.id()));
                return;
            }
            boolean created = TitleRegistry.register(new TitleDefinition(
                    payload.id(),
                    payload.text(),
                    payload.color(),
                    "",
                    ""
            ), false);
            if (!created) {
                player.sendSystemMessage(Component.translatable("tagvyn.message.title_invalid", payload.id()));
                return;
            }
            TagvynService.refreshAllTitleSnapshots(player.server);
            player.sendSystemMessage(Component.translatable("tagvyn.message.title_created", payload.id()));
            openTitleManagerScreen(player);
        });
    }

    private static void handleUploadImageTitle(UploadImageTitlePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || !isOperator(player)) return;
            String id = payload.id().trim().toLowerCase();
            if (!TitleRegistry.isValidId(id)) {
                player.sendSystemMessage(Component.translatable("tagvyn.message.title_invalid", id));
                return;
            }
            if (TitleRegistry.get(id).isPresent()) {
                player.sendSystemMessage(Component.translatable("tagvyn.message.title_exists", id));
                return;
            }

            byte[] png = payload.png();
            TitleImageStore.Validation validation = TitleImageStore.validate(png);
            if (!validation.valid()) {
                player.sendSystemMessage(Component.translatable("tagvyn.message.title_image_invalid"));
                return;
            }
            if (!TitleImageStore.save(id, png)) {
                player.sendSystemMessage(Component.translatable("tagvyn.message.title_image_save_failed"));
                return;
            }
            if (!TitleRegistry.registerUploadedImage(id, payload.text(), payload.color(), false)) {
                TitleImageStore.delete(id);
                player.sendSystemMessage(Component.translatable("tagvyn.message.title_invalid", id));
                return;
            }

            TagvynService.refreshAllTitleSnapshots(player.server);
            syncUploadedTitleImagesToAll(player.server);
            player.sendSystemMessage(Component.translatable(
                    "tagvyn.message.title_image_created",
                    id,
                    validation.width(),
                    validation.height()
            ));
            openTitleManagerScreen(player);
        });
    }

    private static void handleDeleteTitle(DeleteTitlePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || !isOperator(player)) return;
            if (!TitleRegistry.remove(payload.id())) {
                player.sendSystemMessage(Component.translatable("tagvyn.message.title_missing", payload.id()));
                return;
            }
            TagvynService.refreshAllTitleSnapshots(player.server);
            syncUploadedTitleImagesToAll(player.server);
            player.sendSystemMessage(Component.translatable("tagvyn.message.title_deleted", payload.id()));
            openTitleManagerScreen(player);
        });
    }

    private static boolean isOperator(ServerPlayer player) {
        return player.createCommandSourceStack().hasPermission(2);
    }
}
