package com.carpinchoia.client;

import com.carpinchoia.CarpinchoIA;
import com.carpinchoia.ModNet;
import com.carpinchoia.client.screen.CarpinchoEditScreen;
import com.carpinchoia.client.screen.CarpinchoListScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class ClientHooks {
    /** Momento (en reloj local) en que termina el Momento Carpincho; 0 si no hay. */
    public static long momentoEnd = 0;

    public static void openList(List<ModNet.Info> list) {
        Minecraft.getInstance().setScreen(new CarpinchoListScreen(list));
    }

    public static void openCreate(BlockPos pos) {
        Minecraft.getInstance().setScreen(new CarpinchoEditScreen(null, pos));
    }

    public static void momento(long remaining, boolean animate) {
        momentoEnd = remaining > 0 ? System.currentTimeMillis() + remaining : 0;
        if (animate && remaining > 0) {
            Minecraft mc = Minecraft.getInstance();
            // animacion tipo totem, pero con la cara del carpincho
            mc.gameRenderer.displayItemActivation(new ItemStack(CarpinchoIA.FACE.get()));
            if (mc.level != null && mc.player != null) {
                mc.level.playLocalSound(mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                        SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0F, 1.0F, false);
            }
        }
    }
}
