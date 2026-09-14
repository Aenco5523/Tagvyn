package dev.aenco.tagvyn.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record IdentityData(
        String nickname,
        int nicknameChanges,
        String titleId,
        String titleText,
        int titleColor,
        String imageFont,
        String imageGlyph
) {
    public static final Codec<IdentityData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("nickname", "").forGetter(IdentityData::nickname),
            Codec.INT.optionalFieldOf("nicknameChanges", 0).forGetter(IdentityData::nicknameChanges),
            Codec.STRING.optionalFieldOf("titleId", "").forGetter(IdentityData::titleId),
            Codec.STRING.optionalFieldOf("titleText", "").forGetter(IdentityData::titleText),
            Codec.INT.optionalFieldOf("titleColor", 0xFFFFFF).forGetter(IdentityData::titleColor),
            Codec.STRING.optionalFieldOf("imageFont", "").forGetter(IdentityData::imageFont),
            Codec.STRING.optionalFieldOf("imageGlyph", "").forGetter(IdentityData::imageGlyph)
    ).apply(instance, IdentityData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, IdentityData> STREAM_CODEC = StreamCodec.of(
            (buf, value) -> {
                buf.writeUtf(value.nickname, 64);
                buf.writeVarInt(value.nicknameChanges);
                buf.writeUtf(value.titleId, 128);
                buf.writeUtf(value.titleText, 128);
                buf.writeInt(value.titleColor);
                buf.writeUtf(value.imageFont, 256);
                buf.writeUtf(value.imageGlyph, 16);
            },
            buf -> new IdentityData(
                    buf.readUtf(64),
                    buf.readVarInt(),
                    buf.readUtf(128),
                    buf.readUtf(128),
                    buf.readInt(),
                    buf.readUtf(256),
                    buf.readUtf(16)
            )
    );

    public static IdentityData empty() {
        return new IdentityData("", 0, "", "", 0xFFFFFF, "", "");
    }

    public boolean hasNickname() {
        return !nickname.isBlank();
    }

    public boolean hasTitle() {
        return !titleId.isBlank() && (!titleText.isBlank() || !imageGlyph.isBlank());
    }

    public IdentityData withNickname(String nickname, boolean countChange) {
        return new IdentityData(
                nickname,
                nicknameChanges + (countChange ? 1 : 0),
                titleId,
                titleText,
                titleColor,
                imageFont,
                imageGlyph
        );
    }

    public IdentityData withNicknameChanges(int changes) {
        return new IdentityData(nickname, Math.max(0, changes), titleId, titleText, titleColor, imageFont, imageGlyph);
    }

    public IdentityData withTitle(String id, String text, int color, String font, String glyph) {
        return new IdentityData(nickname, nicknameChanges, id, text, color, font, glyph);
    }

    public IdentityData clearTitle() {
        return new IdentityData(nickname, nicknameChanges, "", "", 0xFFFFFF, "", "");
    }
}
