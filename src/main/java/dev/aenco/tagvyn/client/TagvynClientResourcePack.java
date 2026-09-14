package dev.aenco.tagvyn.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.aenco.tagvyn.Tagvyn;
import dev.aenco.tagvyn.network.TitleImageDataPayload;
import dev.aenco.tagvyn.network.TitleImageManifestPayload;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.AddPackFindersEvent;

/**
 * Builds an always-active client resource pack from PNG files uploaded by server operators.
 * The generated bitmap font is an implementation detail: admins only upload normal PNG files.
 */
public final class TagvynClientResourcePack {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PACK_ROOT = FMLPaths.CONFIGDIR.get()
            .resolve(Tagvyn.MOD_ID)
            .resolve("client-generated-pack");
    private static final Map<String, String> PENDING_GLYPHS = new LinkedHashMap<>();
    private static final Map<String, byte[]> PENDING_IMAGES = new LinkedHashMap<>();

    private TagvynClientResourcePack() {}

    public static void register(IEventBus modBus) {
        ensureBasePack();
        modBus.addListener(TagvynClientResourcePack::onAddPackFinders);
    }

    public static synchronized void beginSync(TitleImageManifestPayload payload) {
        PENDING_GLYPHS.clear();
        PENDING_IMAGES.clear();
        for (TitleImageManifestPayload.Entry entry : payload.entries()) {
            if (entry.id().matches("[a-z0-9_.-]{1,64}") && !entry.glyph().isBlank()) {
                PENDING_GLYPHS.put(entry.id(), entry.glyph());
            }
        }
    }

    public static synchronized void acceptImage(TitleImageDataPayload payload) {
        if (!PENDING_GLYPHS.containsKey(payload.id())) return;
        PENDING_IMAGES.put(payload.id(), payload.png());
    }

    public static synchronized void finishSync() {
        try {
            ensureBasePack();
            Path textureDir = PACK_ROOT.resolve("assets/tagvyn/textures/font/uploaded");
            Path fontDir = PACK_ROOT.resolve("assets/tagvyn/font");
            Files.createDirectories(textureDir);
            Files.createDirectories(fontDir);

            JsonObject root = new JsonObject();
            JsonArray providers = new JsonArray();
            Map<String, byte[]> desiredImages = new LinkedHashMap<>();

            for (Map.Entry<String, String> entry : PENDING_GLYPHS.entrySet()) {
                byte[] png = PENDING_IMAGES.get(entry.getKey());
                if (png == null) continue;

                desiredImages.put(entry.getKey() + ".png", png);

                JsonObject provider = new JsonObject();
                provider.addProperty("type", "bitmap");
                provider.addProperty("file", "tagvyn:font/uploaded/" + entry.getKey() + ".png");
                provider.addProperty("ascent", 9);
                provider.addProperty("height", 10);
                JsonArray chars = new JsonArray();
                chars.add(entry.getValue());
                provider.add("chars", chars);
                providers.add(provider);
            }
            root.add("providers", providers);

            boolean changed = syncImages(textureDir, desiredImages);
            String fontJson = GSON.toJson(root) + System.lineSeparator();
            changed |= writeStringIfChanged(fontDir.resolve("uploaded.json"), fontJson);

            PENDING_GLYPHS.clear();
            PENDING_IMAGES.clear();

            if (changed) {
                Minecraft.getInstance().reloadResourcePacks().exceptionally(exception -> {
                    Tagvyn.LOGGER.error("Failed to reload generated title image pack", exception);
                    return null;
                });
            }
        } catch (Exception exception) {
            Tagvyn.LOGGER.error("Failed to rebuild generated title image pack", exception);
        }
    }

    private static boolean syncImages(Path textureDir, Map<String, byte[]> desiredImages) throws Exception {
        boolean changed = false;

        try (var files = Files.list(textureDir)) {
            for (Path path : files.filter(file -> file.getFileName().toString().endsWith(".png")).toList()) {
                String fileName = path.getFileName().toString();
                byte[] desired = desiredImages.remove(fileName);
                if (desired == null) {
                    Files.deleteIfExists(path);
                    changed = true;
                    continue;
                }

                byte[] current = Files.readAllBytes(path);
                if (!Arrays.equals(current, desired)) {
                    Files.write(path, desired);
                    changed = true;
                }
            }
        }

        for (Map.Entry<String, byte[]> entry : desiredImages.entrySet()) {
            Files.write(textureDir.resolve(entry.getKey()), entry.getValue());
            changed = true;
        }

        return changed;
    }

    private static boolean writeStringIfChanged(Path path, String content) throws Exception {
        if (Files.exists(path) && Files.readString(path, StandardCharsets.UTF_8).equals(content)) {
            return false;
        }
        Files.writeString(path, content, StandardCharsets.UTF_8);
        return true;
    }

    private static void onAddPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.CLIENT_RESOURCES) return;
        ensureBasePack();

        Pack pack = Pack.readMetaAndCreate(
                new PackLocationInfo(
                        "tagvyn_uploaded_titles",
                        Component.literal("Tagvyn Uploaded Titles"),
                        PackSource.BUILT_IN,
                        Optional.empty()
                ),
                new PathPackResources.PathResourcesSupplier(PACK_ROOT),
                PackType.CLIENT_RESOURCES,
                new PackSelectionConfig(true, Pack.Position.TOP, false)
        );
        if (pack != null) {
            event.addRepositorySource(consumer -> consumer.accept(pack));
        }
    }

    private static void ensureBasePack() {
        try {
            Files.createDirectories(PACK_ROOT.resolve("assets/tagvyn/font"));
            Files.createDirectories(PACK_ROOT.resolve("assets/tagvyn/textures/font/uploaded"));

            JsonObject pack = new JsonObject();
            JsonObject metadata = new JsonObject();
            metadata.addProperty(
                    "pack_format",
                    SharedConstants.getCurrentVersion().getPackVersion(PackType.CLIENT_RESOURCES)
            );
            metadata.addProperty("description", "Tagvyn uploaded title images");
            pack.add("pack", metadata);
            Files.writeString(
                    PACK_ROOT.resolve("pack.mcmeta"),
                    GSON.toJson(pack) + System.lineSeparator(),
                    StandardCharsets.UTF_8
            );

            Path font = PACK_ROOT.resolve("assets/tagvyn/font/uploaded.json");
            if (Files.notExists(font)) {
                Files.writeString(font, "{\n  \"providers\": []\n}\n", StandardCharsets.UTF_8);
            }
        } catch (Exception exception) {
            Tagvyn.LOGGER.error("Failed to initialize generated title image pack", exception);
        }
    }
}
