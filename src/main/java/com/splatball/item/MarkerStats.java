package com.splatball.item;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

/**
 * Tuning for one paint marker.
 *
 * @param damage      health points per paintball (a player has 20)
 * @param speed       blocks per tick
 * @param spread      inaccuracy; 0 is a laser
 * @param pellets     paintballs per shot
 * @param cooldown    ticks between shots (20 ticks = 1 second)
 * @param magazine    shots per reload
 * @param reloadTicks reload time in ticks
 * @param gravity     drop per tick
 * @param splash      splash radius in blocks, 0 for none
 * @param zoom        FOV multiplier while sneaking, 1 for none
 */
public record MarkerStats(float damage, float speed, float spread, int pellets, int cooldown, int magazine,
                          int reloadTicks, float gravity, float splash, float zoom, SoundEvent sound, float pitch) {

    public static final MarkerStats PISTOL = new MarkerStats(5F, 3.0F, 1.0F, 1, 5, 12, 25, 0.02F, 0F, 1F,
            SoundEvents.SNOW_GOLEM_SHOOT, 1.4F);
    public static final MarkerStats SMG = new MarkerStats(3F, 3.0F, 3.0F, 1, 2, 30, 40, 0.02F, 0F, 1F,
            SoundEvents.SNOW_GOLEM_SHOOT, 1.8F);
    public static final MarkerStats SHOTGUN = new MarkerStats(3F, 2.5F, 9.0F, 7, 18, 5, 45, 0.03F, 0F, 1F,
            SoundEvents.FIREWORK_ROCKET_BLAST, 0.8F);
    public static final MarkerStats SNIPER = new MarkerStats(16F, 5.0F, 0.1F, 1, 30, 4, 50, 0.005F, 0F, 0.25F,
            SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 1.3F);
    public static final MarkerStats LAUNCHER = new MarkerStats(10F, 1.4F, 1.0F, 1, 25, 3, 60, 0.05F, 3.5F, 1F,
            SoundEvents.FIREWORK_ROCKET_LAUNCH, 0.7F);
}
