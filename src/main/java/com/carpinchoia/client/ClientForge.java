package com.carpinchoia.client;

import com.carpinchoia.CarpinchoIA;
import com.carpinchoia.ClientConfig;
import com.carpinchoia.ModNet;
import com.carpinchoia.client.screen.CarpinchoOptionsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CarpinchoIA.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientForge {
    /** Agrega el boton "Carpincho..." en el menu de Opciones. */
    @SubscribeEvent
    public static void screenInit(ScreenEvent.Init.Post e) {
        if (e.getScreen() instanceof OptionsScreen options) {
            e.addListener(Button.builder(Component.literal("Carpincho..."),
                    b -> Minecraft.getInstance().setScreen(new CarpinchoOptionsScreen(options)))
                    .bounds(6, 6, 100, 20).build());
        }
    }

    /** Al entrar a un mundo, le avisa al servidor tu configuracion de carpincho. */
    @SubscribeEvent
    public static void login(ClientPlayerNetworkEvent.LoggingIn e) {
        sendSettings();
    }

    public static void sendSettings() {
        ModNet.CHANNEL.sendToServer(new ModNet.Settings(ClientConfig.SOUND.get(), ClientConfig.ALERTS.get(), ClientConfig.STEPS.get()));
    }
}
