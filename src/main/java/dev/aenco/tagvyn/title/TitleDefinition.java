package dev.aenco.tagvyn.title;

public record TitleDefinition(
        String id,
        String text,
        int color,
        String imageFont,
        String imageGlyph
) {
    public boolean hasImage() {
        return !imageFont.isBlank() && !imageGlyph.isBlank();
    }
}
