package com.carpinchoia;

import com.carpinchoia.client.ClientHooks;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class ModNet {
    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(CarpinchoIA.MODID, "main"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, Settings.class, Settings::encode, Settings::decode, Settings::handle);
        CHANNEL.registerMessage(id++, ListRequest.class, ListRequest::encode, ListRequest::decode, ListRequest::handle);
        CHANNEL.registerMessage(id++, ListResponse.class, ListResponse::encode, ListResponse::decode, ListResponse::handle);
        CHANNEL.registerMessage(id++, Save.class, Save::encode, Save::decode, Save::handle);
        CHANNEL.registerMessage(id++, Momento.class, Momento::encode, Momento::decode, Momento::handle);
    }

    public static void toPlayer(ServerPlayer p, Object msg) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), msg);
    }

    public static void toAll(Object msg) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), msg);
    }

    /** Datos de un carpincho tal como los ve el cliente. */
    public record Info(UUID id, String name, String dim, int x, int y, int z, int radius, String owners, boolean loaded) {
    }

    /** Configuracion "Carpincho" del jugador (cliente -> servidor). */
    public static class Settings {
        final boolean sound, alerts, steps;

        public Settings(boolean sound, boolean alerts, boolean steps) {
            this.sound = sound;
            this.alerts = alerts;
            this.steps = steps;
        }

        static void encode(Settings m, FriendlyByteBuf b) {
            b.writeBoolean(m.sound);
            b.writeBoolean(m.alerts);
            b.writeBoolean(m.steps);
        }

        static Settings decode(FriendlyByteBuf b) {
            return new Settings(b.readBoolean(), b.readBoolean(), b.readBoolean());
        }

        static void handle(Settings m, Supplier<NetworkEvent.Context> c) {
            ServerPlayer p = c.get().getSender();
            c.get().enqueueWork(() -> {
                if (p == null) return;
                ServerEvents.Settings s = ServerEvents.settings(p);
                s.sound = m.sound;
                s.alerts = m.alerts;
                s.steps = m.steps;
            });
            c.get().setPacketHandled(true);
        }
    }

    /** Pide la lista de carpinchos (cliente -> servidor). */
    public static class ListRequest {
        static void encode(ListRequest m, FriendlyByteBuf b) {
        }

        static ListRequest decode(FriendlyByteBuf b) {
            return new ListRequest();
        }

        static void handle(ListRequest m, Supplier<NetworkEvent.Context> c) {
            ServerPlayer p = c.get().getSender();
            c.get().enqueueWork(() -> {
                if (p != null) ServerEvents.sendList(p);
            });
            c.get().setPacketHandled(true);
        }
    }

    /** Lista de carpinchos (servidor -> cliente). */
    public static class ListResponse {
        final List<Info> list;

        public ListResponse(List<Info> list) {
            this.list = list;
        }

        static void encode(ListResponse m, FriendlyByteBuf b) {
            b.writeVarInt(m.list.size());
            for (Info i : m.list) {
                b.writeUUID(i.id());
                b.writeUtf(i.name(), 64);
                b.writeUtf(i.dim(), 64);
                b.writeInt(i.x());
                b.writeInt(i.y());
                b.writeInt(i.z());
                b.writeInt(i.radius());
                b.writeUtf(i.owners(), 512);
                b.writeBoolean(i.loaded());
            }
        }

        static ListResponse decode(FriendlyByteBuf b) {
            int n = b.readVarInt();
            List<Info> l = new ArrayList<>();
            for (int k = 0; k < n; k++) {
                l.add(new Info(b.readUUID(), b.readUtf(64), b.readUtf(64), b.readInt(), b.readInt(), b.readInt(),
                        b.readInt(), b.readUtf(512), b.readBoolean()));
            }
            return new ListResponse(l);
        }

        static void handle(ListResponse m, Supplier<NetworkEvent.Context> c) {
            c.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.openList(m.list)));
            c.get().setPacketHandled(true);
        }
    }

    /** Crear (id = 0) o modificar un carpincho (cliente -> servidor). */
    public static class Save {
        final UUID id;
        final String name, owners;
        final int x, y, z, radius;

        public Save(UUID id, String name, int x, int y, int z, int radius, String owners) {
            this.id = id;
            this.name = name;
            this.x = x;
            this.y = y;
            this.z = z;
            this.radius = radius;
            this.owners = owners;
        }

        static void encode(Save m, FriendlyByteBuf b) {
            b.writeUUID(m.id);
            b.writeUtf(m.name, 64);
            b.writeInt(m.x);
            b.writeInt(m.y);
            b.writeInt(m.z);
            b.writeInt(m.radius);
            b.writeUtf(m.owners, 512);
        }

        static Save decode(FriendlyByteBuf b) {
            return new Save(b.readUUID(), b.readUtf(64), b.readInt(), b.readInt(), b.readInt(), b.readInt(), b.readUtf(512));
        }

        static void handle(Save m, Supplier<NetworkEvent.Context> c) {
            ServerPlayer p = c.get().getSender();
            c.get().enqueueWork(() -> {
                if (p != null) ServerEvents.handleSave(p, m.id, m.name, m.x, m.y, m.z, m.radius, m.owners);
            });
            c.get().setPacketHandled(true);
        }
    }

    /** Estado del Momento Carpincho (servidor -> cliente). */
    public static class Momento {
        final long remaining;
        final boolean animate;

        public Momento(long remaining, boolean animate) {
            this.remaining = remaining;
            this.animate = animate;
        }

        static void encode(Momento m, FriendlyByteBuf b) {
            b.writeLong(m.remaining);
            b.writeBoolean(m.animate);
        }

        static Momento decode(FriendlyByteBuf b) {
            return new Momento(b.readLong(), b.readBoolean());
        }

        static void handle(Momento m, Supplier<NetworkEvent.Context> c) {
            c.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.momento(m.remaining, m.animate)));
            c.get().setPacketHandled(true);
        }
    }
}
