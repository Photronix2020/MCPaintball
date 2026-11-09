package com.splatball.game;

import com.splatball.Config;
import com.splatball.Splatball;
import com.splatball.entity.BotEntity;
import com.splatball.item.MarkerItem;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Red vs blue team deathmatch, one per server. Players drop in (and out) at any time through a
 * Paintball Station; the match starts with the first player and ends on the splat target, the clock,
 * or when the last player leaves.
 */
public final class Match {
    public static final String RED = "splat_red";
    public static final String BLUE = "splat_blue";
    private static final String KIT_TAG = "SplatballKit";
    /** How far above a station its arena is built. */
    private static final int SKY_OFFSET = 40;

    /** What to give a player back when they leave: game mode and where they came from. */
    private record Entry(GameType mode, ResourceKey<Level> dimension, Vec3 pos, float yRot) {}

    private static boolean active;
    private static int redScore, blueScore, target, ticksLeft, totalTicks;
    private static final Map<UUID, Entry> players = new HashMap<>();
    /** Computer players filling out the teams. */
    private static final List<BotEntity> bots = new ArrayList<>();
    private static int botSerial;
    @Nullable
    private static ServerBossEvent bar;

    public static boolean isActive() {
        return active;
    }

    public static boolean isPlaying(Player player) {
        return active && players.containsKey(player.getUUID());
    }

    public static boolean isRed(Entity entity) {
        Team team = entity.getTeam();
        return team != null && team.getName().equals(RED);
    }

    public static boolean ownsBot(BotEntity bot) {
        return active && bots.contains(bot);
    }

    public static List<BotEntity> bots() {
        return Collections.unmodifiableList(bots);
    }

    public static int score(boolean red) {
        return red ? redScore : blueScore;
    }

    /** In the running match, as a player or a bot. */
    private static boolean isParticipant(Entity entity) {
        return (entity instanceof Player player && isPlaying(player)) || (entity instanceof BotEntity bot && ownsBot(bot));
    }

    public static boolean isEnemy(Entity self, Entity other) {
        return other.isAlive() && isParticipant(self) && isParticipant(other) && isRed(self) != isRed(other);
    }

    /** A short leg toward the enemy base for a bot with nothing to shoot at. */
    @Nullable
    public static Vec3 waypointFor(BotEntity bot) {
        if (!(bot.level() instanceof ServerLevel level)) return null;
        ArenaData data = ArenaData.get(level.getServer());
        if (data.origin == null) return null;
        RandomSource random = bot.getRandom();
        int forward = isRed(bot) ? 1 : -1;
        if (random.nextInt(4) == 0) forward = -forward;
        int x = Mth.clamp(bot.getBlockX() - data.origin.getX() + random.nextInt(25) - 12, 2, Arena.W - 3);
        int z = Mth.clamp(bot.getBlockZ() - data.origin.getZ() + forward * (6 + random.nextInt(16)), 3, Arena.L - 4);
        return new Vec3(data.origin.getX() + x + 0.5, data.origin.getY() + 1, data.origin.getZ() + z + 0.5);
    }

