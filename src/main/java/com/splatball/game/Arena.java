package com.splatball.game;

import com.splatball.Splatball;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Generates the paintball arena: an 81 x 145 enclosed hall, red base at the low-Z end and blue base at
 * the high-Z end. Cover is laid out in one quadrant and mirrored four ways so both teams get the same map.
 * Everything white is paintable, so the map fills with colour as the match goes on.
 */
public final class Arena {
    public static final int W = 81;   // x: 0..80
    public static final int L = 145;  // z: 0..144
    public static final int H = 13;   // floor at y 0, glass roof at y 13

    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final BlockState CONCRETE = Blocks.WHITE_CONCRETE.defaultBlockState();
    private static final BlockState WOOL = Blocks.WHITE_WOOL.defaultBlockState();
    private static final BlockState TERRACOTTA = Blocks.WHITE_TERRACOTTA.defaultBlockState();
    private static final BlockState GLASS = Blocks.WHITE_STAINED_GLASS.defaultBlockState();
    private static final BlockState SLAB = Blocks.SMOOTH_QUARTZ_SLAB.defaultBlockState();
    private static final BlockState LIGHT = Blocks.SEA_LANTERN.defaultBlockState();
    private static final BlockState RED_FLOOR = Blocks.RED_GLAZED_TERRACOTTA.defaultBlockState();
    private static final BlockState BLUE_FLOOR = Blocks.BLUE_GLAZED_TERRACOTTA.defaultBlockState();

    private final ServerLevel level;
    private final BlockPos origin;
    private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

    private Arena(ServerLevel level, BlockPos origin) {
        this.level = level;
        this.origin = origin;
    }

    /** Builds (or rebuilds, wiping all paint) the arena with its minimum corner at origin. */
    public static void build(ServerLevel level, BlockPos origin) {
        new Arena(level, origin).generate();
        // no stray animals, dropped items or old paintballs on the field
        AABB box = new AABB(origin, origin.offset(W, H + 1, L));
        for (Entity entity : level.getEntitiesOfClass(Entity.class, box, e -> !(e instanceof Player))) entity.discard();
        Splatball.LOGGER.info("Built paintball arena at {} in {}", origin, level.dimension().location());
    }

    public static boolean contains(BlockPos origin, BlockPos pos) {
        int x = pos.getX() - origin.getX(), y = pos.getY() - origin.getY(), z = pos.getZ() - origin.getZ();
        return x >= 0 && x < W && y >= 0 && y <= H && z >= 0 && z < L;
    }

    /** A random spot on a team's spawn strip. */
    public static Vec3 spawn(BlockPos origin, boolean red, RandomSource random) {
        double x = origin.getX() + W / 2 + 0.5 + (random.nextInt(33) - 16);
        double z = origin.getZ() + (red ? 3.5 : L - 3.5);
        return new Vec3(x, origin.getY() + 1, z);
    }

