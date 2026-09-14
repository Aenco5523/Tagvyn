package dev.aenco.tagvyn.api;

import java.util.Objects;

/** Public title definition used by the Tagvyn API. */
public record TagvynTitle(
        String id,
        String text,
        int color,
        String imageFont,
        String imageGlyph
) {
    public TagvynTitle {
        id = Objects.requireNonNullElse(id, "");
        text = Objects.requireNonNullElse(text, "");
        color &= 0xFFFFFF;
        imageFont = Objects.requireNonNullElse(imageFont, "");
        imageGlyph = Objects.requireNonNullElse(imageGlyph, "");
    }

    public boolean hasImage() {
        return !imageFont.isBlank() && !imageGlyph.isBlank();
    }
}
