package com.carpinchoia.voice;

import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.AudioPlayer;
import de.maxhenkel.voicechat.api.audiochannel.EntityAudioChannel;
import de.maxhenkel.voicechat.api.opus.OpusEncoder;
import net.minecraft.world.entity.Entity;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/** Hace que el carpincho "hable" por el Simple Voice Chat (sale de su posicion). */
public class VoiceBridge {
    public static void speak(Entity carpincho, short[] pcm48k) {
        VoicechatServerApi api = CarpinchoVoicePlugin.api();
        if (api == null || pcm48k == null || pcm48k.length == 0) return;
        EntityAudioChannel channel = api.createEntityAudioChannel(UUID.randomUUID(), api.fromEntity(carpincho));
        if (channel == null) return;
        OpusEncoder encoder = api.createEncoder();
        int[] pos = {0};
        Supplier<short[]> frames = () -> {
            if (pos[0] >= pcm48k.length) return null;
            short[] f = new short[960];
            System.arraycopy(pcm48k, pos[0], f, 0, Math.min(960, pcm48k.length - pos[0]));
            pos[0] += 960;
            return f;
        };
        AudioPlayer player = api.createAudioPlayer(channel, encoder, frames);
        player.startPlaying();
        long ms = pcm48k.length * 1000L / 48000L + 2000L;
        CarpinchoVoicePlugin.scheduler().schedule(encoder::close, ms, TimeUnit.MILLISECONDS);
    }
}
