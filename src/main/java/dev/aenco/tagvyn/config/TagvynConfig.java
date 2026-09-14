package dev.aenco.tagvyn.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public final class TagvynConfig {
    public static final Values VALUES;
    public static final ModConfigSpec SPEC;

    static {
        Pair<Values, ModConfigSpec> pair = new ModConfigSpec.Builder().configure(Values::new);
        VALUES = pair.getLeft();
        SPEC = pair.getRight();
    }

    private TagvynConfig() {}

    public static final class Values {
        public final ModConfigSpec.IntValue nicknameChangeLimit;
        public final ModConfigSpec.IntValue nicknameMinLength;
        public final ModConfigSpec.IntValue nicknameMaxLength;
        public final ModConfigSpec.BooleanValue allowSpaces;
        public final ModConfigSpec.BooleanValue showTitleInDisplayName;
        public final ModConfigSpec.BooleanValue showTitleInTab;

        Values(ModConfigSpec.Builder builder) {
            builder.push("nickname");
            nicknameChangeLimit = builder
                    .comment("How many nickname changes normal players may make. -1 = unlimited. Server operators bypass this limit.")
                    .defineInRange("changeLimit", 3, -1, 100000);
            nicknameMinLength = builder
                    .comment("Minimum nickname length.")
                    .defineInRange("minLength", 1, 1, 64);
            nicknameMaxLength = builder
                    .comment("Maximum nickname length.")
                    .defineInRange("maxLength", 24, 1, 64);
            allowSpaces = builder
                    .comment("Allow spaces inside nicknames.")
                    .define("allowSpaces", true);
            builder.pop();

            builder.push("display");
            showTitleInDisplayName = builder
                    .comment("Show titles in the general player display name (chat, name tag, and command feedback where Minecraft uses display names).")
                    .define("showTitleInDisplayName", true);
            showTitleInTab = builder
                    .comment("Show titles in the TAB player list.")
                    .define("showTitleInTab", true);
            builder.pop();
        }
    }
}
