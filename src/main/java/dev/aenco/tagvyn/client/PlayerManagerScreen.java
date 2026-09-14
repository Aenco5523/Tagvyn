package dev.aenco.tagvyn.client;

import dev.aenco.tagvyn.network.AdminDashboardActionPayload;
import dev.aenco.tagvyn.network.AdminPlayerActionPayload;
import dev.aenco.tagvyn.network.OpenPlayerManagerPayload;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public final class PlayerManagerScreen extends Screen {
    private static final int PLAYER_ROW_HEIGHT = 24;
    private static final int TITLE_ROW_HEIGHT = 18;

    private final OpenPlayerManagerPayload payload;
    private String selectedUsername = "";
    private String selectedTitleId = "";
    private int playerScroll;
    private int titleScroll;

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
                layout.searchY(),
                layout.leftWidth(),
                20,
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
                layout.nicknameY(),
                layout.rightWidth(),
                20,
                Component.translatable("tagvyn.gui.players.nickname")
        );
        this.nicknameBox.setMaxLength(64);
        this.nicknameBox.setResponder(value -> updateButtons());
        this.addRenderableWidget(this.nicknameBox);

        int half = layout.rightWidth() / 2;
        this.saveNicknameButton = this.addRenderableWidget(Button.builder(
                Component.translatable("tagvyn.gui.players.save_nickname"),
                button -> sendPlayerAction("set_nickname", this.nicknameBox.getValue())
        ).bounds(layout.rightX(), layout.nicknameButtonsY(), half - 2, 20).build());

        this.clearNicknameButton = this.addRenderableWidget(Button.builder(
                Component.translatable("tagvyn.gui.players.clear_nickname"),
                button -> sendPlayerAction("clear_nickname", "")
        ).bounds(layout.rightX() + half + 2, layout.nicknameButtonsY(), layout.rightWidth() - half - 2, 20).build());

        this.resetCountButton = this.addRenderableWidget(Button.builder(
                Component.translatable("tagvyn.gui.players.reset_count"),
                button -> sendPlayerAction("reset_count", "")
        ).bounds(layout.rightX(), layout.resetY(), layout.rightWidth(), 20).build());

        this.titleSearchBox = new EditBox(
                this.font,
                layout.rightX(),
                layout.titleSearchY(),
                layout.rightWidth(),
                20,
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
        ).bounds(layout.rightX(), layout.titleButtonsY(), half - 2, 20).build());

        this.clearTitleButton = this.addRenderableWidget(Button.builder(
                Component.translatable("tagvyn.gui.players.clear_title"),
                button -> sendPlayerAction("clear_title", "")
        ).bounds(layout.rightX() + half + 2, layout.titleButtonsY(), layout.rightWidth() - half - 2, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("tagvyn.gui.back"),
                button -> PacketDistributor.sendToServer(new AdminDashboardActionPayload("dashboard"))
        ).bounds(layout.rightX(), layout.backY(), layout.rightWidth(), 20).build());

        ensureSelectedPlayer();
        loadSelectedPlayer();
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

        if (inside(mouseX, mouseY, layout.rightX(), layout.titleListY(), layout.rightWidth(), layout.titleListHeight())) {
            List<String> titles = filteredTitles();
            int row = ((int) mouseY - layout.titleListY()) / TITLE_ROW_HEIGHT;
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
        if (inside(mouseX, mouseY, layout.rightX(), layout.titleListY(), layout.rightWidth(), layout.titleListHeight())) {
            this.titleScroll -= (int) Math.signum(scrollY);
            clampTitleScroll();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void clampPlayerScroll() {
        Layout layout = layout();
        int visible = Math.max(1, layout.playerListHeight() / PLAYER_ROW_HEIGHT);
        this.playerScroll = Math.max(0, Math.min(this.playerScroll, Math.max(0, filteredPlayers().size() - visible)));
    }

    private void clampTitleScroll() {
        Layout layout = layout();
        int visible = Math.max(1, layout.titleListHeight() / TITLE_ROW_HEIGHT);
        this.titleScroll = Math.max(0, Math.min(this.titleScroll, Math.max(0, filteredTitles().size() - visible)));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Layout layout = layout();
        graphics.fill(0, 0, this.width, this.height, 0xD0101010);
        graphics.fill(layout.leftX() - 6, 43, layout.leftX() + layout.leftWidth() + 6, layout.bottom() + 5, 0x70181818);
        graphics.fill(layout.rightX() - 6, 43, layout.rightX() + layout.rightWidth() + 6, layout.bottom() + 5, 0x70181818);

        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.drawCenteredString(this.font, this.title, this.width / 2, 14, 0xFFFFFF);
        graphics.drawCenteredString(
                this.font,
                Component.translatable("tagvyn.gui.players.description"),
                this.width / 2,
                28,
                0xA0A0A0
        );

        graphics.drawString(this.font, Component.translatable("tagvyn.gui.players.search"), layout.leftX(), 47, 0xB0B0B0);
        renderPlayerList(graphics, layout);
        renderPlayerDetails(graphics, layout);
        renderTitleList(graphics, layout);
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
        graphics.drawString(
                this.font,
                Component.translatable("tagvyn.gui.players.selected", fitToWidth(selectedName, layout.rightWidth() - 72)),
                layout.rightX(),
                47,
                0xFFFFFF
        );

        if (selected != null) {
            String remaining = selected.remainingChanges() < 0 ? "∞" : Integer.toString(selected.remainingChanges());
            graphics.drawString(
                    this.font,
                    Component.translatable("tagvyn.gui.players.stats", selected.nicknameChanges(), remaining),
                    layout.rightX(),
                    59,
                    0x909090
            );
        }

        graphics.drawString(this.font, Component.translatable("tagvyn.gui.players.nickname"), layout.rightX(), layout.nicknameY() - 10, 0xB0B0B0);
        graphics.drawString(this.font, Component.translatable("tagvyn.gui.players.title_label"), layout.rightX(), layout.titleSearchY() - 10, 0xB0B0B0);
        String current = selected == null || selected.titleId().isBlank()
                ? Component.translatable("tagvyn.gui.players.no_title").getString()
                : selected.titleId();
        graphics.drawString(
                this.font,
                Component.translatable("tagvyn.gui.players.current_title", fitToWidth(current, layout.rightWidth() - 90)),
                layout.rightX(),
                layout.titleSearchY() - 22,
                0x909090
        );
    }

    private void renderTitleList(GuiGraphics graphics, Layout layout) {
        List<String> titles = filteredTitles();
        int visible = Math.max(1, layout.titleListHeight() / TITLE_ROW_HEIGHT);
        int end = Math.min(titles.size(), this.titleScroll + visible);

        if (titles.isEmpty()) {
            graphics.drawCenteredString(
                    this.font,
                    Component.translatable("tagvyn.gui.players.no_title_matches"),
                    layout.rightX() + layout.rightWidth() / 2,
                    layout.titleListY() + 6,
                    0x909090
            );
            return;
        }

        OpenPlayerManagerPayload.PlayerSummary selectedPlayer = selectedPlayer();
        String currentTitle = selectedPlayer == null ? "" : selectedPlayer.titleId();
        for (int index = this.titleScroll; index < end; index++) {
            String title = titles.get(index);
            int rowY = layout.titleListY() + (index - this.titleScroll) * TITLE_ROW_HEIGHT;
            if (title.equals(this.selectedTitleId)) {
                graphics.fill(layout.rightX(), rowY, layout.rightX() + layout.rightWidth(), rowY + TITLE_ROW_HEIGHT - 2, 0x90606060);
            } else if (title.equals(currentTitle)) {
                graphics.fill(layout.rightX(), rowY, layout.rightX() + layout.rightWidth(), rowY + TITLE_ROW_HEIGHT - 2, 0x50506030);
            } else if (index % 2 == 0) {
                graphics.fill(layout.rightX(), rowY, layout.rightX() + layout.rightWidth(), rowY + TITLE_ROW_HEIGHT - 2, 0x40282828);
            }
            graphics.drawString(this.font, fitToWidth(title, layout.rightWidth() - 8), layout.rightX() + 4, rowY + 5, 0xE0E0E0);
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
        int totalWidth = Math.max(300, Math.min(660, this.width - 24));
        int gap = 12;
        int leftWidth = Math.max(110, Math.min(200, totalWidth / 3));
        int rightWidth = totalWidth - leftWidth - gap;
        if (rightWidth < 210) {
            leftWidth = Math.max(90, totalWidth - gap - 210);
            rightWidth = totalWidth - leftWidth - gap;
        }
        int leftX = (this.width - totalWidth) / 2;
        int rightX = leftX + leftWidth + gap;
        int bottom = Math.max(210, this.height - 12);
        int searchY = 58;
        int playerListY = 82;
        int playerListHeight = Math.max(48, bottom - playerListY);

        int nicknameY = 80;
        int nicknameButtonsY = 104;
        int resetY = 128;
        int titleSearchY = 164;
        int titleListY = 188;
        int backY = bottom - 20;
        int titleButtonsY = backY - 24;
        int titleListHeight = Math.max(18, titleButtonsY - 4 - titleListY);

        return new Layout(
                leftX,
                leftWidth,
                rightX,
                rightWidth,
                searchY,
                playerListY,
                playerListHeight,
                nicknameY,
                nicknameButtonsY,
                resetY,
                titleSearchY,
                titleListY,
                titleListHeight,
                titleButtonsY,
                backY,
                bottom
        );
    }

    private record Layout(
            int leftX,
            int leftWidth,
            int rightX,
            int rightWidth,
            int searchY,
            int playerListY,
            int playerListHeight,
            int nicknameY,
            int nicknameButtonsY,
            int resetY,
            int titleSearchY,
            int titleListY,
            int titleListHeight,
            int titleButtonsY,
            int backY,
            int bottom
    ) {}

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
