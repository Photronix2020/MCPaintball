package com.splatball.entity;

import com.splatball.Config;
import com.splatball.Paint;
import com.splatball.Splatball;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class Paintball extends ThrowableProjectile implements ItemSupplier {
    private static final EntityDataAccessor<Integer> COLOR =
            SynchedEntityData.defineId(Paintball.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> GRAVITY =
            SynchedEntityData.defineId(Paintball.class, EntityDataSerializers.FLOAT);
    private static final int MAX_AGE = 100;

    private float damage = 4.0F;
    private float splash = 0.0F;

    public Paintball(EntityType<? extends Paintball> type, Level level) {
        super(type, level);
    }

    public Paintball(Level level, LivingEntity owner, DyeColor color, float damage, float gravity, float splash) {
        super(Splatball.PAINTBALL_ENTITY.get(), owner, level);
        this.damage = damage;
        this.splash = splash;
        this.entityData.set(COLOR, color.getId());
        this.entityData.set(GRAVITY, gravity);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(COLOR, DyeColor.WHITE.getId());
        this.entityData.define(GRAVITY, 0.03F);
    }

    public DyeColor getColor() {
        return DyeColor.byId(this.entityData.get(COLOR));
    }

    @Override
    protected float getGravity() {
        return this.entityData.get(GRAVITY);
    }

    @Override
    public ItemStack getItem() {
        ItemStack stack = new ItemStack(Splatball.PAINTBALL.get());
        stack.getOrCreateTag().putInt("Color", this.entityData.get(COLOR));
        return stack;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            level().addParticle(new DustParticleOptions(Paint.rgb(getColor()), 0.6F), getX(), getY(), getZ(), 0, 0, 0);
        } else if (this.tickCount > MAX_AGE) {
            discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!level().isClientSide) splat(result.getEntity(), damage);
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (level() instanceof ServerLevel server) Paint.paintBlock(server, result.getBlockPos(), getColor());
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!(level() instanceof ServerLevel server)) return;
        Vec3 p = result.getLocation();
        DyeColor color = getColor();
        if (splash > 0) {
            Entity direct = result instanceof EntityHitResult ehr ? ehr.getEntity() : null;
            Entity owner = getOwner();
            for (LivingEntity e : server.getEntitiesOfClass(LivingEntity.class, new AABB(p, p).inflate(splash))) {
                if (e == direct || e == owner) continue;
                double dist = Math.sqrt(e.distanceToSqr(p));
                if (dist <= splash) splat(e, damage * (float) (1.0 - 0.6 * dist / splash));
            }
            BlockPos center = BlockPos.containing(p);
            int r = Mth.ceil(splash) - 1;
            for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, -r, -r), center.offset(r, r, r))) {
                if (pos.distSqr(center) <= r * r + 1) Paint.paintBlock(server, pos, color);
            }
        }
        Paint.burst(server, p, color, splash > 0 ? 80 : 12, splash > 0 ? 1.2 : 0.15);
        server.playSound(null, p.x, p.y, p.z, SoundEvents.SLIME_BLOCK_BREAK, SoundSource.PLAYERS,
                splash > 0 ? 1.2F : 0.6F, 1.2F + this.random.nextFloat() * 0.4F);
        discard();
    }

    private void splat(Entity target, float amount) {
        if (!(target instanceof LivingEntity living)) return;
        Entity owner = getOwner();
        if (owner != null && owner != target && owner.isAlliedTo(target)) return;   // no friendly fire
        // paintballs ignore hurt-immunity frames so every pellet of a burst counts
        living.invulnerableTime = 0;
        boolean hurt = living.hurt(Paint.damageSource(level(), this, owner),
                amount * Config.DAMAGE_MULTIPLIER.get().floatValue());
        if (hurt && owner instanceof ServerPlayer shooter && target != owner) {
            // the "ding" arrows play for the shooter on a hit
            shooter.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.ARROW_HIT_PLAYER, 0.0F));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Color", this.entityData.get(COLOR));
        tag.putFloat("Gravity", this.entityData.get(GRAVITY));
        tag.putFloat("Damage", damage);
        tag.putFloat("Splash", splash);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Color")) this.entityData.set(COLOR, tag.getInt("Color"));
        if (tag.contains("Gravity")) this.entityData.set(GRAVITY, tag.getFloat("Gravity"));
        if (tag.contains("Damage")) damage = tag.getFloat("Damage");
        if (tag.contains("Splash")) splash = tag.getFloat("Splash");
    }
}
