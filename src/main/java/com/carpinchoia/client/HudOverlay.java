package com.carpinchoia.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** Temporizador del Momento Carpincho, a la derecha de la barra de objetos. */
public class HudOverlay {
    public static void render(GuiGraphics g, int w, int h) {
        long rem = ClientHooks.momentoEnd - System.currentTimeMillis();
        if (rem <= 0) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui) return;
        long s = rem / 1000;
        String t = String.format("Momento Carpincho %02d:%02d", s / 60, s % 60);
        g.drawString(mc.font, t, w / 2 + 98, h - 16, 0x8B5A2B, true);
    }
}
