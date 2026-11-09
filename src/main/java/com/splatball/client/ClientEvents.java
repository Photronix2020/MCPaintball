package com.splatball.client;

import com.splatball.Net;
import com.splatball.Splatball;
import com.splatball.item.MarkerItem;
import com.splatball.item.MarkerStats;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Splatball.MODID, value = Dist.CLIENT)
public final class ClientEvents {
    private static long ticks;
    private static long nextFire;

    /** While the use key is held with a marker in hand, ask the server to fire at the marker's rate. */
    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        ticks++;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.screen != null || player.isSpectator()) return;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof MarkerItem marker)) return;

        while (ClientSetup.RELOAD.consumeClick()) Net.CHANNEL.sendToServer(new Net.Action(true));

        if (mc.options.keyUse.isDown() && ticks >= nextFire && !player.getCooldowns().isOnCooldown(marker)) {
            MarkerStats stats = marker.stats();
            nextFire = ticks + stats.cooldown();
            Net.CHANNEL.sendToServer(new Net.Action(false));
            if (MarkerItem.getAmmo(stack) > 0) {
                // a little muzzle climb
                player.setXRot(player.getXRot() - Math.min(2.5F, stats.damage() * stats.pellets() * 0.08F));
            }
        }
    }

    /** Sneak with a scoped marker to zoom. */
    @SubscribeEvent
    public static void fov(ComputeFovModifierEvent event) {
        if (event.getPlayer().getMainHandItem().getItem() instanceof MarkerItem marker
                && marker.stats().zoom() < 1.0F && event.getPlayer().isShiftKeyDown()) {
            event.setNewFovModifier(event.getNewFovModifier() * marker.stats().zoom());
        }
    }

    private ClientEvents() {}
}
