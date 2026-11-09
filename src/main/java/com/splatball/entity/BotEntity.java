package com.splatball.entity;

import com.splatball.Paint;
import com.splatball.game.Match;
import com.splatball.item.MarkerItem;
import com.splatball.item.MarkerStats;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * A computer-controlled paintball player. Bots fill out the teams during a match: they push toward the
 * enemy base, shoot enemies they can see with the marker they are holding, and respawn like players.
 * A bot that is not part of the running match removes itself.
 */
public class BotEntity extends PathfinderMob implements RangedAttackMob {
    /** Bots hit softer and spray wider than a player with the same marker. */
    private static final float DAMAGE_SCALE = 0.6F;
    private static final float EXTRA_SPREAD = 4.0F;

    public BotEntity(EntityType<? extends BotEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 40.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new RangedAttackGoal(this, 1.0, 12, 30, 22.0F));
        this.goalSelector.addGoal(2, new AdvanceGoal(this));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false,
                e -> Match.isEnemy(this, e)));
    }

    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        MarkerStats stats = getMainHandItem().getItem() instanceof MarkerItem marker ? marker.stats() : MarkerStats.PISTOL;
        DyeColor color = Paint.colorFor(this);
        for (int i = 0; i < stats.pellets(); i++) {
            Paintball ball = new Paintball(level(), this, color, stats.damage() * DAMAGE_SCALE, stats.gravity(), stats.splash());
            double dx = target.getX() - getX();
            double dy = target.getY(0.6) - ball.getY();
            double dz = target.getZ() - getZ();
            ball.shoot(dx, dy, dz, stats.speed(), stats.spread() + EXTRA_SPREAD);
            level().addFreshEntity(ball);
        }
        playSound(stats.sound(), 0.8F, stats.pitch());
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && this.tickCount % 20 == 0 && !Match.ownsBot(this)) discard();
    }

    @Override
    protected ResourceLocation getDefaultLootTable() {
        return BuiltInLootTables.EMPTY;
    }

    @Override
    public boolean shouldDropExperience() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    /** With nobody to shoot, walk a short leg toward the enemy base. */
    private static class AdvanceGoal extends Goal {
        private final BotEntity bot;

        AdvanceGoal(BotEntity bot) {
            this.bot = bot;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return bot.getTarget() == null && bot.getNavigation().isDone() && bot.getRandom().nextInt(10) == 0;
        }

        @Override
        public void start() {
            Vec3 point = Match.waypointFor(bot);
            if (point != null) bot.getNavigation().moveTo(point.x, point.y, point.z, 1.0);
        }

        @Override
        public boolean canContinueToUse() {
            return bot.getTarget() == null && !bot.getNavigation().isDone();
        }
    }
}
