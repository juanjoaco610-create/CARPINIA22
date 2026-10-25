package com.carpinchoia;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Datos de todos los carpinchos del servidor + estado del Momento Carpincho. */
public class CarpinchoData extends SavedData {
    public static class Entry {
        public final UUID id;
        public String name;
        public String dim = "minecraft:overworld";
        public int x, y, z, radius = 40;
        public List<String> owners = new ArrayList<>();

        public Entry(UUID id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    public final Map<UUID, Entry> map = new LinkedHashMap<>();
    public long momentoEnd = 0;

    public static CarpinchoData get(MinecraftServer s) {
        return s.overworld().getDataStorage().computeIfAbsent(CarpinchoData::load, CarpinchoData::new, "carpincho_ia");
    }

    public Entry byName(String name, UUID except) {
        for (Entry e : map.values()) {
            if (e.name.equalsIgnoreCase(name) && !e.id.equals(except)) return e;
        }
        return null;
    }

    public boolean isMomento() {
        return momentoEnd > System.currentTimeMillis();
    }

    public long momentoRemaining() {
        return Math.max(0, momentoEnd - System.currentTimeMillis());
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Entry e : map.values()) {
            CompoundTag t = new CompoundTag();
            t.putUUID("id", e.id);
            t.putString("name", e.name);
            t.putString("dim", e.dim);
            t.putInt("x", e.x);
            t.putInt("y", e.y);
            t.putInt("z", e.z);
            t.putInt("radius", e.radius);
            t.putString("owners", String.join(",", e.owners));
            list.add(t);
        }
        tag.put("carpinchos", list);
        tag.putLong("momentoEnd", momentoEnd);
        return tag;
    }

    public static CarpinchoData load(CompoundTag tag) {
        CarpinchoData d = new CarpinchoData();
        ListTag list = tag.getList("carpinchos", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            Entry e = new Entry(t.getUUID("id"), t.getString("name"));
            e.dim = t.getString("dim");
            e.x = t.getInt("x");
            e.y = t.getInt("y");
            e.z = t.getInt("z");
            e.radius = t.getInt("radius");
            for (String o : t.getString("owners").split(",")) if (!o.isBlank()) e.owners.add(o.trim());
            d.map.put(e.id, e);
        }
        d.momentoEnd = tag.getLong("momentoEnd");
        return d;
    }
}
