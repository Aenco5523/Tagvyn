package dev.aenco.tagvyn.title;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.aenco.tagvyn.Tagvyn;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.neoforged.fml.loading.FMLPaths;

public final class TitleRegistry {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, TitleDefinition> TITLES = new LinkedHashMap<>();

    private TitleRegistry() {}

    public static Path configPath() {
        return FMLPaths.CONFIGDIR.get().resolve(Tagvyn.MOD_ID).resolve("titles.json");
    }

    public static synchronized void load() {
        Path path = configPath();
        try {
            Files.createDirectories(path.getParent());
            if (Files.notExists(path)) {
                Files.writeString(path, defaultConfig(), StandardCharsets.UTF_8);
            }

            JsonObject root = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonArray titles = root.getAsJsonArray("titles");
            Map<String, TitleDefinition> loaded = new LinkedHashMap<>();
            if (titles != null) {
                for (JsonElement element : titles) {
                    if (!element.isJsonObject()) continue;
                    TitleDefinition definition = readDefinition(element.getAsJsonObject());
                    if (definition == null) continue;
                    loaded.put(definition.id(), definition);
                }
            }

            TITLES.clear();
            TITLES.putAll(loaded);
            Tagvyn.LOGGER.info("Loaded {} Tagvyn title definitions", TITLES.size());
        } catch (Exception exception) {
            Tagvyn.LOGGER.error("Failed to load {}", path, exception);
        }
    }

    public static synchronized Optional<TitleDefinition> get(String id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(TITLES.get(normalizeId(id)));
    }

    public static synchronized Collection<TitleDefinition> all() {
        return Collections.unmodifiableList(new ArrayList<>(TITLES.values()));
    }

    public static synchronized boolean register(TitleDefinition definition, boolean overwrite) {
        TitleDefinition normalized = normalize(definition);
        if (normalized == null) return false;
        if (!overwrite && TITLES.containsKey(normalized.id())) return false;

        TitleDefinition previous = TITLES.put(normalized.id(), normalized);
        if (!save()) {
            if (previous == null) TITLES.remove(normalized.id());
            else TITLES.put(normalized.id(), previous);
            return false;
        }
        return true;
    }

    public static synchronized boolean remove(String id) {
        String normalizedId = normalizeId(id);
        TitleDefinition removed = TITLES.remove(normalizedId);
        if (removed == null) return false;
        if (!save()) {
            TITLES.put(normalizedId, removed);
            return false;
        }
        return true;
    }

    public static boolean isValidId(String id) {
        return id != null && normalizeId(id).matches("[a-z0-9_.-]{1,64}");
    }

    private static boolean save() {
        Path path = configPath();
        try {
            Files.createDirectories(path.getParent());
            JsonObject root = new JsonObject();
            JsonArray titles = new JsonArray();
            for (TitleDefinition definition : TITLES.values()) {
                titles.add(writeDefinition(definition));
            }
            root.add("titles", titles);
            Files.writeString(path, GSON.toJson(root) + System.lineSeparator(), StandardCharsets.UTF_8);
            return true;
        } catch (Exception exception) {
            Tagvyn.LOGGER.error("Failed to save {}", path, exception);
            return false;
        }
    }

    private static TitleDefinition readDefinition(JsonObject object) {
        String id = normalizeId(getString(object, "id", ""));
        String text = getString(object, "text", "");
        int color = parseColor(getString(object, "color", "#FFFFFF"));
        String imageFont = "";
        String imageGlyph = "";
        if (object.has("image") && object.get("image").isJsonObject()) {
            JsonObject image = object.getAsJsonObject("image");
            imageFont = getString(image, "font", "");
            imageGlyph = getString(image, "glyph", "");
        }

        TitleDefinition normalized = normalize(new TitleDefinition(id, text, color, imageFont, imageGlyph));
        if (normalized == null) {
            Tagvyn.LOGGER.warn("Ignoring invalid title definition: {}", id);
        }
        return normalized;
    }

    private static JsonObject writeDefinition(TitleDefinition definition) {
        JsonObject object = new JsonObject();
        object.addProperty("id", definition.id());
        object.addProperty("text", definition.text());
        object.addProperty("color", String.format("#%06X", definition.color() & 0xFFFFFF));
        JsonObject image = new JsonObject();
        image.addProperty("font", definition.imageFont());
        image.addProperty("glyph", definition.imageGlyph());
        object.add("image", image);
        return object;
    }

    private static TitleDefinition normalize(TitleDefinition definition) {
        if (definition == null) return null;
        String id = normalizeId(definition.id());
        if (!isValidId(id)) return null;

        String text = Objects.requireNonNullElse(definition.text(), "").trim();
        String imageFont = Objects.requireNonNullElse(definition.imageFont(), "").trim();
        String imageGlyph = Objects.requireNonNullElse(definition.imageGlyph(), "").trim();
        boolean hasAnyImagePart = !imageFont.isBlank() || !imageGlyph.isBlank();
        boolean hasCompleteImage = !imageFont.isBlank() && !imageGlyph.isBlank();
        if (hasAnyImagePart && !hasCompleteImage) return null;
        if (text.isBlank() && !hasCompleteImage) return null;

        return new TitleDefinition(id, text, definition.color() & 0xFFFFFF, imageFont, imageGlyph);
    }

    private static String normalizeId(String id) {
        return Objects.requireNonNullElse(id, "").trim().toLowerCase(Locale.ROOT);
    }

    private static String getString(JsonObject object, String key, String fallback) {
        try {
            return object.has(key) ? object.get(key).getAsString() : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static int parseColor(String value) {
        String normalized = Objects.requireNonNullElse(value, "").trim();
        if (normalized.startsWith("#")) normalized = normalized.substring(1);
        if (!normalized.matches("[0-9a-fA-F]{6}")) return 0xFFFFFF;
        return Integer.parseInt(normalized, 16);
    }

    private static String defaultConfig() {
        JsonObject root = new JsonObject();
        JsonArray titles = new JsonArray();

        JsonObject example = new JsonObject();
        example.addProperty("id", "founder");
        example.addProperty("text", "FOUNDER");
        example.addProperty("color", "#FFB347");
        JsonObject image = new JsonObject();
        image.addProperty("font", "");
        image.addProperty("glyph", "");
        example.add("image", image);
        titles.add(example);

        root.add("titles", titles);
        return GSON.toJson(root) + System.lineSeparator();
    }
}
