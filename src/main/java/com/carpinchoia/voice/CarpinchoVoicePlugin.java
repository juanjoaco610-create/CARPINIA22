package com.carpinchoia.voice;

import com.carpinchoia.CarpinchoEntity;
import com.carpinchoia.CarpinchoIA;
import com.carpinchoia.ai.AiConfig;
import com.carpinchoia.ai.AiService;
import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** Plugin del Simple Voice Chat: escucha lo que dices cerca de un carpincho. */
@ForgeVoicechatPlugin
public class CarpinchoVoicePlugin implements VoicechatPlugin {
    private static volatile VoicechatServerApi serverApi;
    private static final ScheduledExecutorService SCHED = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "carpincho-voz");
        t.setDaemon(true);
        return t;
    });
    private static final Map<UUID, Rec> RECS = new ConcurrentHashMap<>();

    static {
        SCHED.scheduleAtFixedRate(CarpinchoVoicePlugin::flush, 300, 300, TimeUnit.MILLISECONDS);
    }

    private static class Rec {
        final OpusDecoder decoder;
        ServerPlayer player;
        final List<short[]> frames = new ArrayList<>();
        int samples;
        long last;
        boolean ended;

        Rec(OpusDecoder d) {
            this.decoder = d;
        }
    }

    public static VoicechatServerApi api() {
        return serverApi;
    }

    public static ScheduledExecutorService scheduler() {
        return SCHED;
    }

    @Override
    public String getPluginId() {
        return CarpinchoIA.MODID;
    }

    @Override
    public void initialize(VoicechatApi api) {
    }

    @Override
    public void registerEvents(EventRegistration reg) {
        reg.registerEvent(VoicechatServerStartedEvent.class, e -> serverApi = e.getVoicechat());
        reg.registerEvent(MicrophonePacketEvent.class, this::onMic);
    }

    private void onMic(MicrophonePacketEvent e) {
        VoicechatConnection con = e.getSenderConnection();
        if (con == null || serverApi == null) return;
        if (!(con.getPlayer().getPlayer() instanceof ServerPlayer sp)) return;
        byte[] data = e.getPacket().getOpusEncodedData();
        Rec r = RECS.computeIfAbsent(sp.getUUID(), k -> new Rec(serverApi.createDecoder()));
        synchronized (r) {
            r.player = sp;
            if (data == null || data.length == 0) {
                r.ended = true; // fin de la transmision
            } else if (r.samples < 48000 * 20) { // maximo 20 segundos
                short[] pcm = r.decoder.decode(data);
                r.frames.add(pcm);
                r.samples += pcm.length;
                r.last = System.currentTimeMillis();
            }
        }
    }

    /** Cada 300 ms: si alguien termino de hablar (silencio de ~1 s), procesa lo que dijo. */
    private static void flush() {
        long now = System.currentTimeMillis();
        for (Rec r : RECS.values()) {
            short[] pcm = null;
            ServerPlayer sp;
            synchronized (r) {
                sp = r.player;
                if (r.frames.isEmpty() || (!r.ended && now - r.last < 1000)) continue;
                pcm = new short[r.samples];
                int p = 0;
                for (short[] f : r.frames) {
                    System.arraycopy(f, 0, pcm, p, f.length);
                    p += f.length;
                }
                r.frames.clear();
                r.samples = 0;
                r.ended = false;
            }
            if (sp == null || pcm.length < 48000 / 2) continue; // menos de medio segundo: ruido
            final short[] audio = pcm;
            final ServerPlayer player = sp;
            player.getServer().execute(() -> {
                double max = AiConfig.get().hearRadius;
                List<CarpinchoEntity> near = player.serverLevel().getEntitiesOfClass(CarpinchoEntity.class,
                        player.getBoundingBox().inflate(max));
                CarpinchoEntity best = null;
                for (CarpinchoEntity c : near) {
                    if (c.distanceTo(player) <= max && (best == null || c.distanceTo(player) < best.distanceTo(player))) best = c;
                }
                if (best != null) AiService.handleVoice(best, player, audio);
            });
        }
    }
}
