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

            try (var files = Files.list(textureDir)) {
                files.filter(path -> path.getFileName().toString().endsWith(".png"))
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (Exception exception) {
                                Tagvyn.LOGGER.warn("Failed to clear generated title texture {}", path, exception);
                            }
                        });
            }

            JsonObject root = new JsonObject();
            JsonArray providers = new JsonArray();
            for (Map.Entry<String, String> entry : PENDING_GLYPHS.entrySet()) {
                byte[] png = PENDING_IMAGES.get(entry.getKey());
                if (png == null) continue;
                Files.write(textureDir.resolve(entry.getKey() + ".png"), png);

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
            Files.writeString(
                    fontDir.resolve("uploaded.json"),
                    GSON.toJson(root) + System.lineSeparator(),
                    StandardCharsets.UTF_8
            );

            Minecraft.getInstance().reloadResourcePacks();
        } catch (Exception exception) {
            Tagvyn.LOGGER.error("Failed to rebuild generated title image pack", exception);
        }
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
