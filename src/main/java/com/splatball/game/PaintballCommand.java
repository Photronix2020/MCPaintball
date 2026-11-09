package com.splatball.game;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.splatball.Config;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public final class PaintballCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("paintball")
                .then(Commands.literal("arena")
                        .then(Commands.literal("build").requires(s -> s.hasPermission(2))
                                .executes(ctx -> warn(ctx.getSource()))
                                .then(Commands.literal("confirm").executes(ctx -> build(ctx.getSource()))))
                        .then(Commands.literal("reset").requires(s -> s.hasPermission(2))
                                .executes(ctx -> reset(ctx.getSource())))
                        .then(Commands.literal("tp").executes(ctx -> tp(ctx.getSource()))))
                .then(Commands.literal("start").requires(s -> s.hasPermission(2))
                        .executes(ctx -> start(ctx.getSource(), Config.KILLS_TO_WIN.get()))
                        .then(Commands.argument("splats", IntegerArgumentType.integer(1, 500))
                                .executes(ctx -> start(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "splats")))))
                .then(Commands.literal("stop").requires(s -> s.hasPermission(2))
                        .executes(ctx -> stop(ctx.getSource())))
                .then(Commands.literal("join")
                        .executes(ctx -> join(ctx.getSource(), null))
                        .then(Commands.literal("red").executes(ctx -> join(ctx.getSource(), true)))
                        .then(Commands.literal("blue").executes(ctx -> join(ctx.getSource(), false))))
                .then(Commands.literal("leave").executes(ctx -> leave(ctx.getSource())))
                .then(Commands.literal("kit").requires(s -> s.hasPermission(2))
                        .executes(ctx -> kit(ctx.getSource()))));
    }

    private static BlockPos originFor(CommandSourceStack src) {
        return BlockPos.containing(src.getPosition()).offset(-Arena.W / 2, -1, -Arena.L / 2);
    }

    private static int warn(CommandSourceStack src) {
        BlockPos o = originFor(src);
        src.sendSuccess(() -> Component.literal("This replaces every block in a " + Arena.W + " x " + (Arena.H + 1) + " x "
                + Arena.L + " box centred on you, from " + o.toShortString() + " to "
                + o.offset(Arena.W - 1, Arena.H, Arena.L - 1).toShortString()
                + ". Run /paintball arena build confirm to go ahead.").withStyle(ChatFormatting.YELLOW), false);
        return 1;
    }

    private static int build(CommandSourceStack src) {
        if (Match.isActive()) return fail(src, "Stop the running match first.");
        ServerLevel level = src.getLevel();
        BlockPos origin = originFor(src);
        if (origin.getY() < level.getMinBuildHeight() || origin.getY() + Arena.H >= level.getMaxBuildHeight()) {
            return fail(src, "Not enough vertical room here for the arena.");
        }
        Arena.build(level, origin);
        ArenaData.get(src.getServer()).set(level.dimension(), origin, null);
        if (src.getEntity() instanceof ServerPlayer player) toArena(player, level, origin);
        src.sendSuccess(() -> Component.literal("Arena built. Run /paintball start to play.").withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int reset(CommandSourceStack src) {
        ArenaData data = ArenaData.get(src.getServer());
        ServerLevel level = data.origin == null ? null : src.getServer().getLevel(data.dimension);
        if (level == null) return fail(src, "No arena yet. Run /paintball arena build.");
        Arena.build(level, data.origin);
        src.sendSuccess(() -> Component.literal("Arena reset and all paint washed off."), true);
        return 1;
    }

    private static int tp(CommandSourceStack src) throws CommandSyntaxException {
        ServerPlayer player = src.getPlayerOrException();
        if (Match.isPlaying(player)) return fail(src, "You are already in the match.");
        ArenaData data = ArenaData.get(src.getServer());
        ServerLevel level = data.origin == null ? null : src.getServer().getLevel(data.dimension);
        if (level == null) return fail(src, "No arena yet.");
        toArena(player, level, data.origin);
        return 1;
    }

    private static void toArena(ServerPlayer player, ServerLevel level, BlockPos origin) {
        Vec3 spot = Arena.spawn(origin, true, player.getRandom());
        player.teleportTo(level, spot.x, spot.y, spot.z, 0.0F, 0.0F);
    }

    private static int start(CommandSourceStack src, int splats) {
        String error = Match.start(src.getServer(), splats);
        return error == null ? 1 : fail(src, error);
    }

    private static int stop(CommandSourceStack src) {
        if (!Match.isActive()) return fail(src, "No match is running.");
        Match.stop(src.getServer());
        src.sendSuccess(() -> Component.literal("Paintball match stopped."), true);
        return 1;
    }

    private static int join(CommandSourceStack src, @Nullable Boolean red) throws CommandSyntaxException {
        ServerPlayer player = src.getPlayerOrException();
        Match.join(src.getServer(), player, red);
        boolean onRed = Match.isRed(player);
        src.sendSuccess(() -> Component.literal("You are on the ")
                .append(Component.literal(onRed ? "red" : "blue").withStyle(onRed ? ChatFormatting.RED : ChatFormatting.BLUE))
                .append(" team."), false);
        return 1;
    }

    private static int leave(CommandSourceStack src) throws CommandSyntaxException {
        ServerPlayer player = src.getPlayerOrException();
        Match.leave(src.getServer(), player);
        src.sendSuccess(() -> Component.literal("You left paintball."), false);
        return 1;
    }

    private static int kit(CommandSourceStack src) throws CommandSyntaxException {
        Match.giveKit(src.getPlayerOrException());
        return 1;
    }

    private static int fail(CommandSourceStack src, String message) {
        src.sendFailure(Component.literal(message));
        return 0;
    }

    private PaintballCommand() {}
}
