package dev.aenco.tagvyn.client;

import dev.aenco.tagvyn.network.CreateTextTitlePayload;
import dev.aenco.tagvyn.network.DeleteTitlePayload;
import dev.aenco.tagvyn.network.OpenTitleManagerPayload;
import dev.aenco.tagvyn.network.UploadImageTitlePayload;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public final class TitleManagerScreen extends Screen {
    private final OpenTitleManagerPayload payload;
    private EditBox idBox;
    private EditBox textBox;
    private EditBox colorBox;
    private Button createTextButton;
    private Button createImageButton;
    private Button deleteButton;
    private byte[] selectedPng;
    private String selectedFile = "";
    private String localError = "";

    public TitleManagerScreen(OpenTitleManagerPayload payload) {
        super(Component.translatable("tagvyn.gui.titles.title"));
        this.payload = payload;
    }

    @Override
    protected void init() {
        int width = Math.min(280, this.width - 40);
        int x = (this.width - width) / 2;
        int y = Math.max(52, this.height / 2 - 72);

        this.idBox = new EditBox(this.font, x, y, width, 20, Component.translatable("tagvyn.gui.titles.id"));
        this.idBox.setMaxLength(64);
        this.addRenderableWidget(this.idBox);

        this.textBox = new EditBox(this.font, x, y + 28, width, 20, Component.translatable("tagvyn.gui.titles.text"));
        this.textBox.setMaxLength(128);
        this.addRenderableWidget(this.textBox);

        this.colorBox = new EditBox(this.font, x, y + 56, width, 20, Component.translatable("tagvyn.gui.titles.color"));
        this.colorBox.setMaxLength(7);
        this.colorBox.setValue("#FFFFFF");
        this.addRenderableWidget(this.colorBox);

        this.createTextButton = this.addRenderableWidget(Button.builder(
                Component.translatable("tagvyn.gui.titles.create_text"),
                button -> createText()
        ).bounds(x, y + 86, width / 2 - 2, 20).build());

        this.createImageButton = this.addRenderableWidget(Button.builder(
                Component.translatable("tagvyn.gui.titles.create_image"),
                button -> createImage()
        ).bounds(x + width / 2 + 2, y + 86, width / 2 - 2, 20).build());

        this.deleteButton = this.addRenderableWidget(Button.builder(
                Component.translatable("tagvyn.gui.titles.delete"),
                button -> deleteTitle()
        ).bounds(x, y + 112, width / 2 - 2, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("tagvyn.gui.cancel"),
                button -> onClose()
        ).bounds(x + width / 2 + 2, y + 112, width / 2 - 2, 20).build());

        this.idBox.setResponder(value -> updateButtons());
        this.textBox.setResponder(value -> updateButtons());
        this.colorBox.setResponder(value -> updateButtons());
        this.setInitialFocus(this.idBox);
        updateButtons();
    }

    @Override
    public void onFilesDrop(List<Path> files) {
        if (files.isEmpty()) return;
        Path path = files.getFirst();
        try {
            if (!path.getFileName().toString().toLowerCase().endsWith(".png")) {
                setLocalError("tagvyn.gui.titles.error_png_only");
                return;
            }
            byte[] bytes = Files.readAllBytes(path);
            if (!validPng(bytes)) {
                setLocalError("tagvyn.gui.titles.error_png_invalid");
                return;
            }
            this.selectedPng = bytes;
            this.selectedFile = path.getFileName().toString();
            this.localError = "";
            updateButtons();
        } catch (Exception exception) {
            setLocalError("tagvyn.gui.titles.error_read");
        }
    }

    private void createText() {
        Integer color = parseColor();
        if (color == null || !validId() || this.textBox.getValue().trim().isBlank()) return;
        PacketDistributor.sendToServer(new CreateTextTitlePayload(
                this.idBox.getValue().trim(),
                this.textBox.getValue().trim(),
                color
        ));
    }

    private void createImage() {
        Integer color = parseColor();
        if (color == null || !validId() || this.selectedPng == null) return;
        PacketDistributor.sendToServer(new UploadImageTitlePayload(
                this.idBox.getValue().trim(),
                this.textBox.getValue().trim(),
                color,
                this.selectedPng
        ));
    }

    private void deleteTitle() {
        if (!validId()) return;
        PacketDistributor.sendToServer(new DeleteTitlePayload(this.idBox.getValue().trim()));
    }

    private void updateButtons() {
        if (this.createTextButton == null) return;
        boolean id = validId();
        boolean color = parseColor() != null;
        this.createTextButton.active = id && color && !this.textBox.getValue().trim().isBlank();
        this.createImageButton.active = id && color && this.selectedPng != null;
        this.deleteButton.active = id;
    }

    private boolean validId() {
        if (this.idBox == null) return false;
        return this.idBox.getValue().trim().toLowerCase().matches("[a-z0-9_.-]{1,64}");
    }

    private Integer parseColor() {
        if (this.colorBox == null) return null;
        String color = this.colorBox.getValue().trim();
        if (color.startsWith("#")) color = color.substring(1);
        if (!color.matches("[0-9a-fA-F]{6}")) return null;
        return Integer.parseInt(color, 16);
    }

    private void setLocalError(String key) {
        this.selectedPng = null;
        this.selectedFile = "";
        this.localError = key;
        updateButtons();
    }

    private static boolean validPng(byte[] bytes) {
        if (bytes == null || bytes.length < 24 || bytes.length > UploadImageTitlePayload.MAX_PACKET_BYTES) return false;
        int[] signature = {0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        for (int i = 0; i < signature.length; i++) {
            if ((bytes[i] & 0xFF) != signature[i]) return false;
        }
        if (bytes[12] != 'I' || bytes[13] != 'H' || bytes[14] != 'D' || bytes[15] != 'R') return false;
        int width = readInt(bytes, 16);
        int height = readInt(bytes, 20);
        return width > 0 && height > 0 && width <= 256 && height <= 256;
    }

    private static int readInt(byte[] bytes, int offset) {
        return ((bytes[offset] & 0xFF) << 24)
                | ((bytes[offset + 1] & 0xFF) << 16)
                | ((bytes[offset + 2] & 0xFF) << 8)
                | (bytes[offset + 3] & 0xFF);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0xD0101010);
        int formWidth = Math.min(280, this.width - 40);
        int x = (this.width - formWidth) / 2;
        int y = Math.max(52, this.height / 2 - 72);

        graphics.drawCenteredString(this.font, this.title, this.width / 2, 16, 0xFFFFFF);
        graphics.drawString(this.font, Component.translatable("tagvyn.gui.titles.id"), x, y - 10, 0xA0A0A0);
        graphics.drawString(this.font, Component.translatable("tagvyn.gui.titles.text"), x, y + 18, 0xA0A0A0);
        graphics.drawString(this.font, Component.translatable("tagvyn.gui.titles.color"), x, y + 46, 0xA0A0A0);

        Component drop = this.selectedPng == null
                ? Component.translatable("tagvyn.gui.titles.drop_png")
                : Component.translatable("tagvyn.gui.titles.selected_png", this.selectedFile, this.selectedPng.length / 1024);
        graphics.drawCenteredString(this.font, drop, this.width / 2, y + 140, this.selectedPng == null ? 0xB0B0B0 : 0x80FF80);
        if (!this.localError.isBlank()) {
            graphics.drawCenteredString(this.font, Component.translatable(this.localError), this.width / 2, y + 153, 0xFF7070);
        }

        int listY = y + 170;
        graphics.drawString(
                this.font,
                Component.translatable("tagvyn.gui.titles.existing", this.payload.titles().size()),
                12,
                listY,
                0xD0D0D0
        );
        int shown = Math.min(6, this.payload.titles().size());
        for (int i = 0; i < shown; i++) {
            OpenTitleManagerPayload.TitleSummary summary = this.payload.titles().get(i);
            String type = summary.image() ? "IMG" : "TXT";
            String line = "[" + type + "] " + summary.id()
                    + (summary.text().isBlank() ? "" : " - " + summary.text());
            graphics.drawString(this.font, line, 12, listY + 13 + i * 11, summary.color() | 0xFF000000);
        }
        if (this.payload.titles().size() > shown) {
            graphics.drawString(this.font, "... +" + (this.payload.titles().size() - shown), 12, listY + 13 + shown * 11, 0x909090);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
