package com.splatball.test;

import com.splatball.Paint;
import com.splatball.Splatball;
import com.splatball.entity.Paintball;
import com.splatball.game.Arena;
import com.splatball.game.ArenaData;
import com.splatball.game.Match;
import com.splatball.item.MarkerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Dev-only end-to-end test (excluded from the release jar). Run with: gradlew runGameTestServer
 * Builds an arena, fires a marker at the floor, then plays a one-splat match between two fake players.
 */
@GameTestHolder(Splatball.MODID)
@PrefixGameTestTemplate(false)
public class SplatballTests {
    @GameTest(template = "empty", timeoutTicks = 400)
    public static void arenaMarkerAndMatch(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        server.setPvpAllowed(true);

        BlockPos o = new BlockPos(2000, 80, 2000);
        Arena.build(level, o);
        ArenaData.get(server).set(level.dimension(), o, null);
        check(level.getBlockState(o.offset(41, 0, 72)).is(Blocks.WHITE_CONCRETE), "floor is white concrete");
        check(level.getBlockState(o.offset(40, 0, 72)).is(Blocks.SEA_LANTERN), "floor lights");
        check(level.getBlockState(o.offset(41, 0, 3)).is(Blocks.RED_GLAZED_TERRACOTTA), "red spawn floor");
        check(level.getBlockState(o.offset(41, 0, Arena.L - 4)).is(Blocks.BLUE_GLAZED_TERRACOTTA), "blue spawn floor");
        check(level.getBlockState(o.offset(0, 5, 30)).is(Blocks.WHITE_CONCRETE), "outer wall");
        check(level.getBlockState(o.offset(12, Arena.H, 30)).is(Blocks.WHITE_STAINED_GLASS), "glass roof");
        check(level.getBlockState(o.offset(33, 3, 65)).is(Blocks.WHITE_TERRACOTTA), "centre platform");
        check(level.getBlockState(o.offset(47, 3, 79)).is(Blocks.WHITE_TERRACOTTA), "centre platform is mirrored");
        check(level.getBlockState(o.offset(40, 1, 9)).isAir(), "centre door is open");
        check(level.getBlockState(o.offset(1, 1, 14)).is(Blocks.SMOOTH_QUARTZ_SLAB), "tower ramp starts with a slab");
        check(level.getBlockState(o.offset(Arena.W - 2, 1, Arena.L - 1 - 14)).is(Blocks.SMOOTH_QUARTZ_SLAB), "tower ramp is mirrored");
        check(!Paint.paintBlock(level, o.offset(-3, 0, 5), net.minecraft.world.item.DyeColor.RED), "no painting outside the arena");

        // keep the firing spot ticking even though only fake players are nearby
        BlockPos spot = o.offset(10, 1, 40);
        ChunkPos chunk = new ChunkPos(spot);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) level.setChunkForced(chunk.x + dx, chunk.z + dz, true);
        }

        ServerPlayer a = fakePlayer(level, "splat_a");
        ServerPlayer b = fakePlayer(level, "splat_b");
        a.setGameMode(GameType.SURVIVAL);
        b.setGameMode(GameType.SURVIVAL);
        a.teleportTo(level, spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, 0.0F, 90.0F);   // looking straight down

        MarkerItem pistol = Splatball.PISTOL.get();
        ItemStack stack = new ItemStack(pistol);
        a.setItemInHand(InteractionHand.MAIN_HAND, stack);
        pistol.tryFire(a, stack);
        check(MarkerItem.getAmmo(stack) == pistol.stats().magazine() - 1, "firing uses one round");
        pistol.tryFire(a, stack);
        check(MarkerItem.getAmmo(stack) == pistol.stats().magazine() - 1, "cooldown blocks an instant second shot");

        helper.runAfterDelay(40, () -> {
            check(!level.getBlockState(spot.below()).is(Blocks.WHITE_CONCRETE), "paintball painted the floor, found "
                    + level.getBlockState(spot.below()));

            String error = Match.start(server, 1);
            check(error == null, "match starts: " + error);
            check(level.getBlockState(spot.below()).is(Blocks.WHITE_CONCRETE), "starting a match washes the arena");
            check(a.gameMode.getGameModeForPlayer() == GameType.ADVENTURE, "players are put in adventure mode");
            check(Match.isRed(a) != Match.isRed(b), "two players land on opposite teams");
            check(Match.bots().size() == 6, "bots fill both teams to four, found " + Match.bots().size());
            check(a.getInventory().items.stream().filter(s -> s.getItem() instanceof MarkerItem).count() == 5, "kit has all five markers");
            check(Arena.contains(o, b.blockPosition()), "players are moved into the arena");

            // spawn protection holds, then a real hit ends a first-to-1 match
            b.hurt(Paint.damageSource(level, a, a), 1000.0F);
            check(Match.isActive() && b.getHealth() == b.getMaxHealth(), "spawn protection absorbs the hit");
            for (int i = 0; i < 80; i++) b.tick();   // run out the fake player's join invulnerability
            b.removeAllEffects();
            check(b.hurt(Paint.damageSource(level, a, a), 1000.0F), "an unprotected enemy can be hit");
            check(!b.isDeadOrDying() && b.getHealth() == b.getMaxHealth(), "a splat respawns instead of killing");
            check(!Match.isActive(), "reaching the target ends the match");
            check(a.gameMode.getGameModeForPlayer() == GameType.SURVIVAL, "game mode is restored afterwards");
            check(a.getTeam() == null, "teams are cleaned up afterwards");
            check(!Arena.contains(o, a.blockPosition()) && !Arena.contains(o, b.blockPosition()), "players are sent back out of the arena");
            check(level.getBlockState(o.offset(Arena.W / 2, 1, 1)).is(Splatball.STATION.get()), "exit station in the red base");

            // station flow: one click drops a lone player into a fresh match, a second click takes them out
            BlockPos station = new BlockPos(2300, 80, 2300);
            a.teleportTo(level, 2300.5, 81, 2300.5, 0.0F, 0.0F);
            Match.useStation(level, station, a);
            check(Match.isActive() && Match.isPlaying(a), "station starts a match and drops the player in");
            check(Arena.contains(o, a.blockPosition()), "station moves the player into the arena");
            check(a.getInventory().items.stream().filter(s -> s.getItem() instanceof MarkerItem).count() == 5, "station gives the kit once");
            check(Match.bots().size() == 7, "a lone player gets three bot team-mates and four bot enemies, found " + Match.bots().size());
            com.splatball.entity.BotEntity enemy = Match.bots().stream().filter(x -> Match.isRed(x) != Match.isRed(a)).findFirst().orElseThrow();
            com.splatball.entity.BotEntity friend = Match.bots().stream().filter(x -> Match.isRed(x) == Match.isRed(a)).findFirst().orElseThrow();
            check(Match.isEnemy(enemy, a) && !Match.isEnemy(friend, a), "bots know friend from foe");
            enemy.removeAllEffects();
            check(enemy.hurt(Paint.damageSource(level, a, a), 1000.0F), "an enemy bot can be hit");
            check(enemy.isAlive() && enemy.getHealth() == enemy.getMaxHealth(), "a splatted bot respawns");
            check(Match.score(Match.isRed(a)) == 1 && Match.isActive(), "splatting a bot scores a point");
            for (int i = 0; i < 80; i++) a.tick();
            a.removeAllEffects();
            a.hurt(Paint.damageSource(level, enemy, enemy), 1000.0F);
            check(Match.score(Match.isRed(enemy)) == 1 && a.getHealth() == a.getMaxHealth(), "a bot splatting a player scores for its team");
            enemy.performRangedAttack(a, 1.0F);
            Match.useStation(level, o.offset(Arena.W / 2, 1, 1), a);
            check(!Match.isActive(), "leaving as the last player ends the match");
            check(Match.bots().isEmpty() && enemy.isRemoved(), "bots are removed when the match ends");
            check(a.blockPosition().distManhattan(station) <= 2, "leaving returns the player to the station, at " + a.blockPosition());

            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) level.setChunkForced(chunk.x + dx, chunk.z + dz, false);
            }
            helper.succeed();
        });
    }

    /** A logged-in player backed by an in-memory connection. */
    private static ServerPlayer fakePlayer(ServerLevel level, String name) {
        ServerPlayer player = new ServerPlayer(level.getServer(), level, new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), name));
        net.minecraft.network.Connection connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        level.getServer().getPlayerList().placeNewPlayer(connection, player);
        return player;
    }

    private static void check(boolean condition, String what) {
        if (!condition) throw new GameTestAssertException("Expected: " + what);
    }
}
