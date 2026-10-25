package com.carpinchoia.ai;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Files;
import java.nio.file.Path;

/** Claves y servicios de la IA. Se guarda en config/carpincho_ia-ai.json (SOLO en el servidor). */
public class AiConfig {
    public String anthropicKey = "";
    public String claudeModel = "claude-haiku-5-5";
    /** Clave del servicio de voz (escuchar y hablar). Por defecto es compatible con OpenAI. */
    public String speechKey = "";
    public String sttUrl = "https://api.openai.com/v1/audio/transcriptions";
    public String sttModel = "whisper-1";
    public String ttsUrl = "https://api.openai.com/v1/audio/speech";
    public String ttsModel = "tts-1";
    public String ttsVoice = "alloy";
    /** Distancia maxima (bloques) a la que el carpincho te escucha hablar. */
    public double hearRadius = 6.0;

    private static AiConfig instance;

    public static synchronized AiConfig get() {
        if (instance == null) instance = load();
        return instance;
    }

    private static AiConfig load() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Path file = FMLPaths.CONFIGDIR.get().resolve("carpincho_ia-ai.json");
        AiConfig c = new AiConfig();
        try {
            if (Files.exists(file)) {
                AiConfig read = gson.fromJson(Files.readString(file), AiConfig.class);
                if (read != null) c = read;
            } else {
                Files.writeString(file, gson.toJson(c));
            }
        } catch (Exception e) {
            System.err.println("[carpincho_ia] No pude leer " + file + ": " + e.getMessage());
        }
        if (c.anthropicKey == null || c.anthropicKey.isBlank()) c.anthropicKey = env("ANTHROPIC_API_KEY");
        if (c.speechKey == null || c.speechKey.isBlank()) c.speechKey = env("OPENAI_API_KEY");
        return c;
    }

    private static String env(String n) {
        String v = System.getenv(n);
        return v == null ? "" : v;
    }

    public boolean ready() {
        return anthropicKey != null && !anthropicKey.isBlank();
    }

    public boolean voiceReady() {
        return speechKey != null && !speechKey.isBlank();
    }
}
