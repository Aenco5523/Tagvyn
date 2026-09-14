package dev.aenco.tagvyn.client;

import dev.aenco.tagvyn.network.AdminDashboardActionPayload;
import dev.aenco.tagvyn.network.CreateTextTitlePayload;
import dev.aenco.tagvyn.network.DeleteTitlePayload;
import dev.aenco.tagvyn.network.GiveTitleItemPayload;
import dev.aenco.tagvyn.network.OpenTitleManagerPayload;
import dev.aenco.tagvyn.network.UpdateTitlePayload;
import dev.aenco.tagvyn.network.UploadImageTitlePayload;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public final class TitleManagerScreen extends Screen {
    private static final int ROW_HEIGHT = 22;
    private static final int CONTROL_HEIGHT = 20;
    private static final int CONTROL_GAP = 5;
    private static final int LABEL_GAP = 3;
    private static final int SECTION_GAP = 12;
    private static final int SCROLL_STEP = 24;

    private final OpenTitleManagerPayload payload;
    private String selectedTitleId = "";
    private int titleScroll;
    private int editorScroll;

    private EditBox idBox;
    private EditBox textBox;
    private EditBox colorBox;
    private EditBox searchBox;
    private Button primarySaveButton;
    private Button saveImageButton;
    private Button newButton;
    private Button deleteButton;
    private Button titleItemButton;
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

        this.searchBox = new EditBox(
                this.font,
                layout.listX(),
                layout.searchY(),
                layout.listWidth(),
                CONTROL_HEIGHT,
                Component.translatable("tagvyn.gui.titles.search")
        );
        this.searchBox.setMaxLength(64);
        this.searchBox.setResponder(value -> {
            this.titleScroll = 0;
            clampTitleScroll();
        });
        this.addRenderableWidget(this.searchBox);

        this.idBox = new EditBox(
                this.font,
                layout.editorX(),
                editorScreenY(layout, layout.idY()),
                layout.editorContentWidth(),
                CONTROL_HEIGHT,
                Component.translatable("tagvyn.gui.titles.id")
        );
        this.idBox.setMaxLength(64);
        this.addRenderableWidget(this.idBox);

        this.textBox = new EditBox(
                this.font,
                layout.editorX(),
                editorScreenY(layout, layout.textY()),
                layout.editorContentWidth(),
                CONTROL_HEIGHT,
                Component.translatable("tagvyn.gui.titles.text")
        );
        this.textBox.setMaxLength(128);
        this.addRenderableWidget(this.textBox);

        this.colorBox = new EditBox(
                this.font,
                layout.editorX(),
                editorScreenY(layout, layout.colorY()),
                layout.editorContentWidth(),
                CONTROL_HEIGHT,
                Component.translatable("tagvyn.gui.titles.color")
        );
        this.colorBox.setMaxLength(7);
        this.colorBox.setValue("#FFFFFF");
        this.addRenderableWidget(this.colorBox);

        this.primarySaveButton = this.addRenderableWidget(Button.builder(
                Component.translatable("tagvyn.gui.titles.create_text"),
                button -> saveMetadataOrCreateText()
        ).bounds(
                layout.editorX(),
                editorScreenY(layout, layout.primarySaveY()),
                layout.editorContentWidth(),
                CONTROL_HEIGHT
        ).build());

        this.saveImageButton = this.addRenderableWidget(Button.builder(
                Component.translatable("tagvyn.gui.titles.save_image"),
                button -> saveImage()
        ).bounds(
                layout.editorX(),
                editorScreenY(layout, layout.saveImageY()),
                layout.editorContentWidth(),
                CONTROL_HEIGHT
        ).build());

        this.newButton = this.addRenderableWidget(Button.builder(
                Component.translatable("tagvyn.gui.titles.new"),
                button -> newTitle()
        ).bounds(
                layout.editorX(),
                editorScreenY(layout, layout.newY()),
                layout.editorContentWidth(),
                CONTROL_HEIGHT
        ).build());

        this.deleteButton = this.addRenderableWidget(Button.builder(
                Component.translatable("tagvyn.gui.titles.delete_selected"),
                button -> deleteSelectedTitle()
        ).bounds(
                layout.editorX(),
                editorScreenY(layout, layout.deleteY()),
                layout.editorContentWidth(),
                CONTROL_HEIGHT
        ).build());

        this.titleItemButton = this.addRenderableWidget(Button.builder(
                Component.translatable("tagvyn.gui.titles.give_item"),
                button -> giveTitleItem()
        ).bounds(
                layout.editorX(),
                editorScreenY(layout, layout.itemY()),
                layout.editorContentWidth(),
                CONTROL_HEIGHT
        ).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("tagvyn.gui.back"),
                button -> PacketDistributor.sendToServer(new AdminDashboardActionPayload("dashboard"))
        ).bounds(layout.editorX(), layout.backY(), layout.editorWidth(), CONTROL_HEIGHT).build());

        this.idBox.setResponder(value -> updateButtons());
        this.textBox.setResponder(value -> updateButtons());
        this.colorBox.setResponder(value -> updateButtons());

        if (!this.selectedTitleId.isBlank() && findTitle(this.selectedTitleId) != null) {
            selectTitle(this.selectedTitleId);
        } else {
            newTitle();
        }
        clampEditorScroll();
        updateEditorWidgets();
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

    private void giveTitleItem() {
        if (this.selectedTitleId.isBlank()) return;
        PacketDistributor.sendToServer(new GiveTitleItemPayload(this.selectedTitleId));
    }

    private void newTitle() {
        this.selectedTitleId = "";
        this.selectedPng = null;
        this.selectedFile = "";
        this.localError = "";
        this.editorScroll = 0;
        if (this.idBox != null) {
            this.idBox.setEditable(true);
            this.idBox.setValue("");
        }
        if (this.textBox != null) this.textBox.setValue("");
        if (this.colorBox != null) this.colorBox.setValue("#FFFFFF");
        updateEditorWidgets();
        updateButtons();
    }

    private void selectTitle(String id) {
        OpenTitleManagerPayload.TitleSummary summary = findTitle(id);
        if (summary == null) return;
        this.selectedTitleId = summary.id();
        this.selectedPng = null;
        this.selectedFile = "";
        this.localError = "";
        this.editorScroll = 0;
        if (this.idBox != null) {
            this.idBox.setValue(summary.id());
            this.idBox.setEditable(false);
        }
        if (this.textBox != null) this.textBox.setValue(summary.text());
        if (this.colorBox != null) this.colorBox.setValue(String.format("#%06X", summary.color() & 0xFFFFFF));
        updateEditorWidgets();
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
        this.titleItemButton.active = selected;
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
            clampTitleScroll();
            return true;
        }
        if (inside(mouseX, mouseY, layout.editorX(), layout.viewportTop(), layout.editorWidth(), layout.viewportHeight())) {
            this.editorScroll -= (int) Math.signum(scrollY) * SCROLL_STEP;
            clampEditorScroll();
            updateEditorWidgets();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void clampTitleScroll() {
        Layout layout = layout();
        int visible = Math.max(1, layout.rowsHeight() / ROW_HEIGHT);
        this.titleScroll = Math.max(0, Math.min(this.titleScroll, Math.max(0, filteredTitles().size() - visible)));
    }

    private void clampEditorScroll() {
        Layout layout = layout();
        this.editorScroll = Math.max(0, Math.min(this.editorScroll, Math.max(0, layout.contentHeight() - layout.viewportHeight())));
    }

    private int editorScreenY(Layout layout, int contentY) {
        return layout.viewportTop() + contentY - this.editorScroll;
    }

    private void updateEditorWidgets() {
        Layout layout = layout();
        placeEditorWidget(this.idBox, editorScreenY(layout, layout.idY()), layout);
        placeEditorWidget(this.textBox, editorScreenY(layout, layout.textY()), layout);
        placeEditorWidget(this.colorBox, editorScreenY(layout, layout.colorY()), layout);
        placeEditorWidget(this.primarySaveButton, editorScreenY(layout, layout.primarySaveY()), layout);
        placeEditorWidget(this.saveImageButton, editorScreenY(layout, layout.saveImageY()), layout);
        placeEditorWidget(this.newButton, editorScreenY(layout, layout.newY()), layout);
        placeEditorWidget(this.deleteButton, editorScreenY(layout, layout.deleteY()), layout);
        placeEditorWidget(this.titleItemButton, editorScreenY(layout, layout.itemY()), layout);
    }

    private void placeEditorWidget(AbstractWidget widget, int y, Layout layout) {
        if (widget == null) return;
        widget.setY(y);
        boolean visible = y >= layout.viewportTop() && y + widget.getHeight() <= layout.viewportBottom();
        widget.visible = visible;
        if (!visible) widget.setFocused(false);
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
        clampEditorScroll();
        updateEditorWidgets();

        graphics.fill(0, 0, this.width, this.height, 0xD0101010);
        graphics.fill(layout.listX() - 6, layout.panelTop(), layout.listX() + layout.listWidth() + 6, layout.bottom() + 5, 0x70181818);
        graphics.fill(layout.editorX() - 6, layout.panelTop(), layout.editorX() + layout.editorWidth() + 6, layout.bottom() + 5, 0x70181818);

        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.drawCenteredString(this.font, this.title, this.width / 2, layout.titleY(), 0xFFFFFF);

        graphics.drawString(
                this.font,
                fitToWidth(Component.translatable("tagvyn.gui.titles.existing", this.payload.titles().size()).getString(), layout.listWidth()),
                layout.listX(),
                layout.listHeaderY(),
                0xD0D0D0
        );
        graphics.drawString(
                this.font,
                Component.translatable("tagvyn.gui.titles.search"),
                layout.listX(),
                layout.searchLabelY(),
                0xA0A0A0
        );
        renderTitleList(graphics, layout);

        graphics.enableScissor(layout.editorX(), layout.viewportTop(), layout.editorX() + layout.editorWidth(), layout.viewportBottom());
        renderEditor(graphics, layout);
        graphics.disableScissor();
        renderScrollbar(graphics, layout);
    }

    private void renderEditor(GuiGraphics graphics, Layout layout) {
        String selected = this.selectedTitleId.isBlank()
                ? Component.translatable("tagvyn.gui.titles.new_mode").getString()
                : Component.translatable("tagvyn.gui.titles.selected", this.selectedTitleId).getString();
        graphics.drawString(
                this.font,
                fitToWidth(selected, layout.editorContentWidth()),
                layout.editorX(),
                editorScreenY(layout, layout.statusY()),
                0xD0D0D0
        );

        graphics.drawString(
                this.font,
                Component.translatable("tagvyn.gui.titles.id"),
                layout.editorX(),
                editorScreenY(layout, layout.idLabelY()),
                0xA0A0A0
        );
        graphics.drawString(
                this.font,
                Component.translatable("tagvyn.gui.titles.text"),
                layout.editorX(),
                editorScreenY(layout, layout.textLabelY()),
                0xA0A0A0
        );
        graphics.drawString(
                this.font,
                Component.translatable("tagvyn.gui.titles.color"),
                layout.editorX(),
                editorScreenY(layout, layout.colorLabelY()),
                0xA0A0A0
        );

        Component drop = this.selectedPng == null
                ? Component.translatable("tagvyn.gui.titles.drop_png_short")
                : Component.translatable("tagvyn.gui.titles.selected_png", this.selectedFile, this.selectedPng.length / 1024);
        graphics.drawString(
                this.font,
                fitToWidth(drop.getString(), layout.editorContentWidth()),
                layout.editorX(),
                editorScreenY(layout, layout.helperY()),
                this.selectedPng == null ? 0x909090 : 0x80FF80
        );
        if (!this.localError.isBlank()) {
            graphics.drawString(
                    this.font,
                    fitToWidth(Component.translatable(this.localError).getString(), layout.editorContentWidth()),
                    layout.editorX(),
                    editorScreenY(layout, layout.errorY()),
                    0xFF7070
            );
        }
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

    private void renderScrollbar(GuiGraphics graphics, Layout layout) {
        int maxScroll = Math.max(0, layout.contentHeight() - layout.viewportHeight());
        if (maxScroll <= 0) return;
        int x = layout.editorX() + layout.editorWidth() - 3;
        int trackHeight = layout.viewportHeight();
        int thumbHeight = Math.max(18, trackHeight * layout.viewportHeight() / layout.contentHeight());
        int travel = Math.max(1, trackHeight - thumbHeight);
        int thumbY = layout.viewportTop() + (int) ((long) this.editorScroll * travel / maxScroll);
        graphics.fill(x, layout.viewportTop(), x + 2, layout.viewportBottom(), 0x40303030);
        graphics.fill(x, thumbY, x + 2, thumbY + thumbHeight, 0xB0A0A0A0);
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
        boolean compact = this.height < 300;
        int titleY = compact ? 7 : 12;
        int panelTop = compact ? 26 : 38;
        int bottom = Math.max(panelTop + 150, this.height - 12);

        int totalWidth = Math.max(290, Math.min(720, this.width - 24));
        int gap = 12;
        int listWidth = Math.max(92, Math.min(210, totalWidth / 3));
        int editorWidth = totalWidth - listWidth - gap;
        if (editorWidth < 180) {
            listWidth = Math.max(82, totalWidth - gap - 180);
            editorWidth = totalWidth - listWidth - gap;
        }
        int listX = (this.width - totalWidth) / 2;
        int editorX = listX + listWidth + gap;
        int editorContentWidth = Math.max(80, editorWidth - 6);

        int listHeaderY = panelTop + 5;
        int searchLabelY = listHeaderY + this.font.lineHeight + 5;
        int searchY = searchLabelY + this.font.lineHeight + LABEL_GAP;
        int rowsY = searchY + CONTROL_HEIGHT + 6;
        int rowsHeight = Math.max(ROW_HEIGHT, bottom - rowsY);

        int backY = bottom - CONTROL_HEIGHT;
        int viewportTop = panelTop + 5;
        int viewportBottom = Math.max(viewportTop + 50, backY - 6);

        int statusY = 0;
        int idLabelY = statusY + this.font.lineHeight + SECTION_GAP;
        int idY = idLabelY + this.font.lineHeight + LABEL_GAP;
        int textLabelY = idY + CONTROL_HEIGHT + SECTION_GAP;
        int textY = textLabelY + this.font.lineHeight + LABEL_GAP;
        int colorLabelY = textY + CONTROL_HEIGHT + SECTION_GAP;
        int colorY = colorLabelY + this.font.lineHeight + LABEL_GAP;
        int primarySaveY = colorY + CONTROL_HEIGHT + SECTION_GAP;
        int saveImageY = primarySaveY + CONTROL_HEIGHT + CONTROL_GAP;
        int newY = saveImageY + CONTROL_HEIGHT + CONTROL_GAP;
        int deleteY = newY + CONTROL_HEIGHT + CONTROL_GAP;
        int itemY = deleteY + CONTROL_HEIGHT + CONTROL_GAP;
        int helperY = itemY + CONTROL_HEIGHT + SECTION_GAP;
        int errorY = helperY + this.font.lineHeight + 3;
        int contentHeight = errorY + this.font.lineHeight + 8;

        return new Layout(
                titleY,
                panelTop,
                bottom,
                listX,
                listWidth,
                listHeaderY,
                searchLabelY,
                searchY,
                rowsY,
                rowsHeight,
                editorX,
                editorWidth,
                editorContentWidth,
                backY,
                viewportTop,
                viewportBottom,
                statusY,
                idLabelY,
                idY,
                textLabelY,
                textY,
                colorLabelY,
                colorY,
                primarySaveY,
                saveImageY,
                newY,
                deleteY,
                itemY,
                helperY,
                errorY,
                contentHeight
        );
    }

    private record Layout(
            int titleY,
            int panelTop,
            int bottom,
            int listX,
            int listWidth,
            int listHeaderY,
            int searchLabelY,
            int searchY,
            int rowsY,
            int rowsHeight,
            int editorX,
            int editorWidth,
            int editorContentWidth,
            int backY,
            int viewportTop,
            int viewportBottom,
            int statusY,
            int idLabelY,
            int idY,
            int textLabelY,
            int textY,
            int colorLabelY,
            int colorY,
            int primarySaveY,
            int saveImageY,
            int newY,
            int deleteY,
            int itemY,
            int helperY,
            int errorY,
            int contentHeight
    ) {
        int viewportHeight() {
            return Math.max(1, this.viewportBottom - this.viewportTop);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
