package com.splatball.client;

import com.splatball.entity.BotEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;

/** Bots look like a default player wearing their team-coloured armour and holding their marker. */
public class BotRenderer extends HumanoidMobRenderer<BotEntity, HumanoidModel<BotEntity>> {
    private static final ResourceLocation SKIN = new ResourceLocation("textures/entity/player/wide/steve.png");

    public BotRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER)), 0.5F);
        addLayer(new HumanoidArmorLayer<>(this,
                new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                ctx.getModelManager()));
    }

    @Override
    public void render(BotEntity bot, float yaw, float partialTick, com.mojang.blaze3d.vertex.PoseStack pose,
                       net.minecraft.client.renderer.MultiBufferSource buffers, int light) {
        this.model.rightArmPose = HumanoidModel.ArmPose.CROSSBOW_HOLD;   // aiming, same as players
        super.render(bot, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(BotEntity bot) {
        return SKIN;
    }
}
