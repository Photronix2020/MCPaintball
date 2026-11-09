package com.splatball.block;

import com.splatball.game.Match;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Right-click to drop into the paintball match. The first click ever builds the arena in the sky above
 * the station; clicking a station while you are playing takes you back out.
 */
public class StationBlock extends Block {
    public StationBlock(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level instanceof ServerLevel server && player instanceof ServerPlayer serverPlayer) {
            Match.useStation(server, pos, serverPlayer);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
