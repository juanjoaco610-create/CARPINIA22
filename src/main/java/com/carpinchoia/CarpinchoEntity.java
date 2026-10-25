package com.carpinchoia;

import com.carpinchoia.CarpinchoData.Entry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** El carpincho guardian. Su configuracion vive en CarpinchoData. */
public class CarpinchoEntity extends PathfinderMob {
    private static final EntityDataAccessor<Integer> RADIUS =
            SynchedEntityData.defineId(CarpinchoEntity.class, EntityDataSerializers.INT);
    private final Set<UUID> inside = new HashSet<>();

    public CarpinchoEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(RADIUS, 40);
    }

    /** Radio de bloques permitido (se muestra bajo el nombre). */
    public int getRadius() {
        return this.entityData.get(RADIUS);
    }

    public static AttributeSupplier.Builder attributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.22D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.3D, true));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.9D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        // Solo ataca durante el Momento Carpincho, y solo a intrusos dentro de la base
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, this::isDefenseTarget));
    }

    // ------------------------------------------------------------ logica de base

    private boolean insideBase(Entity e, Entry d) {
        if (!e.level().dimension().location().toString().equals(d.dim)) return false;
        double dx = e.getX() - (d.x + 0.5);
        double dz = e.getZ() - (d.z + 0.5);
        return dx * dx + dz * dz <= (double) d.radius * d.radius;
    }

    private boolean isOwner(Player p, Entry d) {
        String n = p.getGameProfile().getName();
        for (String o : d.owners) if (o.equalsIgnoreCase(n)) return true;
        return false;
    }

    private boolean isDefenseTarget(LivingEntity t) {
        if (!(t instanceof Player p) || level().isClientSide) return false;
        MinecraftServer s = level().getServer();
        if (s == null) return false;
        CarpinchoData reg = CarpinchoData.get(s);
        Entry d = reg.map.get(getUUID());
        return d != null && reg.isMomento() && !p.isCreative() && !p.isSpectator()
                && !isOwner(p, d) && insideBase(p, d);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (tickCount % 20 != 0) return;
        MinecraftServer s = level().getServer();
        if (s == null) return;
        Entry d = CarpinchoData.get(s).map.get(getUUID());
        if (d == null) { // fue eliminado con /eliminarcarpincho
            discard();
            return;
        }
        if (getCustomName() == null || !getCustomName().getString().equals(d.name)) setCustomName(Component.literal(d.name));
        setCustomNameVisible(true);
        restrictTo(new BlockPos(d.x, d.y, d.z), d.radius);
        if (getRadius() != d.radius) this.entityData.set(RADIUS, d.radius);

        // Alerta: un solo mensaje cuando alguien nuevo entra a la base
        for (ServerPlayer p : ((ServerLevel) level()).players()) {
            boolean in = !p.isSpectator() && insideBase(p, d) && !isOwner(p, d);
            if (in) {
                if (inside.add(p.getUUID())) alertOwners(s, d);
            } else {
                inside.remove(p.getUUID());
            }
        }
    }

    private void alertOwners(MinecraftServer s, Entry d) {
        for (String name : d.owners) {
            ServerPlayer o = s.getPlayerList().getPlayerByName(name);
            if (o != null && ServerEvents.settings(o).alerts) {
                o.sendSystemMessage(Component.literal("ALERTA: ¡Alguien ha entrado a tu base!")
                        .withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
            }
        }
    }

    // ------------------------------------------------------------ proteccion

    /** Solo los administradores (o /kill) pueden hacerle dano. */
    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
        if (source.getEntity() instanceof Player p && p.hasPermissions(2)) return false;
        return true;
    }

    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide && level().getServer() != null) {
            CarpinchoData reg = CarpinchoData.get(level().getServer());
            if (reg.map.remove(getUUID()) != null) reg.setDirty();
        }
        super.die(source);
    }

    // ------------------------------------------------------------ sonidos (segun la config de cada jugador)

    @Override
    public void playSound(SoundEvent sound, float volume, float pitch) {
        if (!level().isClientSide) ServerEvents.playFor(this, sound, volume, pitch, false);
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        if (!level().isClientSide) ServerEvents.playFor(this, SoundEvents.GRASS_STEP, 0.2F, 1.0F, true);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.RABBIT_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.RABBIT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.RABBIT_DEATH;
    }
}
