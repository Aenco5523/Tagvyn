package dev.aenco.tagvyn.api;

import java.util.Objects;

/** Entry point for integrations from other mods. */
public final class TagvynAPI {
    public static final String API_VERSION = "1";

    private static volatile TagvynApi instance;

    private TagvynAPI() {}

    public static TagvynApi get() {
        TagvynApi api = instance;
        if (api == null) {
            throw new IllegalStateException("Tagvyn API has not been initialized yet");
        }
        return api;
    }

    public static boolean isReady() {
        return instance != null;
    }

    /** Internal bootstrap hook used by the platform implementation. */
    public static synchronized void bootstrap(TagvynApi api) {
        Objects.requireNonNull(api, "api");
        if (instance != null && instance != api) {
            throw new IllegalStateException("Tagvyn API is already initialized");
        }
        instance = api;
    }
}
