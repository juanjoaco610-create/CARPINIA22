package com.carpinchoia.ai;

import com.carpinchoia.CarpinchoData;
import com.carpinchoia.CarpinchoEntity;
import com.carpinchoia.voice.VoiceBridge;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Escucha (voz -> texto), piensa con Claude y responde (texto -> voz). Todo fuera del hilo del juego. */
public class AiService {
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private static final ExecutorService POOL = Executors.newFixedThreadPool(3, r -> {
        Thread t = new Thread(r, "carpincho-ia");
        t.setDaemon(true);
        return t;
    });
    private static final Set<UUID> BUSY = ConcurrentHashMap.newKeySet();

    public static void handleVoice(CarpinchoEntity c, ServerPlayer p, short[] pcm48k) {
        start(c, p, null, pcm48k);
    }

    public static void handleText(CarpinchoEntity c, ServerPlayer p, String text) {
        start(c, p, text, null);
    }

    private static void start(CarpinchoEntity c, ServerPlayer p, String text, short[] pcm) {
        MinecraftServer server = p.getServer();
        if (server == null) return;
        AiConfig cfg = AiConfig.get();
        if (!cfg.ready() || (pcm != null && !cfg.voiceReady())) {
            p.sendSystemMessage(Component.literal("[Carpincho IA] Falta configurar las claves de la IA en config/carpincho_ia-ai.json del servidor.")
                    .withStyle(ChatFormatting.GRAY));
            return;
        }
        CarpinchoData.Entry en = CarpinchoData.get(server).map.get(c.getUUID());
        if (en == null) return;
        UUID cid = c.getUUID();
        if (!BUSY.add(cid)) return; // ya esta pensando
        String cname = en.name;
        String pname = p.getGameProfile().getName();
        boolean owner = en.owners.stream().anyMatch(pname::equalsIgnoreCase);
        String system = "Eres " + cname + ", un carpincho guardián en un mundo de Minecraft. "
                + "Cuidas la base alrededor de tu punto de aparición, dentro de un radio de " + en.radius + " bloques. "
                + "Tus dueños son: " + String.join(", ", en.owners) + ". "
                + "Hablas en español rioplatense, con un tono amable, tranquilo y un poco gracioso, como un carpincho. "
                + "Tus respuestas se leen en voz alta, así que son MUY cortas (1 a 3 frases), sin markdown, sin listas y sin emojis. "
                + "Te acuerdas de lo que te dijeron los jugadores antes. "
                + "Ahora te habla " + pname + (owner ? ", que es uno de tus dueños." : ", que no es tu dueño.")
                + " Si no sabes algo, dilo con honestidad.";

        POOL.execute(() -> {
            try {
                String heard = text != null ? text : speechToText(pcm, cfg);
                if (heard == null || heard.isBlank()) return;
                if (text == null) {
                    server.execute(() -> p.sendSystemMessage(Component.literal("<" + pname + " → " + cname + "> " + heard)
                            .withStyle(ChatFormatting.GRAY)));
                }
                AiMemory.append(cid, "user", pname + ": " + heard);
                String reply = askClaude(system, AiMemory.recent(cid, 30), cfg);
                AiMemory.append(cid, "assistant", reply);
                server.execute(() -> p.sendSystemMessage(Component.literal("<" + cname + "> " + reply)
                        .withStyle(ChatFormatting.GOLD)));
                if (cfg.voiceReady() && ModList.get().isLoaded("voicechat")) {
                    short[] audio = textToSpeech(reply, cfg);
                    server.execute(() -> VoiceBridge.speak(c, audio));
                }
            } catch (Exception ex) {
                System.err.println("[carpincho_ia] Error de IA: " + ex.getMessage());
                server.execute(() -> p.sendSystemMessage(Component.literal("[Carpincho IA] No pude responder (mira la consola del servidor).")
                        .withStyle(ChatFormatting.GRAY)));
            } finally {
                BUSY.remove(cid);
            }
        });
    }

    // ------------------------------------------------------------ Claude

