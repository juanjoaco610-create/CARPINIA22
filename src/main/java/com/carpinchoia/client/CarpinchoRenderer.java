package com.carpinchoia.client;

import com.carpinchoia.CarpinchoEntity;
import com.carpinchoia.CarpinchoIA;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

public class CarpinchoRenderer extends MobRenderer<CarpinchoEntity, CarpinchoModel> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(CarpinchoIA.MODID, "textures/entity/carpincho.png");
    private static final ResourceLocation LOGO = new ResourceLocation(CarpinchoIA.MODID, "textures/gui/ai_logo.png");

    public CarpinchoRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new CarpinchoModel(ctx.bakeLayer(CarpinchoModel.LAYER)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(CarpinchoEntity entity) {
        return TEXTURE;
    }

    /** Nombre con el logo de IA al lado, y debajo "Radio: 40". */
    @Override
    protected void renderNameTag(CarpinchoEntity e, Component name, PoseStack ps, MultiBufferSource buf, int light) {
        if (this.entityRenderDispatcher.distanceToSqr(e) > 4096.0D) return;
        ps.pushPose();
        ps.translate(0.0F, e.getBbHeight() + 0.65F, 0.0F);
        ps.mulPose(this.entityRenderDispatcher.cameraOrientation());
        ps.scale(-0.025F, -0.025F, 0.025F);
        Matrix4f m = ps.last().pose();
        Font font = this.getFont();
        int bg = (int) (Minecraft.getInstance().options.getBackgroundOpacity(0.25F) * 255.0F) << 24;

        float x = -font.width(name) / 2.0F;
        font.drawInBatch(name, x, 0, 0x20FFFFFF, false, m, buf, Font.DisplayMode.SEE_THROUGH, bg, light);
        font.drawInBatch(name, x, 0, -1, false, m, buf, Font.DisplayMode.NORMAL, 0, light);

        Component radio = Component.literal("Radio: " + e.getRadius());
        float rx = -font.width(radio) / 2.0F;
        font.drawInBatch(radio, rx, 11, 0x20AAAAAA, false, m, buf, Font.DisplayMode.SEE_THROUGH, bg, light);
        font.drawInBatch(radio, rx, 11, 0xFFAAAAAA, false, m, buf, Font.DisplayMode.NORMAL, 0, light);

        // logo de IA a la izquierda del nombre
        VertexConsumer vc = buf.getBuffer(RenderType.text(LOGO));
        float x0 = x - 12.0F, x1 = x - 2.0F, y0 = -1.0F, y1 = 9.0F;
        vc.vertex(m, x0, y1, 0.01F).color(255, 255, 255, 255).uv(0.0F, 1.0F).uv2(light).endVertex();
        vc.vertex(m, x1, y1, 0.01F).color(255, 255, 255, 255).uv(1.0F, 1.0F).uv2(light).endVertex();
        vc.vertex(m, x1, y0, 0.01F).color(255, 255, 255, 255).uv(1.0F, 0.0F).uv2(light).endVertex();
        vc.vertex(m, x0, y0, 0.01F).color(255, 255, 255, 255).uv(0.0F, 0.0F).uv2(light).endVertex();
        ps.popPose();
    }
}
