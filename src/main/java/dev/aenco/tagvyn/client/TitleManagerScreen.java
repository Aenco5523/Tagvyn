package dev.aenco.tagvyn.client;

import dev.aenco.tagvyn.network.AdminDashboardActionPayload;
import dev.aenco.tagvyn.network.CreateTextTitlePayload;
import dev.aenco.tagvyn.network.DeleteTitlePayload;
import dev.aenco.tagvyn.network.OpenTitleManagerPayload;
import dev.aenco.tagvyn.network.UpdateTitlePayload;
import dev.aenco.tagvyn.network.UploadImageTitlePayload;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public final class TitleManagerScreen extends Screen {
    private static final int FORM_MAX_WIDTH = 280;
    private static final int LIST_MAX_WIDTH = 300;
    private static final int COLUMN_GAP = 18;
    private static final int ROW_HEIGHT = 22;

    private final OpenTitleManagerPayload payload;
    private String selectedTitleId = "";
    private int titleScroll;

    private EditBox idBox;
    private EditBox textBox;
    private EditBox colorBox;
    private EditBox searchBox;
    private Button primarySaveButton;
    private Button saveImageButton;
    private Button newButton;
    private Button deleteButton;
    private byte[] selectedPng;
    private String selectedFile = "";
    private String localError = "";

    public TitleManagerScreen(OpenTitleManagerPayload payload) {
        super(Component.translatable("tagvyn.gui.titles.title"));
        this.payload = payload;
        this.selectedTitleId = payload.selectedTitleId();
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

        int half = width / 2;
        this.primarySaveButton = this.addRenderableWidget(Button.builder(
                Component.translatable("tagvyn.gui.titles.create_text"),
                button -> saveMetadataOrCreateText()
        ).bounds(x, y + 86, half - 2, 20).build());

        this.saveImageButton = this.addRenderableWidget(Button.builder(
                Component.translatable("tagvyn.gui.titles.save_image"),
                button -> saveImage()
        ).bounds(x + half + 2, y + 86, width - half - 2, 20).build());

        this.newButton = this.addRenderableWidget(Button.builder(
                Component.translatable("tagvyn.gui.titles.new"),
                button -> newTitle()
        ).bounds(x, y + 112, half - 2, 20).build());

        this.deleteButton = this.addRenderableWidget(Button.builder(
                Component.translatable("tagvyn.gui.titles.delete_selected"),
                button -> deleteSelectedTitle()
        ).bounds(x + half + 2, y + 112, width - half - 2, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("tagvyn.gui.back"),
                button -> PacketDistributor.sendToServer(new AdminDashboardActionPayload("dashboard"))
        ).bounds(x, y + 138, width, 20).build());

        this.searchBox = new EditBox(
                this.font,
                layout.listX(),
                layout.searchY(),
                layout.listWidth(),
                20,
                Component.translatable("tagvyn.gui.titles.search")
        );
        this.searchBox.setMaxLength(64);
        this.searchBox.setResponder(value -> {
            this.titleScroll = 0;
            clampScroll();
        });
        this.addRenderableWidget(this.searchBox);

        this.idBox.setResponder(value -> updateButtons());
        this.textBox.setResponder(value -> updateButtons());
        this.colorBox.setResponder(value -> updateButtons());

        if (!this.selectedTitleId.isBlank() && findTitle(this.selectedTitleId) != null) {
            selectTitle(this.selectedTitleId);
        } else {
            newTitle();
        }
        updateButtons();
    }

    @Override
    public void onFilesDrop(List<Path> files) {
        if (files.isEmpty()) return;
        Path path = files.getFirst();
        try {
            if (!path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png")) {
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

    private void saveMetadataOrCreateText() {
        Integer color = parseColor();
        if (color == null) return;
        String text = this.textBox.getValue().trim();

        if (!this.selectedTitleId.isBlank()) {
            PacketDistributor.sendToServer(new UpdateTitlePayload(this.selectedTitleId, text, color));
            return;
        }

        if (!validId() || text.isBlank()) return;
        PacketDistributor.sendToServer(new CreateTextTitlePayload(
                this.idBox.getValue().trim(),
                text,
                color
        ));
    }

    private void saveImage() {
        Integer color = parseColor();
        if (color == null || this.selectedPng == null) return;
        String id = this.selectedTitleId.isBlank() ? this.idBox.getValue().trim() : this.selectedTitleId;
        if (!validIdValue(id)) return;
        PacketDistributor.sendToServer(new UploadImageTitlePayload(
                id,
                this.textBox.getValue().trim(),
                color,
                this.selectedPng
        ));
    }

    private void newTitle() {
        this.selectedTitleId = "";
        this.selectedPng = null;
        this.selectedFile = "";
        this.localError = "";
        if (this.idBox != null) {
            this.idBox.setEditable(true);
            this.idBox.setValue("");
        }
        if (this.textBox != null) this.textBox.setValue("");
        if (this.colorBox != null) this.colorBox.setValue("#FFFFFF");
        updateButtons();
    }

    private void selectTitle(String id) {
        OpenTitleManagerPayload.TitleSummary summary = findTitle(id);
        if (summary == null) return;
        this.selectedTitleId = summary.id();
        this.selectedPng = null;
        this.selectedFile = "";
        this.localError = "";
        if (this.idBox != null) {
            this.idBox.setValue(summary.id());
            this.idBox.setEditable(false);
        }
        if (this.textBox != null) this.textBox.setValue(summary.text());
        if (this.colorBox != null) this.colorBox.setValue(String.format("#%06X", summary.color() & 0xFFFFFF));
        updateButtons();
    }

    private void deleteSelectedTitle() {
        if (this.selectedTitleId.isBlank()) return;
        PacketDistributor.sendToServer(new DeleteTitlePayload(this.selectedTitleId));
    }

    private OpenTitleManagerPayload.TitleSummary findTitle(String id) {
        if (id == null || id.isBlank()) return null;
        for (OpenTitleManagerPayload.TitleSummary title : this.payload.titles()) {
            if (title.id().equalsIgnoreCase(id)) return title;
        }
        return null;
    }

    private List<OpenTitleManagerPayload.TitleSummary> filteredTitles() {
        String query = this.searchBox == null ? "" : this.searchBox.getValue().trim().toLowerCase(Locale.ROOT);
        if (query.isBlank()) return this.payload.titles();
        return this.payload.titles().stream()
                .filter(title -> title.id().toLowerCase(Locale.ROOT).contains(query)
                        || title.text().toLowerCase(Locale.ROOT).contains(query))
                .toList();
    }

    private void updateButtons() {
        if (this.primarySaveButton == null) return;
        boolean selected = !this.selectedTitleId.isBlank();
        boolean id = selected || validId();
        boolean color = parseColor() != null;
        boolean text = this.textBox != null && !this.textBox.getValue().trim().isBlank();
        OpenTitleManagerPayload.TitleSummary selectedSummary = findTitle(this.selectedTitleId);
        boolean selectedImage = selectedSummary != null && selectedSummary.image();

        this.primarySaveButton.setMessage(Component.translatable(
                selected ? "tagvyn.gui.titles.save_changes" : "tagvyn.gui.titles.create_text"
        ));
        this.primarySaveButton.active = id && color && (selectedImage || text);
        this.saveImageButton.active = id && color && this.selectedPng != null;
        this.newButton.active = selected || (this.idBox != null && !this.idBox.getValue().isBlank())
                || (this.textBox != null && !this.textBox.getValue().isBlank())
                || this.selectedPng != null;
        this.deleteButton.active = selected;
    }

    private boolean validId() {
        return this.idBox != null && validIdValue(this.idBox.getValue());
    }

    private static boolean validIdValue(String value) {
        return value != null && value.trim().toLowerCase(Locale.ROOT).matches("[a-z0-9_.-]{1,64}");
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

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        Layout layout = layout();
        if (!inside(mouseX, mouseY, layout.listX(), layout.rowsY(), layout.listWidth(), layout.rowsHeight())) return false;

        List<OpenTitleManagerPayload.TitleSummary> titles = filteredTitles();
        int row = ((int) mouseY - layout.rowsY()) / ROW_HEIGHT;
        int index = this.titleScroll + row;
        if (index >= 0 && index < titles.size()) {
            selectTitle(titles.get(index).id());
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        Layout layout = layout();
        if (inside(mouseX, mouseY, layout.listX(), layout.rowsY(), layout.listWidth(), layout.rowsHeight())) {
            this.titleScroll -= (int) Math.signum(scrollY);
            clampScroll();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void clampScroll() {
        Layout layout = layout();
        int visible = Math.max(1, layout.rowsHeight() / ROW_HEIGHT);
        this.titleScroll = Math.max(0, Math.min(this.titleScroll, Math.max(0, filteredTitles().size() - visible)));
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
        graphics.fill(layout.formX() - 7, 39, layout.formX() + layout.formWidth() + 7, layout.formBottom() + 6, 0x70181818);
        graphics.fill(layout.listX() - 7, layout.listTop() - 7, layout.listX() + layout.listWidth() + 7, layout.listBottom() + 6, 0x70181818);

        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.drawCenteredString(this.font, this.title, this.width / 2, 14, 0xFFFFFF);
        graphics.drawString(this.font, Component.translatable("tagvyn.gui.titles.id"), layout.formX(), layout.formY() - 10, 0xA0A0A0);
        graphics.drawString(this.font, Component.translatable("tagvyn.gui.titles.text"), layout.formX(), layout.formY() + 18, 0xA0A0A0);
        graphics.drawString(this.font, Component.translatable("tagvyn.gui.titles.color"), layout.formX(), layout.formY() + 46, 0xA0A0A0);

        String selected = this.selectedTitleId.isBlank()
                ? Component.translatable("tagvyn.gui.titles.new_mode").getString()
                : Component.translatable("tagvyn.gui.titles.selected", this.selectedTitleId).getString();
        graphics.drawString(this.font, fitToWidth(selected, layout.formWidth()), layout.formX(), layout.formY() + 164, 0xD0D0D0);

        Component drop = this.selectedPng == null
                ? Component.translatable("tagvyn.gui.titles.drop_png_short")
                : Component.translatable("tagvyn.gui.titles.selected_png", this.selectedFile, this.selectedPng.length / 1024);
        graphics.drawString(
                this.font,
                fitToWidth(drop.getString(), layout.formWidth()),
                layout.formX(),
                layout.formY() + 176,
                this.selectedPng == null ? 0x909090 : 0x80FF80
        );
        if (!this.localError.isBlank()) {
            graphics.drawString(
                    this.font,
                    fitToWidth(Component.translatable(this.localError).getString(), layout.formWidth()),
                    layout.formX(),
                    layout.formY() + 188,
                    0xFF7070
            );
        }

        graphics.drawString(
                this.font,
                Component.translatable("tagvyn.gui.titles.existing", this.payload.titles().size()),
                layout.listX(),
                layout.listTop(),
                0xD0D0D0
        );
        graphics.drawString(this.font, Component.translatable("tagvyn.gui.titles.search"), layout.listX(), layout.searchY() - 10, 0xA0A0A0);
        renderTitleList(graphics, layout);
    }

    private void renderTitleList(GuiGraphics graphics, Layout layout) {
        List<OpenTitleManagerPayload.TitleSummary> titles = filteredTitles();
        int visible = Math.max(1, layout.rowsHeight() / ROW_HEIGHT);
        int end = Math.min(titles.size(), this.titleScroll + visible);

        if (titles.isEmpty()) {
            graphics.drawCenteredString(
                    this.font,
                    Component.translatable("tagvyn.gui.titles.no_matches"),
                    layout.listX() + layout.listWidth() / 2,
                    layout.rowsY() + 8,
                    0x909090
            );
            return;
        }

        for (int index = this.titleScroll; index < end; index++) {
            OpenTitleManagerPayload.TitleSummary summary = titles.get(index);
            int rowY = layout.rowsY() + (index - this.titleScroll) * ROW_HEIGHT;
            if (summary.id().equalsIgnoreCase(this.selectedTitleId)) {
                graphics.fill(layout.listX(), rowY, layout.listX() + layout.listWidth(), rowY + ROW_HEIGHT - 2, 0x90606060);
            } else if (index % 2 == 0) {
                graphics.fill(layout.listX(), rowY, layout.listX() + layout.listWidth(), rowY + ROW_HEIGHT - 2, 0x40282828);
            }

            String type = summary.image() ? "IMG" : "TXT";
            String first = "[" + type + "] " + summary.id();
            graphics.drawString(this.font, fitToWidth(first, layout.listWidth() - 8), layout.listX() + 4, rowY + 3, summary.color() | 0xFF000000);
            String second = summary.text().isBlank() ? Component.translatable("tagvyn.gui.titles.no_text").getString() : summary.text();
            graphics.drawString(this.font, fitToWidth(second, layout.listWidth() - 8), layout.listX() + 4, rowY + 12, 0x909090);
        }
    }

    private String fitToWidth(String text, int maxWidth) {
        if (text == null || text.isEmpty() || this.font.width(text) <= maxWidth) return text == null ? "" : text;
        String ellipsis = "…";
        int end = text.length();
        while (end > 0) {
            int previous = text.offsetByCodePoints(end, -1);
            String candidate = text.substring(0, previous) + ellipsis;
            if (this.font.width(candidate) <= maxWidth) return candidate;
            end = previous;
        }
        return ellipsis;
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private Layout layout() {
        boolean sideBySide = this.width >= 560;
        int formWidth;
        int formX;
        int formY = 52;

        if (sideBySide) {
            int usableWidth = Math.max(1, this.width - 36 - COLUMN_GAP);
            formWidth = Math.min(FORM_MAX_WIDTH, usableWidth / 2);
            int listWidth = Math.min(LIST_MAX_WIDTH, usableWidth - formWidth);
            int totalWidth = formWidth + COLUMN_GAP + listWidth;
            formX = Math.max(12, (this.width - totalWidth) / 2);
            int listX = formX + formWidth + COLUMN_GAP;
            int listTop = 48;
            int searchY = 68;
            int rowsY = 92;
            int listBottom = Math.max(rowsY + ROW_HEIGHT, this.height - 14);
            return new Layout(formX, formY, formWidth, formY + 202, listX, listTop, searchY, rowsY, listWidth, listBottom, true);
        }

        formWidth = Math.max(120, Math.min(FORM_MAX_WIDTH, this.width - 40));
        formX = (this.width - formWidth) / 2;
        int listX = formX;
        int listTop = formY + 216;
        int searchY = listTop + 20;
        int rowsY = searchY + 24;
        int listBottom = Math.max(rowsY + ROW_HEIGHT, this.height - 10);
        return new Layout(formX, formY, formWidth, formY + 202, listX, listTop, searchY, rowsY, formWidth, listBottom, false);
    }

    private record Layout(
            int formX,
            int formY,
            int formWidth,
            int formBottom,
            int listX,
            int listTop,
            int searchY,
            int rowsY,
            int listWidth,
            int listBottom,
            boolean sideBySide
    ) {
        int rowsHeight() {
            return Math.max(ROW_HEIGHT, this.listBottom - this.rowsY);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
