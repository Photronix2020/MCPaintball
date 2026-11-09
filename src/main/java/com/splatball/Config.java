package com.splatball;

import net.minecraftforge.common.ForgeConfigSpec;

public final class Config {
    private static final ForgeConfigSpec.Builder B = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue PAINT_BLOCKS = B
            .comment("Paintballs recolour concrete, wool, terracotta, stained glass and carpet.")
            .define("paintBlocks", true);
    public static final ForgeConfigSpec.BooleanValue PAINT_ANYWHERE = B
            .comment("Allow painting blocks outside the arena too. Off by default so builds are safe.")
            .define("paintOutsideArena", false);
    public static final ForgeConfigSpec.IntValue KILLS_TO_WIN = B
            .comment("Splats a team needs to win when /paintball start is run without a number.")
            .defineInRange("killsToWin", 25, 1, 500);
    public static final ForgeConfigSpec.IntValue MATCH_MINUTES = B
            .comment("Match time limit in minutes.")
            .defineInRange("matchMinutes", 8, 1, 60);
    public static final ForgeConfigSpec.DoubleValue DAMAGE_MULTIPLIER = B
            .comment("Multiplier applied to all paintball damage.")
            .defineInRange("damageMultiplier", 1.0, 0.1, 10.0);

    public static final ForgeConfigSpec.IntValue BOT_FILL = B
            .comment("Bots fill each team up to this many members and step aside as players join. 0 turns bots off.")
            .defineInRange("botsFillTeamTo", 4, 0, 16);

    public static final ForgeConfigSpec.BooleanValue AUTO_PLAY = B
            .comment("In a brand-new world, build the arena automatically and drop every player into the match when they log in.",
                    "Worlds that existed before the mod was added are never touched; use a Paintball Station there.")
            .define("autoPlay", true);

    public static final ForgeConfigSpec SPEC = B.build();

    private Config() {}
}
