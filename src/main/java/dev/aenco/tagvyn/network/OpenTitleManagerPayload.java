package dev.aenco.tagvyn.network;

import dev.aenco.tagvyn.Tagvyn;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record OpenTitleManagerPayload(
        List<TitleSummary> titles,
        String selectedTitleId
) implements CustomPacketPayload {
    public static final Type<OpenTitleManagerPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Tagvyn.MOD_ID, "open_title_manager")
    );

    public OpenTitleManagerPayload {
        titles = List.copyOf(titles);
        selectedTitleId = selectedTitleId == null ? "" : selectedTitleId;
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenTitleManagerPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.titles().size());
                for (TitleSummary title : payload.titles()) {
                    buf.writeUtf(title.id(), 64);
                    buf.writeUtf(title.text(), 128);
                    buf.writeInt(title.color());
                    buf.writeBoolean(title.image());
                }
                buf.writeUtf(payload.selectedTitleId(), 64);
            },
            buf -> {
                int count = buf.readVarInt();
                if (count < 0 || count > 512) throw new IllegalArgumentException("Too many Tagvyn titles");
                List<TitleSummary> titles = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    titles.add(new TitleSummary(
                            buf.readUtf(64),
                            buf.readUtf(128),
                            buf.readInt(),
                            buf.readBoolean()
                    ));
                }
                return new OpenTitleManagerPayload(titles, buf.readUtf(64));
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record TitleSummary(String id, String text, int color, boolean image) {}
}
