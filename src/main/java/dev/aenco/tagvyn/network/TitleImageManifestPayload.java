package dev.aenco.tagvyn.network;

import dev.aenco.tagvyn.Tagvyn;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record TitleImageManifestPayload(List<Entry> entries) implements CustomPacketPayload {
    public static final Type<TitleImageManifestPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Tagvyn.MOD_ID, "title_image_manifest")
    );

    public TitleImageManifestPayload {
        entries = List.copyOf(entries);
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, TitleImageManifestPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.entries().size());
                for (Entry entry : payload.entries()) {
                    buf.writeUtf(entry.id(), 64);
                    buf.writeUtf(entry.glyph(), 16);
                }
            },
            buf -> {
                int count = buf.readVarInt();
                if (count < 0 || count > 512) throw new IllegalArgumentException("Too many Tagvyn image titles");
                List<Entry> entries = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    entries.add(new Entry(buf.readUtf(64), buf.readUtf(16)));
                }
                return new TitleImageManifestPayload(entries);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record Entry(String id, String glyph) {}
}
