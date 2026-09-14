package dev.aenco.tagvyn.client;

import dev.aenco.tagvyn.network.OpenNicknameScreenPayload;
import dev.aenco.tagvyn.network.SubmitNicknamePayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public final class NicknameScreen extends Screen {
    private final OpenNicknameScreenPayload payload;
    private EditBox nicknameBox;
    private Button saveButton;

    public NicknameScreen(OpenNicknameScreenPayload payload) {
        super(Component.translatable("tagvyn.gui.nickname.title"));
        this.payload = payload;
    }

    @Override
    protected void init() {
        int boxWidth = Math.min(240, this.width - 40);
        int x = (this.width - boxWidth) / 2;
        int y = this.height / 2 - 10;

        this.nicknameBox = new EditBox(
                this.font,
                x,
                y,
                boxWidth,
                20,
                Component.translatable("tagvyn.gui.nickname.input")
        );
        this.nicknameBox.setMaxLength(payload.maxLength());
        this.nicknameBox.setValue(payload.currentNickname());
        this.addRenderableWidget(this.nicknameBox);

        this.saveButton = this.addRenderableWidget(Button.builder(
                Component.translatable("tagvyn.gui.save"),
                button -> submit()
        ).bounds(this.width / 2 - 102, y + 34, 100, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("tagvyn.gui.cancel"),
                button -> onClose()
        ).bounds(this.width / 2 + 2, y + 34, 100, 20).build());

        this.nicknameBox.setResponder(value -> updateSaveState());
        this.updateSaveState();
        this.setInitialFocus(this.nicknameBox);
    }

    private void updateSaveState() {
        if (this.saveButton == null || this.nicknameBox == null) return;
        String value = this.nicknameBox.getValue().trim();
        int length = value.codePointCount(0, value.length());
        boolean validLength = length >= payload.minLength() && length <= payload.maxLength();
        boolean validSpaces = payload.allowSpaces() || value.codePoints().noneMatch(Character::isWhitespace);
        boolean hasChanges = payload.remainingChanges() != 0;
        this.saveButton.active = validLength && validSpaces && hasChanges;
    }

    private void submit() {
        if (this.nicknameBox == null || !this.saveButton.active) return;
        PacketDistributor.sendToServer(new SubmitNicknamePayload(this.nicknameBox.getValue()));
        this.onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0xC0101010);
        int centerY = this.height / 2;
        graphics.drawCenteredString(this.font, this.title, this.width / 2, centerY - 64, 0xFFFFFF);
        graphics.drawCenteredString(
                this.font,
                Component.translatable("tagvyn.gui.nickname.description"),
                this.width / 2,
                centerY - 46,
                0xB0B0B0
        );
        String remaining = payload.remainingChanges() < 0 ? "∞" : Integer.toString(payload.remainingChanges());
        graphics.drawCenteredString(
                this.font,
                Component.translatable("tagvyn.gui.nickname.remaining", remaining),
                this.width / 2,
                centerY + 50,
                0xA0A0A0
        );
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
