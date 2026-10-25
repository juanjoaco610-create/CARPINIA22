package com.carpinchoia.client.screen;

import com.carpinchoia.ModNet;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Modificador de Carpincho: lista de todos los carpinchos del servidor. */
public class CarpinchoListScreen extends Screen {
    private static final int PER_PAGE = 6;
    private final List<ModNet.Info> list;
    private int page = 0;

    public CarpinchoListScreen(List<ModNet.Info> list) {
        super(Component.literal("Modificador de Carpincho"));
        this.list = list;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int y = 40;
        int start = page * PER_PAGE;
        for (int i = start; i < Math.min(list.size(), start + PER_PAGE); i++) {
            ModNet.Info info = list.get(i);
            String text = info.name() + "   [" + info.x() + ", " + info.y() + ", " + info.z() + "]  radio " + info.radius()
                    + (info.loaded() ? "" : "  (lejos)");
            addRenderableWidget(Button.builder(Component.literal(text),
                    b -> this.minecraft.setScreen(new CarpinchoEditScreen(info, null)))
                    .bounds(cx - 150, y, 300, 20).build());
            y += 24;
        }
        if (list.size() > PER_PAGE) {
            addRenderableWidget(Button.builder(Component.literal("<"), b -> {
                page = Math.max(0, page - 1);
                rebuildWidgets();
            }).bounds(cx - 100, this.height - 52, 40, 20).build());
            addRenderableWidget(Button.builder(Component.literal(">"), b -> {
                if ((page + 1) * PER_PAGE < list.size()) page++;
                rebuildWidgets();
            }).bounds(cx + 60, this.height - 52, 40, 20).build());
        }
        addRenderableWidget(Button.builder(Component.literal("Cerrar"), b -> onClose())
                .bounds(cx - 50, this.height - 28, 100, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        g.drawCenteredString(this.font, Component.literal("§6§lCarpinchos del servidor (" + list.size() + ")"), this.width / 2, 14, 0xFFFFFF);
        if (list.isEmpty()) {
            g.drawCenteredString(this.font, Component.literal("§7Todavía no hay carpinchos. Usa el Huevo de Carpincho IA."), this.width / 2, 60, 0xFFFFFF);
        } else {
            g.drawCenteredString(this.font, Component.literal("§7Toca uno para modificarlo"), this.width / 2, 28, 0xFFFFFF);
        }
        super.render(g, mx, my, pt);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
