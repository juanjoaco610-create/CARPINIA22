package com.carpinchoia;

import com.carpinchoia.CarpinchoData.Entry;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ServerEvents {

    // ------------------------------------------------------------ configuracion por jugador

    public static class Settings {
        public boolean sound = true, alerts = true, steps = true;
    }

    private static final Map<UUID, Settings> SETTINGS = new HashMap<>();

    public static Settings settings(ServerPlayer p) {
        return SETTINGS.computeIfAbsent(p.getUUID(), k -> new Settings());
    }

    /** Reproduce un sonido del carpincho solo a los jugadores que lo tienen activado. */
    public static void playFor(Entity src, SoundEvent sound, float vol, float pitch, boolean step) {
        if (!(src.level() instanceof ServerLevel lvl)) return;
        Holder<SoundEvent> h = BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound);
        for (ServerPlayer p : lvl.players()) {
            if (p.distanceToSqr(src) > 256) continue;
            Settings st = settings(p);
            if (step ? !st.steps : !st.sound) continue;
            p.connection.send(new ClientboundSoundPacket(h, SoundSource.NEUTRAL, src.getX(), src.getY(), src.getZ(),
                    vol, pitch, lvl.random.nextLong()));
        }
    }

    // ------------------------------------------------------------ utilidades

    private static Entity findEntity(MinecraftServer s, UUID id) {
        for (ServerLevel l : s.getAllLevels()) {
            Entity e = l.getEntity(id);
            if (e != null) return e;
        }
        return null;
    }

    public static void sendList(ServerPlayer p) {
        if (!p.hasPermissions(2)) {
            p.sendSystemMessage(Component.literal("Solo los administradores pueden modificar carpinchos.").withStyle(ChatFormatting.RED));
            return;
        }
        MinecraftServer s = p.getServer();
        List<ModNet.Info> list = new ArrayList<>();
        for (Entry e : CarpinchoData.get(s).map.values()) {
            list.add(new ModNet.Info(e.id, e.name, e.dim, e.x, e.y, e.z, e.radius, String.join(", ", e.owners),
                    findEntity(s, e.id) != null));
        }
        ModNet.toPlayer(p, new ModNet.ListResponse(list));
    }

    private static void msg(ServerPlayer p, String text, boolean ok) {
        p.sendSystemMessage(Component.literal(text).withStyle(ok ? ChatFormatting.GREEN : ChatFormatting.RED));
    }

    // ------------------------------------------------------------ crear / modificar

    public static void handleSave(ServerPlayer p, UUID id, String rawName, int x, int y, int z, int radius, String rawOwners) {
        if (!p.hasPermissions(2)) {
            msg(p, "Solo los administradores pueden hacer esto.", false);
            return;
        }
        MinecraftServer s = p.getServer();
        CarpinchoData reg = CarpinchoData.get(s);
        boolean create = id.getMostSignificantBits() == 0 && id.getLeastSignificantBits() == 0;
        String name = rawName.trim();
        if (name.isEmpty() || name.length() > 32) {
            msg(p, "El nombre debe tener entre 1 y 32 letras.", false);
            return;
        }
        if (reg.byName(name, create ? null : id) != null) {
            msg(p, "Ya existe un carpincho llamado \"" + name + "\". Elige otro nombre.", false);
            return;
        }
        radius = Math.max(1, Math.min(200, radius));
        List<String> owners = new ArrayList<>();
        for (String o : rawOwners.split(",")) {
            String t = o.trim();
            if (!t.isEmpty() && owners.stream().noneMatch(t::equalsIgnoreCase)) owners.add(t);
        }
        if (create && owners.isEmpty()) owners.add(p.getGameProfile().getName());

        if (create) {
            ServerLevel lvl = p.serverLevel();
            CarpinchoEntity e = CarpinchoIA.CARPINCHO.get().create(lvl);
            if (e == null) return;
            e.moveTo(x + 0.5, y, z + 0.5, 0, 0);
            Entry en = new Entry(e.getUUID(), name);
            en.dim = lvl.dimension().location().toString();
            en.x = x;
            en.y = y;
            en.z = z;
            en.radius = radius;
            en.owners = owners;
            reg.map.put(en.id, en);
            reg.setDirty();
            lvl.addFreshEntity(e);
            if (!p.getAbilities().instabuild) {
                for (InteractionHand h : InteractionHand.values()) {
                    ItemStack st = p.getItemInHand(h);
                    if (st.is(CarpinchoIA.EGG.get())) {
                        st.shrink(1);
                        break;
                    }
                }
            }
            msg(p, "Carpincho \"" + name + "\" creado.", true);
        } else {
            Entry en = reg.map.get(id);
            if (en == null) {
                msg(p, "Ese carpincho ya no existe.", false);
                return;
            }
            boolean moved = en.x != x || en.y != y || en.z != z;
            en.name = name;
            en.x = x;
            en.y = y;
            en.z = z;
            en.radius = radius;
            en.owners = owners;
            reg.setDirty();
            Entity ent = findEntity(s, id);
            if (moved && ent != null) ent.teleportTo(x + 0.5, y, z + 0.5);
            msg(p, "Carpincho \"" + name + "\" actualizado.", true);
            sendList(p);
        }
    }

    // ------------------------------------------------------------ Momento Carpincho

    public static void startMomento(MinecraftServer s) {
        CarpinchoData reg = CarpinchoData.get(s);
        reg.momentoEnd = System.currentTimeMillis() + CarpinchoIA.MOMENTO_MILLIS;
        reg.setDirty();
        ModNet.toAll(new ModNet.Momento(CarpinchoIA.MOMENTO_MILLIS, true));
        // titulo marron que dura unos 3 segundos
        Component title = Component.literal("MOMENTO CARPINCHO")
                .withStyle(Style.EMPTY.withColor(0x8B5A2B).withBold(true));
        for (ServerPlayer p : s.getPlayerList().getPlayers()) {
            p.connection.send(new ClientboundSetTitlesAnimationPacket(5, 40, 15));
            p.connection.send(new ClientboundSetTitleTextPacket(title));
        }
    }

    public static void stopMomento(MinecraftServer s, boolean natural) {
        CarpinchoData reg = CarpinchoData.get(s);
        reg.momentoEnd = 0;
        reg.setDirty();
        ModNet.toAll(new ModNet.Momento(0, false));
        s.getPlayerList().broadcastSystemMessage(Component.literal(
                natural ? "Terminó el Momento Carpincho." : "El Momento Carpincho fue cancelado.")
                .withStyle(Style.EMPTY.withColor(0x8B5A2B)), false);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || e.getServer().getTickCount() % 20 != 0) return;
        CarpinchoData reg = CarpinchoData.get(e.getServer());
        if (reg.momentoEnd != 0 && !reg.isMomento()) stopMomento(e.getServer(), true);
    }

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        CarpinchoData reg = CarpinchoData.get(p.getServer());
        if (reg.isMomento()) ModNet.toPlayer(p, new ModNet.Momento(reg.momentoRemaining(), false));
    }

    /** Si escribes el nombre del carpincho (o "carpincho") cerca de el, te responde. */
    @SubscribeEvent
    public void onChat(ServerChatEvent e) {
        ServerPlayer p = e.getPlayer();
        String text = e.getRawText();
        String low = text.toLowerCase();
        CarpinchoData reg = CarpinchoData.get(p.getServer());
        CarpinchoEntity best = null;
        for (CarpinchoEntity c : p.serverLevel().getEntitiesOfClass(CarpinchoEntity.class, p.getBoundingBox().inflate(12))) {
            Entry en = reg.map.get(c.getUUID());
            if (en == null) continue;
            boolean named = low.contains(en.name.toLowerCase()) || low.contains("carpincho");
            if (named && (best == null || c.distanceTo(p) < best.distanceTo(p))) best = c;
        }
        if (best != null) com.carpinchoia.ai.AiService.handleText(best, p, text);
    }

    // ------------------------------------------------------------ comandos

    private static LiteralArgumentBuilder<CommandSourceStack> momentoNode(String lit) {
        return Commands.literal(lit).requires(src -> src.hasPermission(2))
                .executes(c -> {
                    startMomento(c.getSource().getServer());
                    return 1;
                })
                .then(Commands.literal("parar").executes(c -> {
                    stopMomento(c.getSource().getServer(), false);
                    return 1;
                }));
    }

    @SubscribeEvent
    public void onCommands(RegisterCommandsEvent e) {
        // /momentocarpincho   y tambien   /momento carpincho
        e.getDispatcher().register(momentoNode("momentocarpincho"));
        e.getDispatcher().register(Commands.literal("momento").requires(src -> src.hasPermission(2))
                .then(momentoNode("carpincho")));

        e.getDispatcher().register(Commands.literal("eliminarcarpincho").requires(src -> src.hasPermission(2))
                .then(Commands.argument("nombre", StringArgumentType.greedyString())
                        .suggests((ctx, b) -> {
                            List<String> names = new ArrayList<>();
                            for (Entry en : CarpinchoData.get(ctx.getSource().getServer()).map.values()) names.add(en.name);
                            return SharedSuggestionProvider.suggest(names, b);
                        })
                        .executes(ctx -> {
                            MinecraftServer s = ctx.getSource().getServer();
                            CarpinchoData reg = CarpinchoData.get(s);
                            String name = StringArgumentType.getString(ctx, "nombre").trim();
                            Entry found = reg.byName(name, null);
                            if (found == null) {
                                ctx.getSource().sendFailure(Component.literal("No existe un carpincho llamado \"" + name + "\"."));
                                return 0;
                            }
                            reg.map.remove(found.id);
                            reg.setDirty();
                            Entity ent = findEntity(s, found.id);
                            if (ent != null) ent.discard();
                            com.carpinchoia.ai.AiMemory.delete(found.id);
                            ctx.getSource().sendSuccess(() -> Component.literal("Carpincho \"" + found.name + "\" eliminado."), true);
                            return 1;
                        })));
    }
}
