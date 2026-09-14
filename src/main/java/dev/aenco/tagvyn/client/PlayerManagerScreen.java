package dev.aenco.tagvyn.client;

import dev.aenco.tagvyn.network.AdminDashboardActionPayload;
import dev.aenco.tagvyn.network.AdminPlayerActionPayload;
import dev.aenco.tagvyn.network.OpenPlayerManagerPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public final class PlayerManagerScreen extends Screen {
    private final OpenPlayerManagerPayload payload;
    private int playerIndex;
    private int titleIndex = -1;
    private Button playerButton;
    private Button titleButton;
    private Button applyTitleButton;
    private Button saveNicknameButton;
    private EditBox nicknameBox;

    public PlayerManagerScreen(OpenPlayerManagerPayload payload) {
        super(Component.translatable("tagvyn.gui.players.title"));
        this.payload = payload;
    }

    @Override
    protected void init() {
        int formWidth = Math.min(320, this.width - 32);
        int x = (this.width - formWidth) / 2;
        int y = Math.max(48, this.height / 2 - 100);
        int arrowWidth = 28;
        int centerWidth = formWidth - arrowWidth * 2 - 8;

        this.addRenderableWidget(Button.builder(Component.literal("<"), button -> cyclePlayer(-1))
                .bounds(x, y, arrowWidth, 20).build());
        this.playerButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> {})
                .bounds(x + arrowWidth + 4, y, centerWidth, 20).build());
        this.playerButton.active = false;
        this.addRenderableWidget(Button.builder(Component.literal(">"), button -> cyclePlayer(1))
                .bounds(x + formWidth - arrowWidth, y, arrowWidth, 20).build());

        this.nicknameBox = new EditBox(
                this.font,
                x,
                y + 36,
                formWidth,
                20,
                Component.translatable("tagvyn.gui.players.nickname")
        );
        this.nicknameBox.setMaxLength(64);
        this.nicknameBox.setResponder(value -> updateButtons());
        this.addRenderableWidget(this.nicknameBox);

        this.saveNicknameButton = this.addRenderableWidget(Button.builder(
                Component.translatable("tagvyn.gui.players.save_nickname"),
                button -> sendPlayerAction("set_nickname", this.nicknameBox.getValue())
        ).bounds(x, y + 62, formWidth / 2 - 2, 20).build());
        this.addRenderableWidget(Button.builder(
                Component.translatable("tagvyn.gui.players.clear_nickname"),
                button -> sendPlayerAction("clear_nickname", "")
        ).bounds(x + formWidth / 2 + 2, y + 62, formWidth / 2 - 2, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("tagvyn.gui.players.reset_count"),
                button -> sendPlayerAction("reset_count", "")
        ).bounds(x, y + 88, formWidth, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("<"), button -> cycleTitle(-1))
                .bounds(x, y + 124, arrowWidth, 20).build());
        this.titleButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> {})
                .bounds(x + arrowWidth + 4, y + 124, centerWidth, 20).build());
        this.titleButton.active = false;
        this.addRenderableWidget(Button.builder(Component.literal(">"), button -> cycleTitle(1))
                .bounds(x + formWidth - arrowWidth, y + 124, arrowWidth, 20).build());

        this.applyTitleButton = this.addRenderableWidget(Button.builder(
                Component.translatable("tagvyn.gui.players.apply_title"),
                button -> {
                    if (this.titleIndex >= 0 && this.titleIndex < this.payload.titles().size()) {
                        sendPlayerAction("set_title", this.payload.titles().get(this.titleIndex));
                    }
                }
        ).bounds(x, y + 150, formWidth / 2 - 2, 20).build());
        this.addRenderableWidget(Button.builder(
                Component.translatable("tagvyn.gui.players.clear_title"),
                button -> sendPlayerAction("clear_title", "")
        ).bounds(x + formWidth / 2 + 2, y + 150, formWidth / 2 - 2, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("tagvyn.gui.back"),
                button -> PacketDistributor.sendToServer(new AdminDashboardActionPayload("dashboard"))
        ).bounds(x, y + 184, formWidth, 20).build());

        loadPlayer();
    }

    private void cyclePlayer(int delta) {
        if (this.payload.players().isEmpty()) return;
        this.playerIndex = Math.floorMod(this.playerIndex + delta, this.payload.players().size());
        loadPlayer();
    }

    private void cycleTitle(int delta) {
        int size = this.payload.titles().size();
        if (size == 0) {
            this.titleIndex = -1;
        } else if (this.titleIndex < 0) {
            this.titleIndex = delta >= 0 ? 0 : size - 1;
        } else {
            this.titleIndex = Math.floorMod(this.titleIndex + delta, size);
        }
        updateTitleButton();
    }

    private void loadPlayer() {
        if (this.payload.players().isEmpty()) {
            if (this.playerButton != null) this.playerButton.setMessage(Component.translatable("tagvyn.gui.players.none"));
            if (this.nicknameBox != null) this.nicknameBox.setValue("");
            this.titleIndex = -1;
            updateTitleButton();
            updateButtons();
            return;
        }

        OpenPlayerManagerPayload.PlayerSummary selected = selectedPlayer();
        if (this.playerButton != null) {
            this.playerButton.setMessage(Component.literal(selected.username()));
        }
        if (this.nicknameBox != null) {
            this.nicknameBox.setValue(selected.nickname());
        }
        this.titleIndex = this.payload.titles().indexOf(selected.titleId());
        updateTitleButton();
        updateButtons();
    }

    private void updateTitleButton() {
        if (this.titleButton == null) return;
        if (this.titleIndex < 0 || this.titleIndex >= this.payload.titles().size()) {
            this.titleButton.setMessage(Component.translatable("tagvyn.gui.players.no_title"));
        } else {
            this.titleButton.setMessage(Component.literal(this.payload.titles().get(this.titleIndex)));
        }
        if (this.applyTitleButton != null) {
            this.applyTitleButton.active = !this.payload.players().isEmpty() && this.titleIndex >= 0;
        }
    }

    private void updateButtons() {
        if (this.saveNicknameButton != null) {
            this.saveNicknameButton.active = !this.payload.players().isEmpty()
                    && this.nicknameBox != null
                    && !this.nicknameBox.getValue().trim().isBlank();
        }
        if (this.applyTitleButton != null) {
            this.applyTitleButton.active = !this.payload.players().isEmpty() && this.titleIndex >= 0;
        }
    }

    private void sendPlayerAction(String action, String value) {
        if (this.payload.players().isEmpty()) return;
        PacketDistributor.sendToServer(new AdminPlayerActionPayload(
                action,
                selectedPlayer().username(),
                value == null ? "" : value
        ));
    }

    private OpenPlayerManagerPayload.PlayerSummary selectedPlayer() {
        return this.payload.players().get(Math.max(0, Math.min(this.playerIndex, this.payload.players().size() - 1)));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0xD0101010);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 16, 0xFFFFFF);
        graphics.drawCenteredString(
                this.font,
                Component.translatable("tagvyn.gui.players.description"),
                this.width / 2,
                31,
                0xA0A0A0
        );

        if (!this.payload.players().isEmpty()) {
            OpenPlayerManagerPayload.PlayerSummary selected = selectedPlayer();
            int formWidth = Math.min(320, this.width - 32);
            int x = (this.width - formWidth) / 2;
            int y = Math.max(48, this.height / 2 - 100);
            String remaining = selected.remainingChanges() < 0 ? "∞" : Integer.toString(selected.remainingChanges());
            graphics.drawString(
                    this.font,
                    Component.translatable(
                            "tagvyn.gui.players.stats",
                            selected.nicknameChanges(),
                            remaining
                    ),
                    x,
                    y + 23,
                    0x909090
            );
            graphics.drawString(this.font, Component.translatable("tagvyn.gui.players.nickname"), x, y + 26, 0xB0B0B0);
            graphics.drawString(this.font, Component.translatable("tagvyn.gui.players.title_label"), x, y + 114, 0xB0B0B0);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
