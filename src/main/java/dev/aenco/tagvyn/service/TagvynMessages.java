package dev.aenco.tagvyn.service;

import dev.aenco.tagvyn.config.TagvynConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;

public final class TagvynMessages {
    private TagvynMessages() {}

    public static void sendNicknamePrompt(ServerPlayer player) {
        Component prompt = Component.translatable("tagvyn.message.nickname_prompt")
                .withStyle(style -> style
                        .withColor(ChatFormatting.AQUA)
                        .withUnderlined(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tagvyn nick gui"))
                        .withHoverEvent(new HoverEvent(
                                HoverEvent.Action.SHOW_TEXT,
                                Component.translatable("tagvyn.message.nickname_prompt_hover")
                        )));
        player.sendSystemMessage(prompt);
    }

    public static void sendNicknameFailure(ServerPlayer player, TagvynService.NicknameResult result) {
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
    }
}
