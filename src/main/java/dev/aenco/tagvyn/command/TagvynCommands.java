package dev.aenco.tagvyn.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.aenco.tagvyn.config.TagvynConfig;
import dev.aenco.tagvyn.data.IdentityData;
import dev.aenco.tagvyn.data.TagvynAttachments;
import dev.aenco.tagvyn.service.TagvynService;
import dev.aenco.tagvyn.title.TitleDefinition;
import dev.aenco.tagvyn.title.TitleRegistry;
import java.util.stream.Collectors;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class TagvynCommands {
    private TagvynCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tagvyn")
                .then(Commands.literal("nick")
                        .then(Commands.literal("set")
                                .then(Commands.argument("nickname", StringArgumentType.greedyString())
                                        .executes(context -> setOwnNickname(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "nickname")
                                        ))))
                        .then(Commands.literal("clear")
                                .executes(context -> clearOwnNickname(context.getSource())))
                        .then(Commands.literal("info")
                                .executes(context -> showInfo(context.getSource()))))
                .then(Commands.literal("title")
                        .then(Commands.literal("list")
                                .executes(context -> listTitles(context.getSource()))))
                .then(Commands.literal("admin")
                        .requires(TagvynCommands::isAdmin)
                        .then(Commands.literal("nick")
                                .then(Commands.literal("set")
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .then(Commands.argument("nickname", StringArgumentType.greedyString())
                                                        .executes(context -> adminSetNickname(
                                                                context.getSource(),
                                                                EntityArgument.getPlayer(context, "player"),
                                                                StringArgumentType.getString(context, "nickname")
                                                        )))))
                                .then(Commands.literal("clear")
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(context -> adminClearNickname(
                                                        context.getSource(),
                                                        EntityArgument.getPlayer(context, "player")
                                                ))))
                                .then(Commands.literal("reset-count")
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(context -> resetCount(
                                                        context.getSource(),
                                                        EntityArgument.getPlayer(context, "player")
                                                )))))
                        .then(Commands.literal("title")
                                .then(Commands.literal("set")
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .then(Commands.argument("title", StringArgumentType.word())
                                                        .executes(context -> adminSetTitle(
                                                                context.getSource(),
                                                                EntityArgument.getPlayer(context, "player"),
                                                                StringArgumentType.getString(context, "title")
                                                        )))))
                                .then(Commands.literal("clear")
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(context -> adminClearTitle(
                                                        context.getSource(),
                                                        EntityArgument.getPlayer(context, "player")
                                                )))))
                        .then(Commands.literal("reload")
                                .executes(context -> reload(context.getSource()))))
        );
    }

    private static int setOwnNickname(CommandSourceStack source, String nickname) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        boolean bypass = isAdmin(source);
        TagvynService.NicknameResult result = TagvynService.setNickname(player, nickname, bypass);
        if (!result.success()) {
            sendNicknameFailure(source, result);
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("tagvyn.message.nickname_set", result.nickname()), false);
        return 1;
    }

    private static int clearOwnNickname(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        TagvynService.NicknameResult result = TagvynService.clearNickname(player, isAdmin(source));
        if (!result.success()) {
            sendNicknameFailure(source, result);
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("tagvyn.message.nickname_cleared"), false);
        return 1;
    }

    private static int showInfo(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        IdentityData data = player.getData(TagvynAttachments.IDENTITY);
        int limit = TagvynConfig.VALUES.nicknameChangeLimit.get();
        String remaining = limit < 0 ? "∞" : Integer.toString(TagvynService.remainingChanges(data));
        String nickname = data.hasNickname() ? data.nickname() : "-";
        String title = data.hasTitle() ? data.titleId() : "-";
        source.sendSuccess(() -> Component.literal(
                "Tagvyn | nickname=" + nickname + " | title=" + title + " | remaining=" + remaining
        ), false);
        return 1;
    }

    private static int listTitles(CommandSourceStack source) {
        String titles = TitleRegistry.all().stream().map(TitleDefinition::id).collect(Collectors.joining(", "));
        source.sendSuccess(() -> Component.literal("Tagvyn titles: " + (titles.isBlank() ? "(none)" : titles)), false);
        return 1;
    }

    private static int adminSetNickname(CommandSourceStack source, ServerPlayer target, String nickname) {
        TagvynService.NicknameResult result = TagvynService.setNickname(target, nickname, true);
        if (!result.success()) {
            source.sendFailure(Component.translatable("tagvyn.message.nickname_invalid"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Set " + target.getGameProfile().getName() + " nickname to " + result.nickname()), true);
        return 1;
    }

    private static int adminClearNickname(CommandSourceStack source, ServerPlayer target) {
        TagvynService.clearNickname(target, true);
        source.sendSuccess(() -> Component.literal("Cleared " + target.getGameProfile().getName() + " nickname."), true);
        return 1;
    }

    private static int resetCount(CommandSourceStack source, ServerPlayer target) {
        TagvynService.resetNicknameChanges(target);
        source.sendSuccess(() -> Component.literal("Reset nickname change count for " + target.getGameProfile().getName() + "."), true);
        return 1;
    }

    private static int adminSetTitle(CommandSourceStack source, ServerPlayer target, String titleId) {
        if (!TagvynService.setTitle(target, titleId)) {
            source.sendFailure(Component.translatable("tagvyn.message.title_missing", titleId));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Set " + target.getGameProfile().getName() + " title to " + titleId + "."), true);
        return 1;
    }

    private static int adminClearTitle(CommandSourceStack source, ServerPlayer target) {
        TagvynService.clearTitle(target);
        source.sendSuccess(() -> Component.literal("Cleared " + target.getGameProfile().getName() + " title."), true);
        return 1;
    }

    private static int reload(CommandSourceStack source) {
        TagvynService.reloadTitles(source.getServer());
        source.sendSuccess(() -> Component.translatable("tagvyn.message.reload"), true);
        return 1;
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

    private static boolean isAdmin(CommandSourceStack source) {
        return source.hasPermission(TagvynConfig.VALUES.adminPermissionLevel.get());
    }
}
