package dev.aenco.tagvyn.display;

import dev.aenco.tagvyn.data.IdentityData;
import dev.aenco.tagvyn.data.TagvynAttachments;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public final class TagvynDisplay {
    private TagvynDisplay() {}

    public static Component format(Player player, Component vanillaName, boolean includeTitle) {
        IdentityData identity = player.getData(TagvynAttachments.IDENTITY);
        MutableComponent result = Component.empty();

        if (includeTitle && identity.hasTitle()) {
            if (!identity.imageFont().isBlank() && !identity.imageGlyph().isBlank()) {
                ResourceLocation font = ResourceLocation.tryParse(identity.imageFont());
                if (font != null) {
                    result.append(Component.literal(identity.imageGlyph())
                            .withStyle(style -> style.withFont(font).withColor(identity.titleColor())));
                    result.append(Component.literal(" "));
                }
            }

            if (!identity.titleText().isBlank()) {
                result.append(Component.literal("[" + identity.titleText() + "]")
                        .withStyle(style -> style.withColor(identity.titleColor())));
                result.append(Component.literal(" "));
            }
        }

        if (identity.hasNickname()) {
            result.append(Component.literal(identity.nickname()));
        } else {
            result.append(vanillaName.copy());
        }
        return result;
    }
}
