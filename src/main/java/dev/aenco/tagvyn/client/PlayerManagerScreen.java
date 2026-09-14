package dev.aenco.tagvyn.client;

import dev.aenco.tagvyn.network.AdminDashboardActionPayload;
import dev.aenco.tagvyn.network.AdminPlayerActionPayload;
import dev.aenco.tagvyn.network.OpenPlayerManagerPayload;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public final class PlayerManagerScreen extends Screen {
    private static final int PLAYER_ROW_HEIGHT = 24;
    private static final int TITLE_ROW_HEIGHT = 18;
    private static final int TITLE_VISIBLE_ROWS = 6;
    private static final int CONTROL_HEIGHT = 20;
    private static final int CONTROL_GAP = 5;
    private static final int LABEL_GAP = 3;
    private static final int SECTION_GAP = 14;
    private static final int SCROLL_STEP = 24;

    private final OpenPlayerManagerPayload payload;
    private String selectedUsername = "";
    private String selectedTitleId = "";
    private int playerScroll;
    private int titleScroll;
    private int detailScroll;

    private EditBox playerSearchBox;
    private EditBox nicknameBox;
    private EditBox titleSearchBox;
    private Button saveNicknameButton;
    private Button clearNicknameButton;
    private Button resetCountButton;
    private Button applyTitleButton;
    private Button clearTitleButton;

    public PlayerManagerScreen(OpenPlayerManagerPayload payload) {
        super(Component.translatable("tagvyn.gui.players.title"));
        this.payload = payload;
        this.selectedUsername = payload.selectedUsername();
    }

    @Override
    protected void init() {
        Layout layout = layout();

        this.playerSearchBox = new EditBox(
                this.font,
                layout.leftX(),
                layout.playerSearchY(),
                layout.leftWidth(),
                CONTROL_HEIGHT,
                Component.translatable("tagvyn.gui.players.search")
        );
        this.playerSearchBox.setMaxLength(64);
        this.playerSearchBox.setResponder(value -> {
            this.playerScroll = 0;
            clampPlayerScroll();
        });
        this.addRenderableWidget(this.playerSearchBox);

        this.nicknameBox = new EditBox(
                this.font,
                layout.rightX(),
                screenY(layout, layout.nicknameY()),
                layout.contentWidth(),
                CONTROL_HEIGHT,
                Component.translatable("tagvyn.gui.players.nickname")
        );
        this.nicknameBox.setMaxLength(64);
        this.nicknameBox.setResponder(value -> updateButtons());
        this.addRenderableWidget(this.nicknameBox);

        this.saveNicknameButton = this.addRenderableWidget(Button.builder(
                Component.translatable("tagvyn.gui.players.save_nickname"),
                button -> sendPlayerAction("set_nickname", this.nicknameBox.getValue())
        ).bounds(layout.rightX(), screenY(layout, layout.saveNicknameY()), layout.contentWidth(), CONTROL_HEIGHT).build());

        this.clearNicknameButton = this.addRenderableWidget(Button.builder(
                Component.translatable("tagvyn.gui.players.clear_nickname"),
                button -> sendPlayerAction("clear_nickname", "")
        ).bounds(layout.rightX(), screenY(layout, layout.clearNicknameY()), layout.contentWidth(), CONTROL_HEIGHT).build());

        this.resetCountButton = this.addRenderableWidget(Button.builder(
                Component.translatable("tagvyn.gui.players.reset_count"),
                button -> sendPlayerAction("reset_count", "")
        ).bounds(layout.rightX(), screenY(layout, layout.resetY()), layout.contentWidth(), CONTROL_HEIGHT).build());

        this.titleSearchBox = new EditBox(
                this.font,
                layout.rightX(),
                screenY(layout, layout.titleSearchY()),
                layout.contentWidth(),
                CONTROL_HEIGHT,
                Component.translatable("tagvyn.gui.players.title_search")
        );
        this.titleSearchBox.setMaxLength(64);
        this.titleSearchBox.setResponder(value -> {
            this.titleScroll = 0;
            clampTitleScroll();
        });
        this.addRenderableWidget(this.titleSearchBox);

        this.applyTitleButton = this.addRenderableWidget(Button.builder(
                Component.translatable("tagvyn.gui.players.apply_title"),
                button -> sendPlayerAction("set_title", this.selectedTitleId)
        ).bounds(layout.rightX(), screenY(layout, layout.applyTitleY()), layout.contentWidth(), CONTROL_HEIGHT).build());

        this.clearTitleButton = this.addRenderableWidget(Button.builder(
                Component.translatable("tagvyn.gui.players.clear_title"),
                button -> sendPlayerAction("clear_title", "")
        ).bounds(layout.rightX(), screenY(layout, layout.clearTitleY()), layout.contentWidth(), CONTROL_HEIGHT).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("tagvyn.gui.back"),
                button -> PacketDistributor.sendToServer(new AdminDashboardActionPayload("dashboard"))
        ).bounds(layout.rightX(), layout.backY(), layout.rightWidth(), CONTROL_HEIGHT).build());

        ensureSelectedPlayer();
        loadSelectedPlayer();
        clampDetailScroll();
        updateScrolledWidgets();
        updateButtons();
    }

    private void ensureSelectedPlayer() {
        if (findPlayer(this.selectedUsername) != null) return;
        if (!this.payload.players().isEmpty()) {
            this.selectedUsername = this.payload.players().getFirst().username();
        } else {
            this.selectedUsername = "";
        }
    }

    private void loadSelectedPlayer() {
        OpenPlayerManagerPayload.PlayerSummary selected = selectedPlayer();
        if (selected == null) {
            if (this.nicknameBox != null) this.nicknameBox.setValue("");
            this.selectedTitleId = "";
            updateButtons();
            return;
        }

        if (this.nicknameBox != null) {
            this.nicknameBox.setValue(selected.nickname());
        }
        this.selectedTitleId = selected.titleId();
        updateButtons();
    }

    private OpenPlayerManagerPayload.PlayerSummary selectedPlayer() {
        return findPlayer(this.selectedUsername);
    }

    private OpenPlayerManagerPayload.PlayerSummary findPlayer(String username) {
        if (username == null || username.isBlank()) return null;
        for (OpenPlayerManagerPayload.PlayerSummary player : this.payload.players()) {
            if (player.username().equalsIgnoreCase(username)) return player;
        }
        return null;
    }

    private List<OpenPlayerManagerPayload.PlayerSummary> filteredPlayers() {
        String query = this.playerSearchBox == null ? "" : this.playerSearchBox.getValue().trim().toLowerCase(Locale.ROOT);
        if (query.isBlank()) return this.payload.players();
        return this.payload.players().stream()
                .filter(player -> player.username().toLowerCase(Locale.ROOT).contains(query)
                        || player.nickname().toLowerCase(Locale.ROOT).contains(query))
                .toList();
    }

    private List<String> filteredTitles() {
        String query = this.titleSearchBox == null ? "" : this.titleSearchBox.getValue().trim().toLowerCase(Locale.ROOT);
        if (query.isBlank()) return this.payload.titles();
        return this.payload.titles().stream()
                .filter(title -> title.toLowerCase(Locale.ROOT).contains(query))
                .toList();
    }

    private void updateButtons() {
        OpenPlayerManagerPayload.PlayerSummary selected = selectedPlayer();
        boolean hasPlayer = selected != null;
        if (this.saveNicknameButton != null) {
            this.saveNicknameButton.active = hasPlayer
                    && this.nicknameBox != null
                    && !this.nicknameBox.getValue().trim().isBlank();
        }
        if (this.clearNicknameButton != null) this.clearNicknameButton.active = hasPlayer;
        if (this.resetCountButton != null) this.resetCountButton.active = hasPlayer;
        if (this.applyTitleButton != null) {
            this.applyTitleButton.active = hasPlayer
                    && !this.selectedTitleId.isBlank()
                    && this.payload.titles().contains(this.selectedTitleId);
        }
        if (this.clearTitleButton != null) this.clearTitleButton.active = hasPlayer;
    }

    private void sendPlayerAction(String action, String value) {
        OpenPlayerManagerPayload.PlayerSummary selected = selectedPlayer();
        if (selected == null) return;
        PacketDistributor.sendToServer(new AdminPlayerActionPayload(
                action,
                selected.username(),
                value == null ? "" : value
        ));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        Layout layout = layout();

        if (inside(mouseX, mouseY, layout.leftX(), layout.playerListY(), layout.leftWidth(), layout.playerListHeight())) {
            List<OpenPlayerManagerPayload.PlayerSummary> players = filteredPlayers();
            int row = ((int) mouseY - layout.playerListY()) / PLAYER_ROW_HEIGHT;
            int index = this.playerScroll + row;
            if (index >= 0 && index < players.size()) {
                this.selectedUsername = players.get(index).username();
                loadSelectedPlayer();
                return true;
            }
        }

        int listY = screenY(layout, layout.titleListY());
        int clipTop = Math.max(layout.viewportTop(), listY);
        int clipBottom = Math.min(layout.viewportBottom(), listY + layout.titleListHeight());
        if (clipBottom > clipTop
                && inside(mouseX, mouseY, layout.rightX(), clipTop, layout.contentWidth(), clipBottom - clipTop)) {
            List<String> titles = filteredTitles();
            int row = ((int) mouseY - listY) / TITLE_ROW_HEIGHT;
            int index = this.titleScroll + row;
            if (index >= 0 && index < titles.size()) {
                this.selectedTitleId = titles.get(index);
                updateButtons();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        Layout layout = layout();
        if (inside(mouseX, mouseY, layout.leftX(), layout.playerListY(), layout.leftWidth(), layout.playerListHeight())) {
            this.playerScroll -= (int) Math.signum(scrollY);
            clampPlayerScroll();
            return true;
        }

        if (inside(mouseX, mouseY, layout.rightX(), layout.viewportTop(), layout.rightWidth(), layout.viewportHeight())) {
            int listY = screenY(layout, layout.titleListY());
            int clipTop = Math.max(layout.viewportTop(), listY);
            int clipBottom = Math.min(layout.viewportBottom(), listY + layout.titleListHeight());
            if (clipBottom > clipTop
                    && inside(mouseX, mouseY, layout.rightX(), clipTop, layout.contentWidth(), clipBottom - clipTop)
                    && maxTitleScroll(layout) > 0) {
                this.titleScroll -= (int) Math.signum(scrollY);
                clampTitleScroll();
            } else {
                this.detailScroll -= (int) Math.signum(scrollY) * SCROLL_STEP;
                clampDetailScroll();
                updateScrolledWidgets();
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void clampPlayerScroll() {
        Layout layout = layout();
        int visible = Math.max(1, layout.playerListHeight() / PLAYER_ROW_HEIGHT);
        this.playerScroll = Math.max(0, Math.min(this.playerScroll, Math.max(0, filteredPlayers().size() - visible)));
    }

    private int maxTitleScroll(Layout layout) {
        int visible = Math.max(1, layout.titleListHeight() / TITLE_ROW_HEIGHT);
        return Math.max(0, filteredTitles().size() - visible);
    }

    private void clampTitleScroll() {
        Layout layout = layout();
        this.titleScroll = Math.max(0, Math.min(this.titleScroll, maxTitleScroll(layout)));
    }

    private void clampDetailScroll() {
        Layout layout = layout();
        this.detailScroll = Math.max(0, Math.min(this.detailScroll, Math.max(0, layout.contentHeight() - layout.viewportHeight())));
    }

    private int screenY(Layout layout, int contentY) {
        return layout.viewportTop() + contentY - this.detailScroll;
    }

    private void updateScrolledWidgets() {
        Layout layout = layout();
        placeScrolledWidget(this.nicknameBox, screenY(layout, layout.nicknameY()), layout);
        placeScrolledWidget(this.saveNicknameButton, screenY(layout, layout.saveNicknameY()), layout);
        placeScrolledWidget(this.clearNicknameButton, screenY(layout, layout.clearNicknameY()), layout);
        placeScrolledWidget(this.resetCountButton, screenY(layout, layout.resetY()), layout);
        placeScrolledWidget(this.titleSearchBox, screenY(layout, layout.titleSearchY()), layout);
        placeScrolledWidget(this.applyTitleButton, screenY(layout, layout.applyTitleY()), layout);
        placeScrolledWidget(this.clearTitleButton, screenY(layout, layout.clearTitleY()), layout);
    }

    private void placeScrolledWidget(AbstractWidget widget, int y, Layout layout) {
        if (widget == null) return;
        widget.setY(y);
        boolean visible = y >= layout.viewportTop() && y + widget.getHeight() <= layout.viewportBottom();
        widget.visible = visible;
        if (!visible) widget.setFocused(false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Layout layout = layout();
        clampDetailScroll();
        updateScrolledWidgets();

        graphics.fill(0, 0, this.width, this.height, 0xD0101010);
        graphics.fill(layout.leftX() - 6, layout.panelTop(), layout.leftX() + layout.leftWidth() + 6, layout.bottom() + 5, 0x70181818);
        graphics.fill(layout.rightX() - 6, layout.panelTop(), layout.rightX() + layout.rightWidth() + 6, layout.bottom() + 5, 0x70181818);

        graphics.enableScissor(layout.rightX(), layout.viewportTop(), layout.rightX() + layout.rightWidth(), layout.viewportBottom());
        renderDetailPanelBackgrounds(graphics, layout);
        graphics.disableScissor();

        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.drawCenteredString(this.font, this.title, this.width / 2, layout.titleY(), 0xFFFFFF);
        if (!layout.compact()) {
            String description = fitToWidth(
                    Component.translatable("tagvyn.gui.players.description").getString(),
                    Math.max(80, this.width - 24)
            );
            graphics.drawCenteredString(this.font, description, this.width / 2, layout.descriptionY(), 0xA0A0A0);
        }

        graphics.drawString(
                this.font,
                Component.translatable("tagvyn.gui.players.search"),
                layout.leftX(),
                layout.playerSearchLabelY(),
                0xB0B0B0
        );
        renderPlayerList(graphics, layout);

        graphics.enableScissor(layout.rightX(), layout.viewportTop(), layout.rightX() + layout.rightWidth(), layout.viewportBottom());
        renderPlayerDetails(graphics, layout);
        renderTitleList(graphics, layout);
        graphics.disableScissor();
        renderScrollbar(graphics, layout);
    }

    private void renderDetailPanelBackgrounds(GuiGraphics graphics, Layout layout) {
        int infoY = screenY(layout, layout.infoY());
        int nicknameTop = screenY(layout, layout.nicknameLabelY()) - 5;
        int nicknameBottom = screenY(layout, layout.resetY()) + CONTROL_HEIGHT + 6;
        int titleTop = screenY(layout, layout.titleLabelY()) - 5;
        int titleBottom = screenY(layout, layout.clearTitleY()) + CONTROL_HEIGHT + 6;
        graphics.fill(layout.rightX(), infoY, layout.rightX() + layout.contentWidth(), infoY + layout.infoHeight(), 0x40282828);
        graphics.fill(layout.rightX(), nicknameTop, layout.rightX() + layout.contentWidth(), nicknameBottom, 0x25202020);
        graphics.fill(layout.rightX(), titleTop, layout.rightX() + layout.contentWidth(), titleBottom, 0x25202020);
    }

    private void renderPlayerList(GuiGraphics graphics, Layout layout) {
        List<OpenPlayerManagerPayload.PlayerSummary> players = filteredPlayers();
        int visible = Math.max(1, layout.playerListHeight() / PLAYER_ROW_HEIGHT);
        int end = Math.min(players.size(), this.playerScroll + visible);

        if (players.isEmpty()) {
            graphics.drawCenteredString(
                    this.font,
                    Component.translatable("tagvyn.gui.players.no_matches"),
                    layout.leftX() + layout.leftWidth() / 2,
                    layout.playerListY() + 8,
                    0x909090
            );
            return;
        }

        for (int index = this.playerScroll; index < end; index++) {
            OpenPlayerManagerPayload.PlayerSummary player = players.get(index);
            int rowY = layout.playerListY() + (index - this.playerScroll) * PLAYER_ROW_HEIGHT;
            boolean selected = player.username().equalsIgnoreCase(this.selectedUsername);
            if (selected) {
                graphics.fill(layout.leftX(), rowY, layout.leftX() + layout.leftWidth(), rowY + PLAYER_ROW_HEIGHT - 2, 0x90505050);
            } else if (index % 2 == 0) {
                graphics.fill(layout.leftX(), rowY, layout.leftX() + layout.leftWidth(), rowY + PLAYER_ROW_HEIGHT - 2, 0x40282828);
            }

            graphics.drawString(this.font, fitToWidth(player.username(), layout.leftWidth() - 8), layout.leftX() + 4, rowY + 3, 0xFFFFFF);
            String secondary = player.nickname().isBlank()
                    ? Component.translatable("tagvyn.gui.players.no_nickname").getString()
                    : player.nickname();
            graphics.drawString(this.font, fitToWidth(secondary, layout.leftWidth() - 8), layout.leftX() + 4, rowY + 13, 0x909090);
        }
    }

    private void renderPlayerDetails(GuiGraphics graphics, Layout layout) {
        OpenPlayerManagerPayload.PlayerSummary selected = selectedPlayer();
        String selectedName = selected == null
                ? Component.translatable("tagvyn.gui.players.none").getString()
                : selected.username();
        int infoY = screenY(layout, layout.infoY());
        graphics.drawString(
                this.font,
                fitToWidth(Component.translatable("tagvyn.gui.players.selected", selectedName).getString(), layout.contentWidth() - 8),
                layout.rightX() + 4,
                infoY + 4,
                0xFFFFFF
        );

        String stats = "";
        String current = Component.translatable("tagvyn.gui.players.no_title").getString();
        if (selected != null) {
            String remaining = selected.remainingChanges() < 0 ? "∞" : Integer.toString(selected.remainingChanges());
            stats = Component.translatable("tagvyn.gui.players.stats", selected.nicknameChanges(), remaining).getString();
            if (!selected.titleId().isBlank()) current = selected.titleId();
        }
        if (!stats.isBlank()) {
            graphics.drawString(
                    this.font,
                    fitToWidth(stats, layout.contentWidth() - 8),
                    layout.rightX() + 4,
                    infoY + 14,
                    0x909090
            );
        }
        graphics.drawString(
                this.font,
                fitToWidth(Component.translatable("tagvyn.gui.players.current_title", current).getString(), layout.contentWidth() - 8),
                layout.rightX() + 4,
                infoY + 24,
                0xB0B0B0
        );

        graphics.drawString(
                this.font,
                Component.translatable("tagvyn.gui.players.nickname"),
                layout.rightX(),
                screenY(layout, layout.nicknameLabelY()),
                0xD0D0D0
        );
        graphics.drawString(
                this.font,
                Component.translatable("tagvyn.gui.players.title_label"),
                layout.rightX(),
                screenY(layout, layout.titleLabelY()),
                0xD0D0D0
        );
    }

    private void renderTitleList(GuiGraphics graphics, Layout layout) {
        List<String> titles = filteredTitles();
        int visible = Math.max(1, layout.titleListHeight() / TITLE_ROW_HEIGHT);
        int end = Math.min(titles.size(), this.titleScroll + visible);
        int listY = screenY(layout, layout.titleListY());

        if (titles.isEmpty()) {
            graphics.drawCenteredString(
                    this.font,
                    Component.translatable("tagvyn.gui.players.no_title_matches"),
                    layout.rightX() + layout.contentWidth() / 2,
                    listY + 6,
                    0x909090
            );
            return;
        }

        OpenPlayerManagerPayload.PlayerSummary selectedPlayer = selectedPlayer();
        String currentTitle = selectedPlayer == null ? "" : selectedPlayer.titleId();
        for (int index = this.titleScroll; index < end; index++) {
            String title = titles.get(index);
            int rowY = listY + (index - this.titleScroll) * TITLE_ROW_HEIGHT;
            if (title.equals(this.selectedTitleId)) {
                graphics.fill(layout.rightX(), rowY, layout.rightX() + layout.contentWidth(), rowY + TITLE_ROW_HEIGHT - 2, 0x90606060);
            } else if (title.equals(currentTitle)) {
                graphics.fill(layout.rightX(), rowY, layout.rightX() + layout.contentWidth(), rowY + TITLE_ROW_HEIGHT - 2, 0x50506030);
            } else if (index % 2 == 0) {
                graphics.fill(layout.rightX(), rowY, layout.rightX() + layout.contentWidth(), rowY + TITLE_ROW_HEIGHT - 2, 0x40282828);
            }
            graphics.drawString(this.font, fitToWidth(title, layout.contentWidth() - 8), layout.rightX() + 4, rowY + 5, 0xE0E0E0);
        }
    }

    private void renderScrollbar(GuiGraphics graphics, Layout layout) {
        int maxScroll = Math.max(0, layout.contentHeight() - layout.viewportHeight());
        if (maxScroll <= 0) return;
        int x = layout.rightX() + layout.rightWidth() - 3;
        int trackHeight = layout.viewportHeight();
        int thumbHeight = Math.max(18, trackHeight * layout.viewportHeight() / layout.contentHeight());
        int travel = Math.max(1, trackHeight - thumbHeight);
        int thumbY = layout.viewportTop() + (int) ((long) this.detailScroll * travel / maxScroll);
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
        int descriptionY = 26;
        int panelTop = compact ? 26 : 42;
        int bottom = Math.max(panelTop + 150, this.height - 12);

        int totalWidth = Math.max(290, Math.min(720, this.width - 24));
        int gap = 12;
        int leftWidth = Math.max(100, Math.min(210, totalWidth / 3));
        int rightWidth = totalWidth - leftWidth - gap;
        if (rightWidth < 180) {
            leftWidth = Math.max(84, totalWidth - gap - 180);
            rightWidth = totalWidth - leftWidth - gap;
        }
        int leftX = (this.width - totalWidth) / 2;
        int rightX = leftX + leftWidth + gap;

        int playerSearchLabelY = panelTop + 5;
        int playerSearchY = playerSearchLabelY + this.font.lineHeight + LABEL_GAP;
        int playerListY = playerSearchY + CONTROL_HEIGHT + 6;
        int playerListHeight = Math.max(PLAYER_ROW_HEIGHT, bottom - playerListY);

        int backY = bottom - CONTROL_HEIGHT;
        int viewportTop = panelTop + 5;
        int viewportBottom = Math.max(viewportTop + 50, backY - 6);
        int contentWidth = Math.max(80, rightWidth - 6);

        int infoY = 0;
        int infoHeight = 38;
        int nicknameLabelY = infoY + infoHeight + SECTION_GAP;
        int nicknameY = nicknameLabelY + this.font.lineHeight + LABEL_GAP;
        int saveNicknameY = nicknameY + CONTROL_HEIGHT + CONTROL_GAP;
        int clearNicknameY = saveNicknameY + CONTROL_HEIGHT + CONTROL_GAP;
        int resetY = clearNicknameY + CONTROL_HEIGHT + CONTROL_GAP;

        int titleLabelY = resetY + CONTROL_HEIGHT + SECTION_GAP;
        int titleSearchY = titleLabelY + this.font.lineHeight + LABEL_GAP;
        int titleListY = titleSearchY + CONTROL_HEIGHT + 6;
        int titleListHeight = TITLE_VISIBLE_ROWS * TITLE_ROW_HEIGHT;
        int applyTitleY = titleListY + titleListHeight + CONTROL_GAP;
        int clearTitleY = applyTitleY + CONTROL_HEIGHT + CONTROL_GAP;
        int contentHeight = clearTitleY + CONTROL_HEIGHT + 6;

        return new Layout(
                leftX,
                leftWidth,
                rightX,
                rightWidth,
                contentWidth,
                panelTop,
                bottom,
                titleY,
                descriptionY,
                compact,
                playerSearchLabelY,
                playerSearchY,
                playerListY,
                playerListHeight,
                backY,
                viewportTop,
                viewportBottom,
                infoY,
                infoHeight,
                nicknameLabelY,
                nicknameY,
                saveNicknameY,
                clearNicknameY,
                resetY,
                titleLabelY,
                titleSearchY,
                titleListY,
                titleListHeight,
                applyTitleY,
                clearTitleY,
                contentHeight
        );
    }

    private record Layout(
            int leftX,
            int leftWidth,
            int rightX,
            int rightWidth,
            int contentWidth,
            int panelTop,
            int bottom,
            int titleY,
            int descriptionY,
            boolean compact,
            int playerSearchLabelY,
            int playerSearchY,
            int playerListY,
            int playerListHeight,
            int backY,
            int viewportTop,
            int viewportBottom,
            int infoY,
            int infoHeight,
            int nicknameLabelY,
            int nicknameY,
            int saveNicknameY,
            int clearNicknameY,
            int resetY,
            int titleLabelY,
            int titleSearchY,
            int titleListY,
            int titleListHeight,
            int applyTitleY,
            int clearTitleY,
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