    private static String askClaude(String system, JsonArray messages, AiConfig cfg) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("model", cfg.claudeModel);
        body.addProperty("max_tokens", 300);
        body.addProperty("system", system);
        body.add("messages", messages);
        HttpRequest req = HttpRequest.newBuilder(URI.create("https://api.anthropic.com/v1/messages"))
                .timeout(Duration.ofSeconds(40))
                .header("x-api-key", cfg.anthropicKey)
                .header("anthropic-version", "2023-06-01")
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8)).build();
        HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (res.statusCode() / 100 != 2) throw new RuntimeException("Claude respondió HTTP " + res.statusCode() + ": " + cut(res.body()));
        JsonObject o = JsonParser.parseString(res.body()).getAsJsonObject();
        return o.getAsJsonArray("content").get(0).getAsJsonObject().get("text").getAsString().trim();
    }

    // ------------------------------------------------------------ voz -> texto

    private static String speechToText(short[] pcm48k, AiConfig cfg) throws Exception {
        // 48 kHz -> 16 kHz (promedio de a 3 muestras) y formato WAV
        int n = pcm48k.length / 3;
        ByteArrayOutputStream pcm = new ByteArrayOutputStream(n * 2);
        for (int i = 0; i < n; i++) {
            int v = (pcm48k[3 * i] + pcm48k[3 * i + 1] + pcm48k[3 * i + 2]) / 3;
            pcm.write(v & 0xFF);
            pcm.write((v >> 8) & 0xFF);
        }
        byte[] data = pcm.toByteArray();
        ByteArrayOutputStream wav = new ByteArrayOutputStream();
        wav.write("RIFF".getBytes());
        le32(wav, 36 + data.length);
        wav.write("WAVEfmt ".getBytes());
        le32(wav, 16);
        le16(wav, 1);
        le16(wav, 1);
        le32(wav, 16000);
        le32(wav, 32000);
        le16(wav, 2);
        le16(wav, 16);
        wav.write("data".getBytes());
        le32(wav, data.length);
        wav.write(data);

        String boundary = "----carpincho" + System.nanoTime();
        ByteArrayOutputStream form = new ByteArrayOutputStream();
        field(form, boundary, "model", cfg.sttModel);
        field(form, boundary, "language", "es");
        form.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"voz.wav\"\r\nContent-Type: audio/wav\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        form.write(wav.toByteArray());
        form.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

        HttpRequest req = HttpRequest.newBuilder(URI.create(cfg.sttUrl))
                .timeout(Duration.ofSeconds(40))
                .header("Authorization", "Bearer " + cfg.speechKey)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(form.toByteArray())).build();
        HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (res.statusCode() / 100 != 2) throw new RuntimeException("Voz->texto HTTP " + res.statusCode() + ": " + cut(res.body()));
        return JsonParser.parseString(res.body()).getAsJsonObject().get("text").getAsString().trim();
    }

    // ------------------------------------------------------------ texto -> voz

    private static short[] textToSpeech(String text, AiConfig cfg) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("model", cfg.ttsModel);
        body.addProperty("voice", cfg.ttsVoice);
        body.addProperty("input", text);
        body.addProperty("response_format", "pcm"); // 24 kHz, 16 bits, mono
        HttpRequest req = HttpRequest.newBuilder(URI.create(cfg.ttsUrl))
                .timeout(Duration.ofSeconds(60))
                .header("Authorization", "Bearer " + cfg.speechKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8)).build();
        HttpResponse<byte[]> res = HTTP.send(req, HttpResponse.BodyHandlers.ofByteArray());
        if (res.statusCode() / 100 != 2) throw new RuntimeException("Texto->voz HTTP " + res.statusCode());
        byte[] b = res.body();
        int n = b.length / 2;
        short[] in = new short[n];
        for (int i = 0; i < n; i++) in[i] = (short) ((b[2 * i] & 0xFF) | (b[2 * i + 1] << 8));
        // 24 kHz -> 48 kHz (interpolacion lineal)
        short[] out = new short[n * 2];
        for (int i = 0; i < n; i++) {
            out[2 * i] = in[i];
            out[2 * i + 1] = (short) ((in[i] + (i + 1 < n ? in[i + 1] : in[i])) / 2);
        }
        return out;
    }

    // ------------------------------------------------------------ utilidades

    private static void field(ByteArrayOutputStream o, String boundary, String name, String value) throws Exception {
        o.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + name + "\"\r\n\r\n" + value + "\r\n").getBytes(StandardCharsets.UTF_8));
    }

    private static void le32(ByteArrayOutputStream o, int v) {
        o.write(v & 0xFF);
        o.write((v >> 8) & 0xFF);
        o.write((v >> 16) & 0xFF);
        o.write((v >> 24) & 0xFF);
    }

    private static void le16(ByteArrayOutputStream o, int v) {
        o.write(v & 0xFF);
        o.write((v >> 8) & 0xFF);
    }

    private static String cut(String s) {
        return s == null ? "" : s.length() > 200 ? s.substring(0, 200) : s;
    }
}
