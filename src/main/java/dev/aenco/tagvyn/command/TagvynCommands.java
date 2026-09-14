package dev.aenco.tagvyn.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.aenco.tagvyn.network.TagvynNetwork;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public final class TagvynCommands {
    private TagvynCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tagvyn")
                .executes(context -> openDefaultGui(context.getSource()))
                .then(Commands.literal("nick")
                        .executes(context -> openNicknameGui(context.getSource())))
        );
    }

    private static int openDefaultGui(CommandSourceStack source) throws CommandSyntaxException {
        TagvynNetwork.openDefaultScreen(source.getPlayerOrException());
        return 1;
    }

    private static int openNicknameGui(CommandSourceStack source) throws CommandSyntaxException {
        TagvynNetwork.openNicknameScreen(source.getPlayerOrException());
        return 1;
    }
}
