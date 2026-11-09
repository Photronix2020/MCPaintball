package com.splatball.client;

import com.splatball.Splatball;
import com.splatball.item.MarkerItem;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = Splatball.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    public static final KeyMapping RELOAD =
            new KeyMapping("key.splatball.reload", GLFW.GLFW_KEY_R, "key.categories.splatball");

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(Splatball.PAINTBALL_ENTITY.get(), ctx -> new ThrownItemRenderer<>(ctx, 0.6F, true));
        event.registerEntityRenderer(Splatball.BOT.get(), BotRenderer::new);
    }

    @SubscribeEvent
    public static void keys(RegisterKeyMappingsEvent event) {
        event.register(RELOAD);
    }

    @SubscribeEvent
    public static void colors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, layer) -> {
            CompoundTag tag = stack.getTag();
            return tag != null && tag.contains("Color") ? DyeColor.byId(tag.getInt("Color")).getFireworkColor() : 0xFFFFFF;
        }, Splatball.PAINTBALL.get());
    }

    @SubscribeEvent
    public static void overlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("ammo", ClientSetup::drawAmmo);
    }

    /** Ammo counter just below-right of the crosshair. */
    private static void drawAmmo(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        ItemStack stack = mc.player.getMainHandItem();
        if (!(stack.getItem() instanceof MarkerItem marker)) return;
        int ammo = MarkerItem.getAmmo(stack);
        int magazine = marker.stats().magazine();
        boolean reloading = MarkerItem.isReloading(stack);
        String text = reloading ? I18n.get("hud.splatball.reloading") : ammo + " / " + magazine;
        int color = reloading ? 0xFFFF55 : ammo * 4 <= magazine ? 0xFF5555 : 0xFFFFFF;
        graphics.drawString(mc.font, text, width / 2 + 12, height / 2 + 8, color, true);
    }

    private ClientSetup() {}
}
