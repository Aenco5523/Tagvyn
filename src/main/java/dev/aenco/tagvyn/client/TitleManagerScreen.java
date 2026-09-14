package dev.aenco.tagvyn.client;

import dev.aenco.tagvyn.network.AdminDashboardActionPayload;
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
    private static final int FORM_MAX_WIDTH = 280;
    private static final int LIST_MAX_WIDTH = 280;
    private static final int COLUMN_GAP = 18;

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
        Layout layout = layout();
        int width = layout.formWidth();
        int x = layout.formX();
        int y = layout.formY();

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
                Component.translatable("tagvyn.gui.back"),
                button -> PacketDistributor.sendToServer(new AdminDashboardActionPayload("dashboard"))
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
        Layout layout = layout();
        graphics.fill(0, 0, this.width, this.height, 0xD0101010);

        if (layout.sideBySide()) {
            graphics.fill(
                    layout.listX() - 7,
                    layout.listY() - 7,
                    layout.listX() + layout.listWidth() + 7,
                    layout.listBottom() + 6,
                    0x80181818
            );
        }

        // Widgets first, then labels/helper text so widget backgrounds never hide labels.
        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.drawCenteredString(this.font, this.title, this.width / 2, 16, 0xFFFFFF);
        graphics.drawString(this.font, Component.translatable("tagvyn.gui.titles.id"), layout.formX(), layout.formY() - 10, 0xA0A0A0);
        graphics.drawString(this.font, Component.translatable("tagvyn.gui.titles.text"), layout.formX(), layout.formY() + 18, 0xA0A0A0);
        graphics.drawString(this.font, Component.translatable("tagvyn.gui.titles.color"), layout.formX(), layout.formY() + 46, 0xA0A0A0);

        Component drop = this.selectedPng == null
                ? Component.translatable("tagvyn.gui.titles.drop_png")
                : Component.translatable("tagvyn.gui.titles.selected_png", this.selectedFile, this.selectedPng.length / 1024);
        String dropText = fitToWidth(drop.getString(), layout.formWidth());
        graphics.drawCenteredString(
                this.font,
                dropText,
                layout.formX() + layout.formWidth() / 2,
                layout.formY() + 144,
                this.selectedPng == null ? 0xB0B0B0 : 0x80FF80
        );
        if (!this.localError.isBlank()) {
            String error = fitToWidth(Component.translatable(this.localError).getString(), layout.formWidth());
            graphics.drawCenteredString(
                    this.font,
                    error,
                    layout.formX() + layout.formWidth() / 2,
                    layout.formY() + 157,
                    0xFF7070
            );
        }

        renderTitleList(graphics, layout);
    }

    private void renderTitleList(GuiGraphics graphics, Layout layout) {
        graphics.drawString(
                this.font,
                Component.translatable("tagvyn.gui.titles.existing", this.payload.titles().size()),
                layout.listX(),
                layout.listY(),
                0xD0D0D0
        );

        int rowY = layout.listY() + 14;
        int availableRows = Math.max(0, (layout.listBottom() - rowY) / 12);
        boolean needsOverflowRow = this.payload.titles().size() > availableRows && availableRows > 0;
        int dataRows = needsOverflowRow ? availableRows - 1 : availableRows;
        int shown = Math.min(Math.max(0, dataRows), this.payload.titles().size());
        int textWidth = Math.max(24, layout.listWidth());

        for (int i = 0; i < shown; i++) {
            OpenTitleManagerPayload.TitleSummary summary = this.payload.titles().get(i);
            String type = summary.image() ? "IMG" : "TXT";
            String line = "[" + type + "] " + summary.id()
                    + (summary.text().isBlank() ? "" : " - " + summary.text());
            graphics.drawString(
                    this.font,
                    fitToWidth(line, textWidth),
                    layout.listX(),
                    rowY + i * 12,
                    summary.color() | 0xFF000000
            );
        }

        int remaining = this.payload.titles().size() - shown;
        if (remaining > 0 && needsOverflowRow) {
            graphics.drawString(
                    this.font,
                    fitToWidth("… +" + remaining, textWidth),
                    layout.listX(),
                    rowY + shown * 12,
                    0x909090
            );
        }
    }

    private String fitToWidth(String text, int maxWidth) {
        if (text == null || text.isEmpty() || this.font.width(text) <= maxWidth) return text == null ? "" : text;
        String ellipsis = "…";
        if (maxWidth <= this.font.width(ellipsis)) return ellipsis;

        int end = text.length();
        while (end > 0) {
            int previous = text.offsetByCodePoints(end, -1);
            String candidate = text.substring(0, previous) + ellipsis;
            if (this.font.width(candidate) <= maxWidth) return candidate;
            end = previous;
        }
        return ellipsis;
    }

    private Layout layout() {
        boolean sideBySide = this.width >= 560;
        int formWidth;
        int formX;
        int formY = Math.max(48, Math.min(this.height / 2 - 72, this.height - 180));
        formY = Math.max(42, formY);

        if (sideBySide) {
            int usableWidth = Math.max(1, this.width - 36 - COLUMN_GAP);
            formWidth = Math.min(FORM_MAX_WIDTH, usableWidth / 2);
            int listWidth = Math.min(LIST_MAX_WIDTH, usableWidth - formWidth);
            int totalWidth = formWidth + COLUMN_GAP + listWidth;
            formX = Math.max(12, (this.width - totalWidth) / 2);
            int listX = formX + formWidth + COLUMN_GAP;
            int listY = Math.max(42, formY - 10);
            int listBottom = Math.max(listY + 26, Math.min(this.height - 16, formY + 162));
            return new Layout(formX, formY, formWidth, listX, listY, listWidth, listBottom, true);
        }

        formWidth = Math.max(80, Math.min(FORM_MAX_WIDTH, this.width - 40));
        formX = (this.width - formWidth) / 2;
        int listX = Math.max(12, formX);
        int listY = formY + 176;
        int listWidth = Math.max(24, Math.min(formWidth, this.width - listX - 12));
        int listBottom = Math.max(listY + 10, this.height - 10);
        return new Layout(formX, formY, formWidth, listX, listY, listWidth, listBottom, false);
    }

    private record Layout(
            int formX,
            int formY,
            int formWidth,
            int listX,
            int listY,
            int listWidth,
            int listBottom,
            boolean sideBySide
    ) {}

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
