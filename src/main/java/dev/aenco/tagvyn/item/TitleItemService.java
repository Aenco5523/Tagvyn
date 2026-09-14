package dev.aenco.tagvyn.item;

import dev.aenco.tagvyn.data.TagvynAttachments;
import dev.aenco.tagvyn.service.TagvynService;
import dev.aenco.tagvyn.title.TitleDefinition;
import dev.aenco.tagvyn.title.TitleRegistry;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;

public final class TitleItemService {
    private static final String MARKER_KEY = "tagvyn_title_item";
    private static final String TITLE_ID_KEY = "tagvyn_title_id";

    private TitleItemService() {}

    public static ItemStack create(TitleDefinition title) {
        ItemStack stack = new ItemStack(Items.NAME_TAG);
        String shown = title.text().isBlank() ? title.id() : title.text();

        stack.set(
                DataComponents.ITEM_NAME,
                Component.translatable("tagvyn.item.title_token.name", shown)
                        .withStyle(style -> style.withColor(title.color()).withItalic(false))
        );
        stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        stack.set(
                DataComponents.LORE,
                new ItemLore(List.of(
                        Component.translatable("tagvyn.item.title_token.lore.use")
                                .withStyle(ChatFormatting.GRAY)
                                .withStyle(style -> style.withItalic(false)),
                        Component.translatable("tagvyn.item.title_token.lore.id", title.id())
                                .withStyle(ChatFormatting.DARK_GRAY)
                                .withStyle(style -> style.withItalic(false))
                ))
        );

        CompoundTag data = new CompoundTag();
        data.putBoolean(MARKER_KEY, true);
        data.putString(TITLE_ID_KEY, title.id());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        return stack;
    }

    public static boolean isTitleItem(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.is(Items.NAME_TAG)) return false;
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null || !data.contains(MARKER_KEY) || !data.contains(TITLE_ID_KEY)) return false;
        CompoundTag tag = data.copyTag();
        return tag.getBoolean(MARKER_KEY) && !tag.getString(TITLE_ID_KEY).isBlank();
    }

    public static String titleId(ItemStack stack) {
        if (!isTitleItem(stack)) return "";
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? "" : data.copyTag().getString(TITLE_ID_KEY).trim().toLowerCase();
    }

    public static boolean give(ServerPlayer player, String titleId) {
        TitleDefinition title = TitleRegistry.get(titleId).orElse(null);
        if (title == null) return false;

        ItemStack stack = create(title);
        if (!player.addItem(stack)) {
            player.drop(stack, false);
        }
        return true;
    }

    public static RedeemResult redeem(ServerPlayer player, ItemStack stack) {
        String id = titleId(stack);
        if (id.isBlank()) return RedeemResult.NOT_TITLE_ITEM;
        if (TitleRegistry.get(id).isEmpty()) return RedeemResult.MISSING_TITLE;
        if (player.getData(TagvynAttachments.IDENTITY).titleId().equalsIgnoreCase(id)) {
            return RedeemResult.ALREADY_EQUIPPED;
        }
        if (!TagvynService.setTitle(player, id)) return RedeemResult.MISSING_TITLE;

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return RedeemResult.SUCCESS;
    }

    public enum RedeemResult {
        SUCCESS,
        ALREADY_EQUIPPED,
        MISSING_TITLE,
        NOT_TITLE_ITEM
    }
}
