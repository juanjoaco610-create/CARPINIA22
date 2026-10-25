package com.carpinchoia.client.screen;

import com.carpinchoia.ModNet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.UUID;

/** Crear (desde el huevo) o modificar (desde el modificador) un carpincho. */
public class CarpinchoEditScreen extends Screen {
    private final UUID id;
    private final boolean create;
    private String fName, fX, fY, fZ, fR, fOwners;

    public CarpinchoEditScreen(ModNet.Info info, BlockPos pos) {
        super(Component.literal(info == null ? "Nuevo Carpincho IA" : "Modificar carpincho"));
        this.create = info == null;
        if (info != null) {
            id = info.id();
            fName = info.name();
            fX = String.valueOf(info.x());
            fY = String.valueOf(info.y());
            fZ = String.valueOf(info.z());
            fR = String.valueOf(info.radius());
            fOwners = info.owners();
        } else {
            id = new UUID(0L, 0L);
            fName = "Carpincho IA 1";
            fX = String.valueOf(pos.getX());
            fY = String.valueOf(pos.getY());
            fZ = String.valueOf(pos.getZ());
            fR = "40";
            var player = Minecraft.getInstance().player;
            fOwners = player == null ? "" : player.getGameProfile().getName();
        }
    }

    private EditBox box(int x, int y, int w, String value, int max, java.util.function.Consumer<String> setter) {
        EditBox b = new EditBox(this.font, x, y, w, 18, Component.empty());
        b.setMaxLength(max);
        b.setValue(value);
        b.setResponder(setter);
        addRenderableWidget(b);
        return b;
    }

    private static int num(String s, int def) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int y = 36;
        box(cx - 120, y, 240, fName, 32, s -> fName = s);
        y += 34;
        box(cx - 120, y, 76, fX, 8, s -> fX = s);
        box(cx - 38, y, 76, fY, 8, s -> fY = s);
        box(cx + 44, y, 76, fZ, 8, s -> fZ = s);
        y += 34;
        box(cx - 120, y, 80, fR, 4, s -> fR = s);
        y += 34;
        box(cx - 120, y, 240, fOwners, 200, s -> fOwners = s);
        y += 34;
        addRenderableWidget(Button.builder(Component.literal(create ? "Crear carpincho" : "Guardar cambios"), b -> {
            ModNet.CHANNEL.sendToServer(new ModNet.Save(id, fName, num(fX, 0), num(fY, 64), num(fZ, 0), num(fR, 40), fOwners));
            this.minecraft.setScreen(null);
        }).bounds(cx - 120, y, 118, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancelar"), b -> onClose())
                .bounds(cx + 2, y, 118, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        int cx = this.width / 2;
        g.drawCenteredString(this.font, Component.literal("§6§l" + this.title.getString()), cx, 12, 0xFFFFFF);
        g.drawString(this.font, "Nombre (no se puede repetir)", cx - 120, 26, 0xA0A0A0, false);
        g.drawString(this.font, "Punto de aparición  X / Y / Z", cx - 120, 60, 0xA0A0A0, false);
        g.drawString(this.font, "Radio de bloques", cx - 120, 94, 0xA0A0A0, false);
        g.drawString(this.font, "Dueños (separados por coma)", cx - 120, 128, 0xA0A0A0, false);
        super.render(g, mx, my, pt);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
