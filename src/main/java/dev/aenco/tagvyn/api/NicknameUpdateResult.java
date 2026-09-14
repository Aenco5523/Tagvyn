package dev.aenco.tagvyn.api;

/** Result returned by nickname mutations through the public API. */
public record NicknameUpdateResult(
        boolean success,
        Failure failure,
        String nickname,
        int nicknameChanges,
        int remainingChanges
) {
    public enum Failure {
        NONE,
        INVALID,
        LIMIT_REACHED
    }
}