    /** Keeps each team topped up with bots to the configured size, removing bots as humans join. */
    private static void balanceBots(MinecraftServer server) {
        ServerLevel level = arenaLevel(server);
        if (!active || level == null) return;
        bots.removeIf(bot -> bot.isRemoved());
        Scoreboard board = server.getScoreboard();
        for (boolean red : new boolean[]{true, false}) {
            int humans = 0;
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (isPlaying(player) && isRed(player) == red) humans++;
            }
            int wanted = Math.max(0, Config.BOT_FILL.get() - humans);
            List<BotEntity> mine = new ArrayList<>();
            for (BotEntity bot : bots) if (isRed(bot) == red) mine.add(bot);
            while (mine.size() > wanted) {
                BotEntity extra = mine.remove(mine.size() - 1);
                bots.remove(extra);
                board.removePlayerFromTeam(extra.getScoreboardName());
                extra.discard();
            }
            while (mine.size() < wanted) {
                BotEntity bot = spawnBot(server, level, red);
                if (bot == null) break;
                mine.add(bot);
            }
        }
    }

    @Nullable
    private static BotEntity spawnBot(MinecraftServer server, ServerLevel level, boolean red) {
        BotEntity bot = Splatball.BOT.get().create(level);
        if (bot == null) return null;
        Scoreboard board = server.getScoreboard();
        PlayerTeam team = red ? team(board, RED, "Red", ChatFormatting.RED) : team(board, BLUE, "Blue", ChatFormatting.BLUE);
        board.addPlayerToTeam(bot.getScoreboardName(), team);
        bot.setCustomName(Component.literal("Bot " + (++botSerial)));
        bot.setCustomNameVisible(true);
        // bots carry one of the straight-shooting markers
        List<RegistryObject<MarkerItem>> choices = List.of(Splatball.PISTOL, Splatball.SMG, Splatball.SHOTGUN);
        bot.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(choices.get(bot.getRandom().nextInt(choices.size())).get()));
        bot.setItemSlot(EquipmentSlot.HEAD, armorPiece(EquipmentSlot.HEAD, red));
        bot.setItemSlot(EquipmentSlot.CHEST, armorPiece(EquipmentSlot.CHEST, red));
        for (EquipmentSlot slot : EquipmentSlot.values()) bot.setDropChance(slot, 0.0F);
        bots.add(bot);
        placeBot(server, bot);
        level.addFreshEntity(bot);
        return bot;
    }

    /** Puts a bot on its team's spawn strip at full health with spawn protection. */
    private static void placeBot(MinecraftServer server, BotEntity bot) {
        ArenaData data = ArenaData.get(server);
        if (data.origin == null) return;
        boolean red = isRed(bot);
        Vec3 spot = Arena.spawn(data.origin, red, bot.getRandom());
        bot.moveTo(spot.x, spot.y, spot.z, red ? 0.0F : 180.0F, 0.0F);
        bot.setDeltaMovement(Vec3.ZERO);
        bot.fallDistance = 0;
        bot.setHealth(bot.getMaxHealth());
        bot.clearFire();
        bot.removeAllEffects();
        bot.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 4, false, false));
        bot.setTarget(null);
        bot.getNavigation().stop();
    }

    private static boolean onATeam(Player player) {
        Team team = player.getTeam();
        return team != null && (team.getName().equals(RED) || team.getName().equals(BLUE));
    }

    @Nullable
    private static ServerLevel arenaLevel(MinecraftServer server) {
        ArenaData data = ArenaData.get(server);
        return data.origin == null ? null : server.getLevel(data.dimension);
    }

    /**
     * A player right-clicked a Paintball Station. Playing: leave. Otherwise: build the arena in the sky
     * above the station if there is none yet, start a match if none is running, and drop the player in.
     */
    public static void useStation(ServerLevel level, BlockPos station, ServerPlayer player) {
        MinecraftServer server = level.getServer();
        if (isPlaying(player)) {
            leave(server, player);
            player.displayClientMessage(Component.literal("You left the paintball match."), true);
            return;
        }
        if (arenaLevel(server) == null) {
            int y = Math.min(station.getY() + SKY_OFFSET, level.getMaxBuildHeight() - Arena.H - 2);
            BlockPos origin = new BlockPos(station.getX() - Arena.W / 2, y, station.getZ() - Arena.L / 2);
            if (y <= station.getY() + 3) {
                player.displayClientMessage(Component.literal("Not enough sky above this station for an arena."), true);
                return;
            }
            Arena.build(level, origin);
            ArenaData.get(server).set(level.dimension(), origin, station);
        }
        if (!active) begin(server, Config.KILLS_TO_WIN.get());
        add(server, player, onATeam(player) ? isRed(player) : null);
        balanceBots(server);
    }

    /** Starts a match with everyone online. Returns an error message, or null on success. */
    @Nullable
    public static String start(MinecraftServer server, int killsToWin) {
        if (active) return "A match is already running. Use /paintball stop first.";
        if (arenaLevel(server) == null) return "No arena yet. Place a Paintball Station and right-click it.";
        List<ServerPlayer> list = new ArrayList<>(server.getPlayerList().getPlayers());
        list.removeIf(ServerPlayer::isSpectator);
        if (list.isEmpty()) return "There are no players to start a match with.";

        begin(server, killsToWin);
        Collections.shuffle(list);
        // players who picked a side with /paintball join keep it; the rest fill the smaller team
        list.sort((a, b) -> Boolean.compare(onATeam(b), onATeam(a)));
        for (ServerPlayer player : list) add(server, player, onATeam(player) ? isRed(player) : null);
        balanceBots(server);
        return null;
    }

    /** Fresh arena, fresh scores, clock running. The arena must exist. */
    private static void begin(MinecraftServer server, int killsToWin) {
        ArenaData data = ArenaData.get(server);
        Arena.build(server.getLevel(data.dimension), data.origin);
        active = true;
        redScore = blueScore = 0;
        target = killsToWin;
        totalTicks = ticksLeft = Config.MATCH_MINUTES.get() * 60 * 20;
        bar = new ServerBossEvent(Component.empty(), BossEvent.BossBarColor.WHITE, BossEvent.BossBarOverlay.PROGRESS);
        updateBar();
        server.getPlayerList().broadcastSystemMessage(Component.literal("A paintball match has started! First team to "
                + target + " splats wins. Right-click a Paintball Station to drop in.").withStyle(ChatFormatting.GOLD), false);
    }

    /** Puts a player on a side (or the smaller side when red is null). Mid-match this also drops them in. */
    public static void join(MinecraftServer server, ServerPlayer player, @Nullable Boolean red) {
        if (active) {
            add(server, player, red);
            balanceBots(server);
        } else {
            assign(server, player, red);
        }
    }

    public static void leave(MinecraftServer server, ServerPlayer player) {
        release(server, player);
        players.remove(player.getUUID());
        if (onATeam(player)) server.getScoreboard().removePlayerFromTeam(player.getScoreboardName());
        if (active && players.isEmpty()) stop(server);
        else balanceBots(server);
    }

    private static boolean assign(MinecraftServer server, ServerPlayer player, @Nullable Boolean red) {
        Scoreboard board = server.getScoreboard();
        PlayerTeam redTeam = team(board, RED, "Red", ChatFormatting.RED);
        PlayerTeam blueTeam = team(board, BLUE, "Blue", ChatFormatting.BLUE);
        // balance on humans only; bots are shuffled around them afterwards
        int reds = 0, blues = 0;
        for (ServerPlayer other : server.getPlayerList().getPlayers()) {
            if (other == player || !onATeam(other)) continue;
            if (isRed(other)) reds++;
            else blues++;
        }
        boolean toRed = red != null ? red : reds <= blues;
        board.addPlayerToTeam(player.getScoreboardName(), toRed ? redTeam : blueTeam);
        return toRed;
    }

    private static PlayerTeam team(Scoreboard board, String name, String display, ChatFormatting color) {
        PlayerTeam team = board.getPlayerTeam(name);
        if (team == null) team = board.addPlayerTeam(name);
        team.setDisplayName(Component.literal(display));
        team.setColor(color);
        team.setAllowFriendlyFire(false);
        return team;
    }

    private static void add(MinecraftServer server, ServerPlayer player, @Nullable Boolean red) {
        boolean toRed = assign(server, player, red);
        if (!players.containsKey(player.getUUID())) players.put(player.getUUID(), entryFor(server, player));
        player.setGameMode(GameType.ADVENTURE);
        giveKit(player);
        giveArmor(player, toRed);
        if (bar != null) bar.addPlayer(player);
        respawn(server, player);
        title(player, Component.literal(toRed ? "RED TEAM" : "BLUE TEAM").withStyle(toRed ? ChatFormatting.RED : ChatFormatting.BLUE),
                Component.literal("Hold right-click to fire, R to reload"));
        player.displayClientMessage(Component.literal("Shoot the other team: hits take health, zero health is a splat. "
                + "The station at the back of your base takes you out."), false);
    }

    /** Where a player goes when they leave. Someone joining from inside the arena must not be sent back into it. */
    private static Entry entryFor(MinecraftServer server, ServerPlayer player) {
        GameType mode = player.gameMode.getGameModeForPlayer();
        ArenaData data = ArenaData.get(server);
        boolean inside = data.origin != null && player.level().dimension() == data.dimension
                && Arena.contains(data.origin, player.blockPosition());
        if (!inside) return new Entry(mode, player.level().dimension(), player.position(), player.getYRot());
        if (data.home != null) return new Entry(mode, data.dimension, Vec3.atBottomCenterOf(data.home.above()), player.getYRot());
        return new Entry(mode, Level.OVERWORLD, Vec3.atBottomCenterOf(server.overworld().getSharedSpawnPos()), player.getYRot());
    }

    public static void giveKit(ServerPlayer player) {
        for (RegistryObject<MarkerItem> marker : Splatball.MARKERS) {
            Item item = marker.get();
            boolean has = player.getInventory().items.stream().anyMatch(s -> s.is(item));
            if (!has) player.getInventory().add(new ItemStack(item));
        }
    }

    private static void giveArmor(ServerPlayer player, boolean red) {
        int rgb = red ? 0xD03030 : 0x3050D0;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST}) {
            ItemStack worn = player.getItemBySlot(slot);
            if (!worn.isEmpty() && !isKit(worn)) continue;   // never replace the player's own armour
            player.setItemSlot(slot, armorPiece(slot, red));
        }
    }

    private static ItemStack armorPiece(EquipmentSlot slot, boolean red) {
        ItemStack piece = new ItemStack(slot == EquipmentSlot.HEAD ? Items.LEATHER_HELMET : Items.LEATHER_CHESTPLATE);
        piece.getOrCreateTagElement("display").putInt("color", red ? 0xD03030 : 0x3050D0);
        piece.getOrCreateTag().putBoolean(KIT_TAG, true);
        piece.getOrCreateTag().putBoolean("Unbreakable", true);
        return piece;
    }

    private static boolean isKit(ItemStack stack) {
        return stack.hasTag() && stack.getTag().getBoolean(KIT_TAG);
    }

    private static void respawn(MinecraftServer server, ServerPlayer player) {
        ArenaData data = ArenaData.get(server);
        ServerLevel level = arenaLevel(server);
        if (level == null) return;
        boolean red = isRed(player);
        Vec3 spot = Arena.spawn(data.origin, red, player.getRandom());
        player.teleportTo(level, spot.x, spot.y, spot.z, red ? 0.0F : 180.0F, 0.0F);
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0;
        player.setHealth(player.getMaxHealth());
        player.getFoodData().setFoodLevel(20);
        player.clearFire();
        player.removeAllEffects();
        // three seconds of spawn protection
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 4, false, false));
        for (ItemStack stack : player.getInventory().items) MarkerItem.refill(stack);
    }

    /** A participant "died": cancel the real death, score it, and put them back at their base. */
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        MinecraftServer server = victim.getServer();
        if (server == null || !isParticipant(victim)) return;
        event.setCanceled(true);
        Entity killer = event.getSource().getEntity();
        Component subtitle = Component.literal("Back to base");
        if (killer != null && killer != victim && isParticipant(killer) && isRed(killer) != isRed(victim)) {
            if (isRed(killer)) redScore++;
            else blueScore++;
            server.getPlayerList().broadcastSystemMessage(Component.empty()
                    .append(killer.getDisplayName()).append(Component.literal(" splatted ").withStyle(ChatFormatting.GRAY))
                    .append(victim.getDisplayName()), false);
            if (killer instanceof ServerPlayer shooter) {
                shooter.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0F, 1.0F);
            }
            subtitle = Component.literal("by ").append(killer.getDisplayName());
        }
        if (victim instanceof ServerPlayer player) {
            title(player, Component.literal("SPLATTED!").withStyle(ChatFormatting.YELLOW), subtitle);
            respawn(server, player);
        } else if (victim instanceof BotEntity bot) {
            placeBot(server, bot);
        }
        updateBar();
        if (redScore >= target || blueScore >= target) finish(server);
    }

    public static void tick(MinecraftServer server) {
        if (!active) return;
        ticksLeft--;
        if (ticksLeft % 20 == 0) {
            updateBar();
            balanceBots(server);   // also replaces any bot that got removed
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (isPlaying(player)) player.getFoodData().setFoodLevel(20);
            }
        }
        if (ticksLeft <= 0) finish(server);
    }

    private static void finish(MinecraftServer server) {
        MutableComponent result = redScore == blueScore
                ? Component.literal("DRAW").withStyle(ChatFormatting.YELLOW)
                : redScore > blueScore
                ? Component.literal("RED WINS").withStyle(ChatFormatting.RED)
                : Component.literal("BLUE WINS").withStyle(ChatFormatting.BLUE);
        Component score = Component.literal(redScore + " - " + blueScore);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!isPlaying(player)) continue;
            title(player, result, score);
            player.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        server.getPlayerList().broadcastSystemMessage(Component.literal("Paintball: ").append(result)
                .append(Component.literal("  " + redScore + " - " + blueScore).withStyle(ChatFormatting.WHITE)), false);
        stop(server);
    }

    /** Ends the match and sends everyone back where they came from. Safe to call when no match is running. */
    public static void stop(MinecraftServer server) {
        active = false;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) release(server, player);
        players.clear();
        for (BotEntity bot : bots) bot.discard();
        bots.clear();
        if (bar != null) bar.removeAllPlayers();
        bar = null;
        Scoreboard board = server.getScoreboard();
        for (String name : new String[]{RED, BLUE}) {
            PlayerTeam team = board.getPlayerTeam(name);
            if (team != null) board.removePlayerTeam(team);
        }
    }

    /** Undo what joining did to one player (game mode, kit armour, boss bar, position). Keeps them in the map. */
    private static void release(MinecraftServer server, ServerPlayer player) {
        Entry entry = players.get(player.getUUID());
        if (entry == null) return;
        player.setGameMode(entry.mode());
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST}) {
            if (isKit(player.getItemBySlot(slot))) player.setItemSlot(slot, ItemStack.EMPTY);
        }
        if (bar != null) bar.removePlayer(player);
        player.removeAllEffects();
        player.setHealth(player.getMaxHealth());
        ServerLevel back = server.getLevel(entry.dimension());
        if (back != null && !(player.hasDisconnected() && back != player.level())) {
            player.teleportTo(back, entry.pos().x, entry.pos().y, entry.pos().z, entry.yRot(), 0.0F);
            player.fallDistance = 0;
        }
    }

    private static void updateBar() {
        if (bar == null) return;
        int seconds = Math.max(ticksLeft, 0) / 20;
        bar.setName(Component.empty()
                .append(Component.literal("RED " + redScore).withStyle(ChatFormatting.RED))
                .append(Component.literal("  |  " + String.format("%d:%02d", seconds / 60, seconds % 60) + "  |  ").withStyle(ChatFormatting.WHITE))
                .append(Component.literal(blueScore + " BLUE").withStyle(ChatFormatting.BLUE))
                .append(Component.literal("   first to " + target).withStyle(ChatFormatting.GRAY)));
        bar.setProgress(totalTicks <= 0 ? 0 : Math.max(0.0F, (float) ticksLeft / totalTicks));
    }

    private static void title(ServerPlayer player, Component title, Component subtitle) {
        player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
        player.connection.send(new ClientboundSetTitleTextPacket(title));
    }

    private Match() {}
}
