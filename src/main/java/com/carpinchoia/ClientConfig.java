package com.carpinchoia;

import net.minecraftforge.common.ForgeConfigSpec;

/** Configuracion "Carpincho" de cada jugador (se guarda en su computadora). */
public class ClientConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue SOUND;
    public static final ForgeConfigSpec.BooleanValue ALERTS;
    public static final ForgeConfigSpec.BooleanValue STEPS;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("carpincho");
        SOUND = b.define("sonido", true);
        ALERTS = b.define("alertas", true);
        STEPS = b.define("pasos", true);
        b.pop();
        SPEC = b.build();
    }
}
