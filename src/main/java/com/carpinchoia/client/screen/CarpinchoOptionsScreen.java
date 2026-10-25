package com.carpinchoia.client.screen;

import com.carpinchoia.ClientConfig;
import com.carpinchoia.client.ClientForge;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.ForgeConfigSpec;

/** Configuracion "Carpincho": sonido, alertas y pasos (si / no). */
public class CarpinchoOptionsScreen extends Screen {
    private final Screen parent;

    public CarpinchoOptionsScreen(Screen parent) {
        super(Component.literal("Carpincho"));
        this.parent = parent;
    }

    private static String yn(boolean b) {
        return b ? "§aSÍ" : "§cNO";
    }

    private Button toggle(int y, String label, ForgeConfigSpec.BooleanValue value) {
        return Button.builder(Component.literal(label + ": " + yn(value.get())), b -> {
            value.set(!value.get());
            value.save();
            b.setMessage(Component.literal(label + ": " + yn(value.get())));
            ClientForge.sendSettings();
        }).bounds(this.width / 2 - 110, y, 220, 20).build();
    }

    @Override
    protected void init() {
        int y = this.height / 4;
        addRenderableWidget(toggle(y, "Escuchar al carpincho (sonidos)", ClientConfig.SOUND));
        addRenderableWidget(toggle(y + 26, "Alertas de intrusos", ClientConfig.ALERTS));
        addRenderableWidget(toggle(y + 52, "Escuchar sus pasos", ClientConfig.STEPS));
        addRenderableWidget(Button.builder(Component.literal("Listo"), b -> onClose())
                .bounds(this.width / 2 - 100, y + 90, 200, 20).build());
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        g.drawCenteredString(this.font, Component.literal("§6§lConfiguración de Carpincho"), this.width / 2, this.height / 4 - 24, 0xFFFFFF);
        super.render(g, mx, my, pt);
    }
}
