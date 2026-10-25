package com.carpinchoia.ai;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/** Memoria: cada carpincho guarda TODA su conversacion en un archivo (como un chat). */
public class AiMemory {
    private static final Gson GSON = new Gson();

    private static Path file(UUID id) throws Exception {
        Path dir = FMLPaths.CONFIGDIR.get().resolve("carpincho_ia").resolve("memoria");
        Files.createDirectories(dir);
        return dir.resolve(id + ".json");
    }

    private static JsonArray readAll(UUID id) {
        try {
            Path f = file(id);
            if (Files.exists(f)) return JsonParser.parseString(Files.readString(f)).getAsJsonArray();
        } catch (Exception ignored) {
        }
        return new JsonArray();
    }

    public static synchronized void append(UUID id, String role, String content) {
        try {
            JsonArray all = readAll(id);
            JsonObject m = new JsonObject();
            m.addProperty("role", role);
            m.addProperty("content", content);
            all.add(m);
            Files.writeString(file(id), GSON.toJson(all));
        } catch (Exception e) {
            System.err.println("[carpincho_ia] No pude guardar la memoria: " + e.getMessage());
        }
    }

    /** Los ultimos mensajes (para mandarlos a Claude). Siempre empieza con un mensaje del jugador. */
    public static synchronized JsonArray recent(UUID id, int max) {
        JsonArray all = readAll(id);
        int from = Math.max(0, all.size() - max);
        JsonArray out = new JsonArray();
        for (int i = from; i < all.size(); i++) out.add(all.get(i));
        while (out.size() > 0 && !out.get(0).getAsJsonObject().get("role").getAsString().equals("user")) out.remove(0);
        return out;
    }

    public static synchronized void delete(UUID id) {
        try {
            Files.deleteIfExists(file(id));
        } catch (Exception ignored) {
        }
    }
}
