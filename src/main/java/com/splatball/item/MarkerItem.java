package com.splatball.item;

import com.splatball.Paint;
import com.splatball.entity.Paintball;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * A paintball gun. Firing is driven by a packet from the client while the use key is held
 * (see ClientEvents), so there is no vanilla "using item" slowdown.
 */
public class MarkerItem extends Item {
    private static final String AMMO = "Ammo";
    private static final String RELOAD_AT = "ReloadAt";
    private static final String NEXT_FIRE = "splatball_next_fire";

    private final MarkerStats stats;

    public MarkerItem(MarkerStats stats) {
        super(new Item.Properties().stacksTo(1));
        this.stats = stats;
    }

    public MarkerStats stats() {
        return stats;
    }

    public static int getAmmo(ItemStack stack) {
        if (!(stack.getItem() instanceof MarkerItem marker)) return 0;
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(AMMO) ? tag.getInt(AMMO) : marker.stats.magazine();
    }

    public static boolean isReloading(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(RELOAD_AT);
    }

    public static void refill(ItemStack stack) {
        if (!(stack.getItem() instanceof MarkerItem marker)) return;
        CompoundTag tag = stack.getOrCreateTag();
        tag.remove(RELOAD_AT);
        tag.putInt(AMMO, marker.stats.magazine());
    }

    /** Server side: fire one shot if cooldown, reload state and ammo allow it. */
    public void tryFire(ServerPlayer player, ItemStack stack) {
        if (player.isSpectator() || !player.isAlive() || isReloading(stack)) return;
        Level level = player.level();
        long now = level.getGameTime();
        CompoundTag data = player.getPersistentData();
        // one tick of leniency so network jitter does not eat shots
        if (now < data.getLong(NEXT_FIRE) - 1) return;

        int ammo = getAmmo(stack);
        if (ammo <= 0) {
            reload(player, stack);
            return;
        }
        data.putLong(NEXT_FIRE, now + stats.cooldown());
        stack.getOrCreateTag().putInt(AMMO, ammo - 1);

        DyeColor color = Paint.colorFor(player);
        for (int i = 0; i < stats.pellets(); i++) {
            Paintball ball = new Paintball(level, player, color, stats.damage(), stats.gravity(), stats.splash());
            ball.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, stats.speed(), stats.spread());
            level.addFreshEntity(ball);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), stats.sound(), SoundSource.PLAYERS,
                0.8F, stats.pitch() + level.random.nextFloat() * 0.1F);
        if (stats.cooldown() >= 15) player.getCooldowns().addCooldown(this, stats.cooldown());
        if (ammo - 1 <= 0) reload(player, stack);
    }

    /** Server side: start a reload. Ammo comes back when the timer ends (see inventoryTick). */
    public void reload(ServerPlayer player, ItemStack stack) {
        if (isReloading(stack) || getAmmo(stack) >= stats.magazine()) return;
        Level level = player.level();
        stack.getOrCreateTag().putLong(RELOAD_AT, level.getGameTime() + stats.reloadTicks());
        player.getCooldowns().addCooldown(this, stats.reloadTicks());
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.CROSSBOW_LOADING_END,
                SoundSource.PLAYERS, 0.7F, 1.2F);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide) return;
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(RELOAD_AT) && level.getGameTime() >= tag.getLong(RELOAD_AT)) {
            refill(stack);
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        // firing is handled by the fire packet; swallow the click so the off hand does nothing either
        return InteractionResultHolder.fail(player.getItemInHand(hand));
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        // ammo changes rewrite NBT every shot; don't bob the item for that
        return slotChanged || oldStack.getItem() != newStack.getItem();
    }

    /** Players hold a marker up and aimed, like a loaded crossbow, instead of at their side. */
    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
            @Override
            public net.minecraft.client.model.HumanoidModel.ArmPose getArmPose(net.minecraft.world.entity.LivingEntity entity,
                                                                               InteractionHand hand, ItemStack stack) {
                return net.minecraft.client.model.HumanoidModel.ArmPose.CROSSBOW_HOLD;
            }
        });
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getAmmo(stack) < stats.magazine();
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * getAmmo(stack) / stats.magazine());
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x55FF55;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag) {
        String dmg = stats.pellets() > 1 ? trim(stats.damage()) + " x" + stats.pellets() : trim(stats.damage());
        lines.add(Component.translatable("tooltip.splatball.damage", dmg).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("tooltip.splatball.magazine", stats.magazine()).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("tooltip.splatball.rate", trim(20.0F / stats.cooldown())).withStyle(ChatFormatting.GRAY));
        if (stats.splash() > 0) lines.add(Component.translatable("tooltip.splatball.splash").withStyle(ChatFormatting.GOLD));
        if (stats.zoom() < 1) lines.add(Component.translatable("tooltip.splatball.zoom").withStyle(ChatFormatting.GOLD));
        lines.add(Component.translatable("tooltip.splatball.use").withStyle(ChatFormatting.DARK_GRAY));
    }

    private static String trim(float v) {
        return v == Math.floor(v) ? Integer.toString((int) v) : String.format("%.1f", v);
    }
}
