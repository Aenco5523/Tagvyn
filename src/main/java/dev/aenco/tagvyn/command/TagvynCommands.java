package dev.aenco.tagvyn.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.aenco.tagvyn.api.TagvynAPI;
import dev.aenco.tagvyn.api.TagvynTitle;
import dev.aenco.tagvyn.config.TagvynConfig;
import dev.aenco.tagvyn.data.IdentityData;
import dev.aenco.tagvyn.data.TagvynAttachments;
import dev.aenco.tagvyn.network.TagvynNetwork;
import dev.aenco.tagvyn.service.TagvynMessages;
import dev.aenco.tagvyn.service.TagvynService;
import java.util.stream.Collectors;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class TagvynCommands {
    private TagvynCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var root = Commands.literal("tagvyn");

        var nick = Commands.literal("nick");
        nick.then(Commands.literal("gui")
                .executes(context -> openNicknameGui(context.getSource())));
        nick.then(Commands.literal("set")
                .then(Commands.argument("nickname", StringArgumentType.greedyString())
                        .executes(context -> setOwnNickname(
                                context.getSource(),
                                StringArgumentType.getString(context, "nickname")
                        ))));
        nick.then(Commands.literal("clear")
                .executes(context -> clearOwnNickname(context.getSource())));
        nick.then(Commands.literal("info")
                .executes(context -> showInfo(context.getSource())));
        nick.then(Commands.literal("setfor")
                .requires(TagvynCommands::isOperator)
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("nickname", StringArgumentType.greedyString())
                                .executes(context -> operatorSetNickname(
                                        context.getSource(),
                                        EntityArgument.getPlayer(context, "player"),
                                        StringArgumentType.getString(context, "nickname")
                                )))));
        nick.then(Commands.literal("clearfor")
                .requires(TagvynCommands::isOperator)
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> operatorClearNickname(
                                context.getSource(),
                                EntityArgument.getPlayer(context, "player")
                        ))));
        nick.then(Commands.literal("resetcount")
                .requires(TagvynCommands::isOperator)
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> resetCount(
                                context.getSource(),
                                EntityArgument.getPlayer(context, "player")
                        ))));
        root.then(nick);

        var title = Commands.literal("title");
        title.then(Commands.literal("list")
                .executes(context -> listTitles(context.getSource())));
        title.then(Commands.literal("set")
                .requires(TagvynCommands::isOperator)
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("title", StringArgumentType.word())
                                .executes(context -> operatorSetTitle(
                                        context.getSource(),
                                        EntityArgument.getPlayer(context, "player"),
                                        StringArgumentType.getString(context, "title")
                                )))));
        title.then(Commands.literal("clear")
                .requires(TagvynCommands::isOperator)
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> operatorClearTitle(
                                context.getSource(),
                                EntityArgument.getPlayer(context, "player")
                        ))));

        var create = Commands.literal("create").requires(TagvynCommands::isOperator);
        create.then(Commands.literal("text")
                .then(Commands.argument("id", StringArgumentType.word())
                        .then(Commands.argument("color", StringArgumentType.word())
                                .then(Commands.argument("text", StringArgumentType.greedyString())
                                        .executes(context -> createTextTitle(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "id"),
                                                StringArgumentType.getString(context, "color"),
                                                StringArgumentType.getString(context, "text")
                                        ))))));
        title.then(create);

        title.then(Commands.literal("delete")
                .requires(TagvynCommands::isOperator)
                .then(Commands.argument("id", StringArgumentType.word())
                        .executes(context -> deleteTitle(
                                context.getSource(),
                                StringArgumentType.getString(context, "id")
                        ))));
        root.then(title);

        root.then(Commands.literal("titles")
                .requires(TagvynCommands::isOperator)
                .executes(context -> openTitleManager(context.getSource())));
        root.then(Commands.literal("reload")
                .requires(TagvynCommands::isOperator)
                .executes(context -> reload(context.getSource())));

        dispatcher.register(root);
    }

    private static int openNicknameGui(CommandSourceStack source) throws CommandSyntaxException {
        TagvynNetwork.openNicknameScreen(source.getPlayerOrException());
        return 1;
    }

    private static int openTitleManager(CommandSourceStack source) throws CommandSyntaxException {
        TagvynNetwork.openTitleManagerScreen(source.getPlayerOrException());
        return 1;
    }

    private static int setOwnNickname(CommandSourceStack source, String nickname) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        TagvynService.NicknameResult result = TagvynService.setNickname(player, nickname, isOperator(source));
        if (!result.success()) {
            sendNicknameFailure(source, result);
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("tagvyn.message.nickname_set", result.nickname()), false);
        return 1;
    }

    private static int clearOwnNickname(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        TagvynService.NicknameResult result = TagvynService.clearNickname(player, isOperator(source));
        if (!result.success()) {
            sendNicknameFailure(source, result);
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("tagvyn.message.nickname_cleared"), false);
        TagvynMessages.sendNicknamePrompt(player);
        return 1;
    }

    private static int showInfo(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        IdentityData data = player.getData(TagvynAttachments.IDENTITY);
        int limit = TagvynConfig.VALUES.nicknameChangeLimit.get();
        String remaining = isOperator(source) || limit < 0 ? "∞" : Integer.toString(TagvynService.remainingChanges(data));
        String nickname = data.hasNickname() ? data.nickname() : "-";
        String title = data.hasTitle() ? data.titleId() : "-";
        source.sendSuccess(() -> Component.literal(
                "Tagvyn | nickname=" + nickname + " | title=" + title + " | remaining=" + remaining
        ), false);
        return 1;
    }

    private static int listTitles(CommandSourceStack source) {
        String titles = TagvynAPI.get().getTitles().stream().map(TagvynTitle::id).collect(Collectors.joining(", "));
        source.sendSuccess(() -> Component.literal("Tagvyn titles: " + (titles.isBlank() ? "(none)" : titles)), false);
        return 1;
    }

    private static int operatorSetNickname(CommandSourceStack source, ServerPlayer target, String nickname) {
        TagvynService.NicknameResult result = TagvynService.setNickname(target, nickname, true);
        if (!result.success()) {
            source.sendFailure(Component.translatable("tagvyn.message.nickname_invalid"));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable(
                "tagvyn.message.operator_nickname_set",
                target.getGameProfile().getName(),
                result.nickname()
        ), true);
        return 1;
    }

    private static int operatorClearNickname(CommandSourceStack source, ServerPlayer target) {
        TagvynService.clearNickname(target, true);
        source.sendSuccess(() -> Component.translatable(
                "tagvyn.message.operator_nickname_cleared",
                target.getGameProfile().getName()
        ), true);
        TagvynMessages.sendNicknamePrompt(target);
        return 1;
    }

    private static int resetCount(CommandSourceStack source, ServerPlayer target) {
        TagvynService.resetNicknameChanges(target);
        source.sendSuccess(() -> Component.translatable(
                "tagvyn.message.operator_count_reset",
                target.getGameProfile().getName()
        ), true);
        return 1;
    }

    private static int operatorSetTitle(CommandSourceStack source, ServerPlayer target, String titleId) {
        if (!TagvynService.setTitle(target, titleId)) {
            source.sendFailure(Component.translatable("tagvyn.message.title_missing", titleId));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable(
                "tagvyn.message.operator_title_set",
                target.getGameProfile().getName(),
                titleId
        ), true);
        return 1;
    }

    private static int operatorClearTitle(CommandSourceStack source, ServerPlayer target) {
        TagvynService.clearTitle(target);
        source.sendSuccess(() -> Component.translatable(
                "tagvyn.message.operator_title_cleared",
                target.getGameProfile().getName()
        ), true);
        return 1;
    }

    private static int createTextTitle(CommandSourceStack source, String id, String colorText, String text) {
        Integer color = parseColor(colorText);
        if (color == null) {
            source.sendFailure(Component.translatable("tagvyn.message.title_color_invalid", colorText));
            return 0;
        }
        return registerTitle(source, new TagvynTitle(id, text, color, "", ""));
    }

    private static int registerTitle(CommandSourceStack source, TagvynTitle title) {
        if (TagvynAPI.get().getTitle(title.id()).isPresent()) {
            source.sendFailure(Component.translatable("tagvyn.message.title_exists", title.id()));
            return 0;
        }
        if (!TagvynAPI.get().registerTitle(title, false)) {
            source.sendFailure(Component.translatable("tagvyn.message.title_invalid", title.id()));
            return 0;
        }
        TagvynAPI.get().refreshTitles(source.getServer());
        source.sendSuccess(() -> Component.translatable("tagvyn.message.title_created", title.id()), true);
        return 1;
    }

    private static int deleteTitle(CommandSourceStack source, String id) {
        if (!TagvynAPI.get().unregisterTitle(id)) {
            source.sendFailure(Component.translatable("tagvyn.message.title_missing", id));
            return 0;
        }
        TagvynAPI.get().refreshTitles(source.getServer());
        source.sendSuccess(() -> Component.translatable("tagvyn.message.title_deleted", id), true);
        return 1;
    }

    private static int reload(CommandSourceStack source) {
        TagvynService.reloadTitles(source.getServer());
        TagvynNetwork.syncUploadedTitleImagesToAll(source.getServer());
        source.sendSuccess(() -> Component.translatable("tagvyn.message.reload"), true);
        return 1;
    }

    private static Integer parseColor(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.startsWith("#")) value = value.substring(1);
        if (!value.matches("[0-9a-fA-F]{6}")) return null;
        return Integer.parseInt(value, 16);
    }

    private static void sendNicknameFailure(CommandSourceStack source, TagvynService.NicknameResult result) {
        if (result.failure() == TagvynService.NicknameFailure.LIMIT_REACHED) {
            int limit = TagvynConfig.VALUES.nicknameChangeLimit.get();
            source.sendFailure(Component.translatable(
                    "tagvyn.message.nickname_limit",
                    result.data().nicknameChanges(),
                    limit
            ));
        } else {
            source.sendFailure(Component.translatable("tagvyn.message.nickname_invalid"));
        }
    }

    private static boolean isOperator(CommandSourceStack source) {
        return source.hasPermission(2);
    }
}