    private void generate() {
        // shell: floor, walls, roof, and clear the inside
        for (int x = 0; x < W; x++) {
            for (int z = 0; z < L; z++) {
                boolean wall = x == 0 || x == W - 1 || z == 0 || z == L - 1;
                BlockState floor = x % 5 == 0 && z % 6 == 0 ? LIGHT : z <= 6 ? RED_FLOOR : z >= L - 7 ? BLUE_FLOOR : CONCRETE;
                raw(x, 0, z, floor);
                for (int y = 1; y < H; y++) raw(x, y, z, wall ? CONCRETE : AIR);
                raw(x, H, z, GLASS);
            }
        }
        // a station against each back wall: click it to leave the match
        BlockState station = Splatball.STATION.get().defaultBlockState();
        raw(W / 2, 1, 1, station);
        raw(W / 2, 1, L - 2, station);

        // --- one quadrant (x 1..40, z 1..72), mirrored to the other three ---

        // base wall with two side doors and a centre door
        for (int x = 1; x <= 37; x++) {
            boolean door = (x >= 6 && x <= 8) || (x >= 22 && x <= 24);
            if (!door) fill(x, 9, x, 9, 1, 3, CONCRETE);
        }
        // first line of cover outside the base
        fill(12, 15, 14, 15, 1, 2, WOOL);
        fill(30, 15, 32, 15, 1, 2, WOOL);

        // tall pillars and lane walls that break up the long sightlines
        fill(18, 21, 19, 22, 1, 5, CONCRETE);
        fill(34, 27, 35, 28, 1, 5, CONCRETE);
        fill(20, 26, 20, 32, 1, 3, CONCRETE);
        fill(28, 52, 28, 56, 1, 3, CONCRETE);

        // towers: two against the side wall, one free-standing
        tower(1, 6, 20, 26, 1, 2);
        tower(14, 19, 44, 49, 16, 17);
        tower(1, 6, 62, 68, 1, 2);

        // bunkers
        fill(10, 28, 12, 28, 1, 2, WOOL);
        fill(10, 29, 10, 30, 1, 2, WOOL);
        fill(26, 22, 28, 22, 1, 2, WOOL);
        fill(28, 23, 28, 24, 1, 2, WOOL);
        fill(30, 34, 31, 34, 1, 2, WOOL);
        fill(4, 44, 5, 44, 1, 2, WOOL);
        fill(24, 50, 25, 50, 1, 2, WOOL);
        fill(36, 46, 37, 46, 1, 2, WOOL);
        fill(8, 56, 9, 56, 1, 2, WOOL);
        fill(20, 58, 21, 58, 1, 2, WOOL);
        fill(32, 60, 33, 60, 1, 2, WOOL);

        // low walls to crouch behind
        fill(6, 34, 16, 34, 1, 1, WOOL);
        fill(24, 40, 34, 40, 1, 1, WOOL);

        // centre platform with a tunnel underneath and a ramp from each base
        ramp(39, 40, 59, 6);
        fill(33, 65, 40, 72, 3, 3, TERRACOTTA);
        fill(33, 65, 33, 65, 1, 2, TERRACOTTA);
        fill(33, 65, 33, 65, 4, 4, TERRACOTTA);
        fill(36, 68, 36, 68, 4, 4, TERRACOTTA);
        fill(40, 72, 40, 72, 4, 5, TERRACOTTA);

        // midline walls
        fill(1, 72, 6, 72, 1, 3, CONCRETE);
        fill(12, 72, 16, 72, 1, 2, WOOL);
        fill(22, 72, 26, 72, 1, 2, WOOL);
    }

    /** A deck three blocks up on corner posts, with battlements and a ramp leading up to its low-z edge. */
    private void tower(int x1, int x2, int z1, int z2, int rampX1, int rampX2) {
        ramp(rampX1, rampX2, z1 - 6, 6);
        fill(x1, z1, x2, z2, 3, 3, TERRACOTTA);
        for (int x : new int[]{x1, x2}) {
            for (int z : new int[]{z1, z2}) fill(x, z, x, z, 1, 2, TERRACOTTA);
        }
        for (int z = z1; z <= z2; z += 2) {
            fill(x2, z, x2, z, 4, 4, TERRACOTTA);
            if (x1 > 1) fill(x1, z, x1, z, 4, 4, TERRACOTTA);
        }
    }

    /** A walkable half-slab ramp rising toward +z, six steps climb three blocks. */
    private void ramp(int x1, int x2, int z0, int steps) {
        for (int i = 0; i < steps; i++) {
            int halves = i + 1;
            int full = halves / 2;
            if (full > 0) fill(x1, z0 + i, x2, z0 + i, 1, full, TERRACOTTA);
            if (halves % 2 == 1) fill(x1, z0 + i, x2, z0 + i, full + 1, full + 1, SLAB);
        }
    }

    /** Fills a box in the quadrant and its three mirror images. */
    private void fill(int x1, int z1, int x2, int z2, int y1, int y2, BlockState state) {
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                for (int y = y1; y <= y2; y++) {
                    raw(x, y, z, state);
                    raw(W - 1 - x, y, z, state);
                    raw(x, y, L - 1 - z, state);
                    raw(W - 1 - x, y, L - 1 - z, state);
                }
            }
        }
    }

    private void raw(int x, int y, int z, BlockState state) {
        cursor.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
        level.setBlock(cursor, state, 2);
    }
}
