package dev.aenco.tagvyn.network;

import dev.aenco.tagvyn.client.TagvynClientNetworking;
import dev.aenco.tagvyn.config.TagvynConfig;
import dev.aenco.tagvyn.data.IdentityData;
import dev.aenco.tagvyn.data.TagvynAttachments;
import dev.aenco.tagvyn.service.TagvynMessages;
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
    private static final String PROTOCOL_VERSION = "3";

    private TagvynNetwork() {}

    public static void register(IEventBus modBus) {
        modBus.addListener(TagvynNetwork::registerPayloadHandlers);
    }

    public static void openDefaultScreen(ServerPlayer player) {
        if (isOperator(player)) {
            openAdminDashboard(player);
        } else {
            openNicknameScreen(player);
        }
    }

    public static void openAdminDashboard(ServerPlayer player) {
        if (!isOperator(player)) return;
        PacketDistributor.sendToPlayer(player, new OpenAdminDashboardPayload());
    }

    public static void openNicknameScreen(ServerPlayer player) {
        IdentityData data = player.getData(TagvynAttachments.IDENTITY);
        boolean operator = isOperator(player);
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

    public static void openPlayerManagerScreen(ServerPlayer player) {
        if (!isOperator(player)) return;
        List<OpenPlayerManagerPayload.PlayerSummary> players = player.server.getPlayerList().getPlayers().stream()
                .map(target -> {
                    IdentityData data = target.getData(TagvynAttachments.IDENTITY);
                    return new OpenPlayerManagerPayload.PlayerSummary(
                            target.getGameProfile().getName(),
                            data.nickname(),
                            data.titleId(),
                            data.nicknameChanges(),
                            TagvynService.remainingChanges(data)
                    );
                })
                .toList();
        List<String> titles = TitleRegistry.all().stream().map(TitleDefinition::id).toList();
        PacketDistributor.sendToPlayer(player, new OpenPlayerManagerPayload(players, titles));
    }

    public static void openTitleManagerScreen(ServerPlayer player) {
        if (!isOperator(player)) return;
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
            registrar.playToClient(OpenAdminDashboardPayload.TYPE, OpenAdminDashboardPayload.STREAM_CODEC, TagvynClientNetworking::handleOpenAdminDashboard);
            registrar.playToClient(OpenPlayerManagerPayload.TYPE, OpenPlayerManagerPayload.STREAM_CODEC, TagvynClientNetworking::handleOpenPlayerManager);
            registrar.playToClient(OpenTitleManagerPayload.TYPE, OpenTitleManagerPayload.STREAM_CODEC, TagvynClientNetworking::handleOpenTitleManager);
            registrar.playToClient(TitleImageManifestPayload.TYPE, TitleImageManifestPayload.STREAM_CODEC, TagvynClientNetworking::handleTitleImageManifest);
            registrar.playToClient(TitleImageDataPayload.TYPE, TitleImageDataPayload.STREAM_CODEC, TagvynClientNetworking::handleTitleImageData);
            registrar.playToClient(TitleImageSyncCompletePayload.TYPE, TitleImageSyncCompletePayload.STREAM_CODEC, TagvynClientNetworking::handleTitleImageSyncComplete);
        } else {
            registrar.playToClient(OpenNicknameScreenPayload.TYPE, OpenNicknameScreenPayload.STREAM_CODEC, (payload, context) -> {});
            registrar.playToClient(OpenAdminDashboardPayload.TYPE, OpenAdminDashboardPayload.STREAM_CODEC, (payload, context) -> {});
            registrar.playToClient(OpenPlayerManagerPayload.TYPE, OpenPlayerManagerPayload.STREAM_CODEC, (payload, context) -> {});
            registrar.playToClient(OpenTitleManagerPayload.TYPE, OpenTitleManagerPayload.STREAM_CODEC, (payload, context) -> {});
            registrar.playToClient(TitleImageManifestPayload.TYPE, TitleImageManifestPayload.STREAM_CODEC, (payload, context) -> {});
            registrar.playToClient(TitleImageDataPayload.TYPE, TitleImageDataPayload.STREAM_CODEC, (payload, context) -> {});
            registrar.playToClient(TitleImageSyncCompletePayload.TYPE, TitleImageSyncCompletePayload.STREAM_CODEC, (payload, context) -> {});
        }

        registrar.playToServer(SubmitNicknamePayload.TYPE, SubmitNicknamePayload.STREAM_CODEC, TagvynNetwork::handleSubmitNickname);
        registrar.playToServer(AdminDashboardActionPayload.TYPE, AdminDashboardActionPayload.STREAM_CODEC, TagvynNetwork::handleAdminDashboardAction);
        registrar.playToServer(AdminPlayerActionPayload.TYPE, AdminPlayerActionPayload.STREAM_CODEC, TagvynNetwork::handleAdminPlayerAction);
        registrar.playToServer(CreateTextTitlePayload.TYPE, CreateTextTitlePayload.STREAM_CODEC, TagvynNetwork::handleCreateTextTitle);
        registrar.playToServer(UploadImageTitlePayload.TYPE, UploadImageTitlePayload.STREAM_CODEC, TagvynNetwork::handleUploadImageTitle);
        registrar.playToServer(DeleteTitlePayload.TYPE, DeleteTitlePayload.STREAM_CODEC, TagvynNetwork::handleDeleteTitle);
    }

    private static void handleSubmitNickname(SubmitNicknamePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            boolean bypass = isOperator(player);
            TagvynService.NicknameResult result = TagvynService.setNickname(player, payload.nickname(), bypass);
            if (result.success()) {
                player.sendSystemMessage(Component.translatable("tagvyn.message.nickname_set", result.nickname()));
                if (bypass) openAdminDashboard(player);
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

    private static void handleAdminDashboardAction(AdminDashboardActionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || !isOperator(player)) return;
            switch (payload.action()) {
                case "dashboard" -> openAdminDashboard(player);
                case "players" -> openPlayerManagerScreen(player);
                case "titles" -> openTitleManagerScreen(player);
                case "nickname" -> openNicknameScreen(player);
                case "reload" -> {
                    TagvynService.reloadTitles(player.server);
                    syncUploadedTitleImagesToAll(player.server);
                    player.sendSystemMessage(Component.translatable("tagvyn.message.reload"));
                    openAdminDashboard(player);
                }
                default -> openAdminDashboard(player);
            }
        });
    }

    private static void handleAdminPlayerAction(AdminPlayerActionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer admin) || !isOperator(admin)) return;
            ServerPlayer target = admin.server.getPlayerList().getPlayers().stream()
                    .filter(player -> player.getGameProfile().getName().equalsIgnoreCase(payload.playerName()))
                    .findFirst()
                    .orElse(null);
            if (target == null) {
                admin.sendSystemMessage(Component.translatable("tagvyn.message.player_missing", payload.playerName()));
                openPlayerManagerScreen(admin);
                return;
            }

            switch (payload.action()) {
                case "set_nickname" -> {
                    TagvynService.NicknameResult result = TagvynService.setNickname(target, payload.value(), true);
                    if (result.success()) {
                        admin.sendSystemMessage(Component.translatable(
                                "tagvyn.message.operator_nickname_set",
                                target.getGameProfile().getName(),
                                result.nickname()
                        ));
                    } else {
                        admin.sendSystemMessage(Component.translatable("tagvyn.message.nickname_invalid"));
                    }
                }
                case "clear_nickname" -> {
                    TagvynService.clearNickname(target, true);
                    admin.sendSystemMessage(Component.translatable(
                            "tagvyn.message.operator_nickname_cleared",
                            target.getGameProfile().getName()
                    ));
                    TagvynMessages.sendNicknamePrompt(target);
                }
                case "reset_count" -> {
                    TagvynService.resetNicknameChanges(target);
                    admin.sendSystemMessage(Component.translatable(
                            "tagvyn.message.operator_count_reset",
                            target.getGameProfile().getName()
                    ));
                }
                case "set_title" -> {
                    if (TagvynService.setTitle(target, payload.value())) {
                        admin.sendSystemMessage(Component.translatable(
                                "tagvyn.message.operator_title_set",
                                target.getGameProfile().getName(),
                                payload.value()
                        ));
                    } else {
                        admin.sendSystemMessage(Component.translatable("tagvyn.message.title_missing", payload.value()));
                    }
                }
                case "clear_title" -> {
                    TagvynService.clearTitle(target);
                    admin.sendSystemMessage(Component.translatable(
                            "tagvyn.message.operator_title_cleared",
                            target.getGameProfile().getName()
                    ));
                }
                default -> {}
            }
            openPlayerManagerScreen(admin);
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
