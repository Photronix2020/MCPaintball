package com.splatball.game;

import com.splatball.Splatball;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Splatball.MODID)
public final class GameEvents {
    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        PaintballCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void death(LivingDeathEvent event) {
        Match.onDeath(event);
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            Match.tick(event.getServer());
            Arena.tick();
        }
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // a tick later, once the player is fully in the world
            player.server.execute(() -> Match.autoJoin(player));
        }
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && Match.isPlaying(player)) {
            Match.leave(player.server, player);
        }
    }

    @SubscribeEvent
    public static void stopping(ServerStoppingEvent event) {
        Match.stop(event.getServer());
        Arena.forgetLevel();
    }

    private GameEvents() {}
}
