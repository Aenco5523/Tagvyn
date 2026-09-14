package dev.aenco.tagvyn.api;

/** Immutable public view of a player's Tagvyn identity data. */
public record IdentitySnapshot(
        String nickname,
        int nicknameChanges,
        String titleId,
        String titleText,
        int titleColor,
        String imageFont,
        String imageGlyph
) {
    public boolean hasNickname() {
        return nickname != null && !nickname.isBlank();
    }

    public boolean hasTitle() {
        return titleId != null && !titleId.isBlank();
    }
}
