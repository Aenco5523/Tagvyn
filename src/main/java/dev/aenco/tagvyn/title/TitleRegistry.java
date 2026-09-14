package dev.aenco.tagvyn.title;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.aenco.tagvyn.Tagvyn;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
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
                    JsonObject object = element.getAsJsonObject();
                    String id = getString(object, "id", "").trim().toLowerCase(Locale.ROOT);
                    if (!id.matches("[a-z0-9_.-]{1,64}")) {
                        Tagvyn.LOGGER.warn("Ignoring invalid title id: {}", id);
                        continue;
                    }
                    String text = getString(object, "text", "");
                    int color = parseColor(getString(object, "color", "#FFFFFF"));
                    String imageFont = "";
                    String imageGlyph = "";
                    if (object.has("image") && object.get("image").isJsonObject()) {
                        JsonObject image = object.getAsJsonObject("image");
                        imageFont = getString(image, "font", "");
                        imageGlyph = getString(image, "glyph", "");
                    }
                    loaded.put(id, new TitleDefinition(id, text, color, imageFont, imageGlyph));
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
        return Optional.ofNullable(TITLES.get(id.toLowerCase(Locale.ROOT)));
    }

    public static synchronized Collection<TitleDefinition> all() {
        return Collections.unmodifiableList(new ArrayList<>(TITLES.values()));
    }

    private static String getString(JsonObject object, String key, String fallback) {
        try {
            return object.has(key) ? object.get(key).getAsString() : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static int parseColor(String value) {
        String normalized = value.trim();
        if (normalized.startsWith("#")) normalized = normalized.substring(1);
        if (!normalized.matches("[0-9a-fA-F]{6}")) return 0xFFFFFF;
        return Integer.parseInt(normalized, 16);
    }

    private static String defaultConfig() throws IOException {
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
