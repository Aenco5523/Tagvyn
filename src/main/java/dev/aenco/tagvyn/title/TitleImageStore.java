package dev.aenco.tagvyn.title;

import dev.aenco.tagvyn.Tagvyn;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import net.neoforged.fml.loading.FMLPaths;

/** Server-side storage and validation for PNG title images uploaded through Tagvyn. */
public final class TitleImageStore {
    public static final int MAX_IMAGE_BYTES = 512 * 1024;
    public static final int MAX_DIMENSION = 256;

    private static final byte[] PNG_SIGNATURE = new byte[] {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };

    private TitleImageStore() {}

    public static Path directory() {
        return FMLPaths.CONFIGDIR.get().resolve(Tagvyn.MOD_ID).resolve("images");
    }

    public static Path path(String titleId) {
        if (!TitleRegistry.isValidId(titleId)) {
            throw new IllegalArgumentException("Invalid title id: " + titleId);
        }
        return directory().resolve(titleId + ".png");
    }

    public static Validation validate(byte[] png) {
        if (png == null || png.length < 24 || png.length > MAX_IMAGE_BYTES) {
            return Validation.invalid();
        }
        for (int i = 0; i < PNG_SIGNATURE.length; i++) {
            if (png[i] != PNG_SIGNATURE[i]) return Validation.invalid();
        }
        if (png[12] != 'I' || png[13] != 'H' || png[14] != 'D' || png[15] != 'R') {
            return Validation.invalid();
        }

        int width = readInt(png, 16);
        int height = readInt(png, 20);
        if (width < 1 || height < 1 || width > MAX_DIMENSION || height > MAX_DIMENSION) {
            return Validation.invalid();
        }
        return new Validation(true, width, height, png.length);
    }

    public static boolean save(String titleId, byte[] png) {
        if (!validate(png).valid()) return false;
        Path target = path(titleId);
        Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
        try {
            Files.createDirectories(target.getParent());
            Files.write(temporary, png);
            try {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException ignored) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException exception) {
            Tagvyn.LOGGER.error("Failed to save title image {}", titleId, exception);
            try {
                Files.deleteIfExists(temporary);
            } catch (IOException ignored) {}
            return false;
        }
    }

    public static Optional<byte[]> read(String titleId) {
        try {
            Path image = path(titleId);
            if (Files.notExists(image)) return Optional.empty();
            byte[] bytes = Files.readAllBytes(image);
            return validate(bytes).valid() ? Optional.of(bytes) : Optional.empty();
        } catch (Exception exception) {
            Tagvyn.LOGGER.warn("Failed to read title image {}", titleId, exception);
            return Optional.empty();
        }
    }

    public static boolean exists(String titleId) {
        try {
            return Files.isRegularFile(path(titleId));
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    public static void delete(String titleId) {
        try {
            Files.deleteIfExists(path(titleId));
        } catch (Exception exception) {
            Tagvyn.LOGGER.warn("Failed to delete title image {}", titleId, exception);
        }
    }

    private static int readInt(byte[] bytes, int offset) {
        return ((bytes[offset] & 0xFF) << 24)
                | ((bytes[offset + 1] & 0xFF) << 16)
                | ((bytes[offset + 2] & 0xFF) << 8)
                | (bytes[offset + 3] & 0xFF);
    }

    public record Validation(boolean valid, int width, int height, int bytes) {
        private static Validation invalid() {
            return new Validation(false, 0, 0, 0);
        }
    }
}
