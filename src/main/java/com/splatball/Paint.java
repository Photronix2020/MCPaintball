package com.splatball;

import com.splatball.game.Arena;
import com.splatball.game.ArenaData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Team;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/** Colour, damage and block-painting helpers. */
public final class Paint {
    public static final ResourceKey<DamageType> DAMAGE =
            ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(Splatball.MODID, "paintball"));

    private static final DyeColor[] FREE_COLORS = {
            DyeColor.YELLOW, DyeColor.LIME, DyeColor.ORANGE, DyeColor.MAGENTA,
            DyeColor.LIGHT_BLUE, DyeColor.PINK, DyeColor.CYAN, DyeColor.PURPLE
    };
    private static final String[] FAMILIES = {"_concrete", "_wool", "_terracotta", "_stained_glass", "_carpet"};
    private static Map<Block, Integer> familyOf;
    private static Block[][] byFamily;

    public static DamageSource damageSource(Level level, Entity direct, @Nullable Entity owner) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DAMAGE),
                direct, owner);
    }

    /** Team colour when on a coloured scoreboard team, otherwise a stable personal colour. */
    public static DyeColor colorFor(Entity player) {
        Team team = player.getTeam();
        if (team != null) {
            DyeColor c = fromFormatting(team.getColor());
            if (c != null) return c;
        }
        return FREE_COLORS[Math.floorMod(player.getUUID().hashCode(), FREE_COLORS.length)];
    }

    @Nullable
    private static DyeColor fromFormatting(ChatFormatting f) {
        return switch (f) {
            case RED, DARK_RED -> DyeColor.RED;
            case BLUE, DARK_BLUE -> DyeColor.BLUE;
            case GREEN -> DyeColor.LIME;
            case DARK_GREEN -> DyeColor.GREEN;
            case YELLOW -> DyeColor.YELLOW;
            case GOLD -> DyeColor.ORANGE;
            case AQUA -> DyeColor.LIGHT_BLUE;
            case DARK_AQUA -> DyeColor.CYAN;
            case LIGHT_PURPLE -> DyeColor.MAGENTA;
            case DARK_PURPLE -> DyeColor.PURPLE;
            case WHITE -> DyeColor.WHITE;
            case GRAY -> DyeColor.LIGHT_GRAY;
            case DARK_GRAY -> DyeColor.GRAY;
            case BLACK -> DyeColor.BLACK;
            default -> null;
        };
    }

    /** Recolours a dyeable block (concrete, wool, ...) in place. Returns true if it changed. */
    public static boolean paintBlock(ServerLevel level, BlockPos pos, DyeColor color) {
        if (!Config.PAINT_BLOCKS.get()) return false;
        if (!Config.PAINT_ANYWHERE.get()) {
            ArenaData data = ArenaData.get(level.getServer());
            if (data.origin == null || level.dimension() != data.dimension || !Arena.contains(data.origin, pos)) {
                return false;
            }
        }
        if (familyOf == null) initFamilies();
        Block current = level.getBlockState(pos).getBlock();
        Integer family = familyOf.get(current);
        if (family == null) return false;
        Block painted = byFamily[family][color.getId()];
        if (painted == null || painted == current) return false;
        level.setBlock(pos, painted.defaultBlockState(), 2);
        return true;
    }

    private static void initFamilies() {
        Map<Block, Integer> map = new HashMap<>();
        Block[][] table = new Block[FAMILIES.length][DyeColor.values().length];
        for (int f = 0; f < FAMILIES.length; f++) {
            for (DyeColor c : DyeColor.values()) {
                Block b = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("minecraft", c.getName() + FAMILIES[f]));
                if (b != null && b != Blocks.AIR) {
                    table[f][c.getId()] = b;
                    map.put(b, f);
                }
            }
        }
        byFamily = table;
        familyOf = map;
    }

    public static Vector3f rgb(DyeColor color) {
        float[] c = color.getTextureDiffuseColors();
        return new Vector3f(c[0], c[1], c[2]);
    }

    public static void burst(ServerLevel level, Vec3 p, DyeColor color, int count, double spread) {
        level.sendParticles(new DustParticleOptions(rgb(color), 1.4F), p.x, p.y, p.z, count, spread, spread, spread, 0.0);
    }

    private Paint() {}
}
