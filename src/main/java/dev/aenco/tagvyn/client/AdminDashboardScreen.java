package dev.aenco.tagvyn.client;

import dev.aenco.tagvyn.network.AdminDashboardActionPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public final class AdminDashboardScreen extends Screen {
    public AdminDashboardScreen() {
        super(Component.translatable("tagvyn.gui.admin.title"));
    }

    @Override
    protected void init() {
        int buttonWidth = Math.min(240, this.width - 40);
        int x = (this.width - buttonWidth) / 2;
        int y = Math.max(58, this.height / 2 - 70);

        this.addRenderableWidget(Button.builder(
                Component.translatable("tagvyn.gui.admin.players"),
                button -> send("players")
        ).bounds(x, y, buttonWidth, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("tagvyn.gui.admin.titles"),
                button -> send("titles")
        ).bounds(x, y + 28, buttonWidth, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("tagvyn.gui.admin.my_nickname"),
                button -> send("nickname")
        ).bounds(x, y + 56, buttonWidth, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("tagvyn.gui.admin.reload"),
                button -> send("reload")
        ).bounds(x, y + 84, buttonWidth, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("tagvyn.gui.close"),
                button -> onClose()
        ).bounds(x, y + 120, buttonWidth, 20).build());
    }

    private void send(String action) {
        PacketDistributor.sendToServer(new AdminDashboardActionPayload(action));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0xD0101010);
        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
        graphics.drawCenteredString(
                this.font,
                Component.translatable("tagvyn.gui.admin.description"),
                this.width / 2,
                38,
                0xA0A0A0
        );
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
