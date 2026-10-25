package com.carpinchoia.client;

import com.carpinchoia.CarpinchoEntity;
import com.carpinchoia.CarpinchoIA;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

/** Capibara cuadrado: cuerpo largo, cabeza con hocico oscuro, orejitas negras. */
public class CarpinchoModel extends EntityModel<CarpinchoEntity> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(new ResourceLocation(CarpinchoIA.MODID, "carpincho"), "main");

    private final ModelPart root, head, frontRight, frontLeft, backRight, backLeft;

    public CarpinchoModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.frontRight = root.getChild("front_right");
        this.frontLeft = root.getChild("front_left");
        this.backRight = root.getChild("back_right");
        this.backLeft = root.getChild("back_left");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition p = mesh.getRoot();
        p.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-5.0F, -5.0F, -9.0F, 10.0F, 9.0F, 18.0F), PartPose.offset(0.0F, 15.0F, 0.0F));
        p.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 28).addBox(-3.5F, -3.5F, -6.0F, 7.0F, 7.0F, 6.0F)
                .texOffs(28, 28).addBox(-3.0F, -0.5F, -9.0F, 6.0F, 4.0F, 3.0F)
                .texOffs(46, 28).addBox(-3.5F, -5.5F, -3.0F, 2.0F, 2.0F, 1.0F)
                .texOffs(46, 28).addBox(1.5F, -5.5F, -3.0F, 2.0F, 2.0F, 1.0F),
                PartPose.offset(0.0F, 13.0F, -8.0F));
        CubeListBuilder leg = CubeListBuilder.create().texOffs(0, 44).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 5.0F, 3.0F);
        p.addOrReplaceChild("front_right", leg, PartPose.offset(-3.5F, 19.0F, -6.5F));
        p.addOrReplaceChild("front_left", leg, PartPose.offset(3.5F, 19.0F, -6.5F));
        p.addOrReplaceChild("back_right", leg, PartPose.offset(-3.5F, 19.0F, 6.5F));
        p.addOrReplaceChild("back_left", leg, PartPose.offset(3.5F, 19.0F, 6.5F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(CarpinchoEntity e, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.head.yRot = netHeadYaw * ((float) Math.PI / 180F);
        this.head.xRot = headPitch * ((float) Math.PI / 180F);
        float a = (float) Math.cos(limbSwing * 0.6662F) * 1.2F * limbSwingAmount;
        this.frontRight.xRot = a;
        this.backLeft.xRot = a;
        this.frontLeft.xRot = -a;
        this.backRight.xRot = -a;
    }

    @Override
    public void renderToBuffer(PoseStack ps, VertexConsumer vc, int light, int overlay, float r, float g, float b, float a) {
        this.root.render(ps, vc, light, overlay, r, g, b, a);
    }
}
