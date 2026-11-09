package com.splatball.test;

import com.splatball.Splatball;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Dev-only (excluded from the release jar). With the environment variable SPLATBALL_SHOTS set, the dev
 * client makes a throwaway flat world, lets auto-play drop the player into the arena, and for each marker
 * saves two screenshots to run/screenshots: the first-person view, and the player seen from their left
 * side (through an armour stand used as a camera). Then it quits. Used to tune how the models are held.
 */
@Mod.EventBusSubscriber(modid = Splatball.MODID, value = Dist.CLIENT)
public final class ShotHook {
    private static final boolean ON = System.getenv("SPLATBALL_SHOTS") != null;
    private static final int START = 160, STEP = 50;
    private static boolean creating;
    private static int ticks;
    private static ArmorStand stand;
    private static volatile int standId = -1;

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (!ON || event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            if (!creating && mc.screen instanceof TitleScreen) {
                creating = true;
                mc.options.pauseOnLostFocus = false;
                String name = "shots" + System.currentTimeMillis();
                mc.createWorldOpenFlows().createFreshLevel(name,
                        new LevelSettings(name, GameType.CREATIVE, false, Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT),
                        new WorldOptions(1L, false, false),
                        registries -> registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
            }
            return;
        }
        IntegratedServer server = mc.getSingleplayerServer();
        if (server == null) return;
        ticks++;
        if (ticks < START) return;
        int step = (ticks - START) / STEP, phase = (ticks - START) % STEP;
        if (step >= Splatball.MARKERS.size()) {
            mc.stop();
            return;
        }
        if (phase == 0) {
            mc.setCameraEntity(mc.player);
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            mc.player.getInventory().selected = step;
            mc.player.setXRot(0);
            server.execute(() -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                if (stand == null) {
                    stand = EntityType.ARMOR_STAND.create(player.serverLevel());
                    stand.setNoGravity(true);
                    stand.setInvisible(true);
                    player.serverLevel().addFreshEntity(stand);
                    standId = stand.getId();
                }
                Vec3 look = player.getLookAngle().multiply(1, 0, 1).normalize();
                Vec3 side = player.position().add(new Vec3(look.z, 0, -look.x).scale(2.2)).add(look.scale(0.4));
                stand.moveTo(side.x, player.getY() - 0.3, side.z, player.getYRot() + 90.0F, 0.0F);
                stand.setYHeadRot(player.getYRot() + 90.0F);
            });
        } else if (phase == 25) {
            Screenshot.grab(mc.gameDirectory, "gun" + step + "_first.png", mc.getMainRenderTarget(), c -> {});
            Entity camera = mc.level.getEntity(standId);
            if (camera != null) mc.setCameraEntity(camera);
        } else if (phase == 45) {
            Screenshot.grab(mc.gameDirectory, "gun" + step + "_side.png", mc.getMainRenderTarget(), c -> {});
            mc.setCameraEntity(mc.player);
        }
    }

    private ShotHook() {}
}
