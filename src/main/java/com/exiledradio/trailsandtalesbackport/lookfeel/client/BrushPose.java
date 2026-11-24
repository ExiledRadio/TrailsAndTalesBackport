package com.exiledradio.trailsandtalesbackport.lookfeel.client;

import com.exiledradio.backportcore.pose.client.PlayerModelHook;
import com.exiledradio.backportcore.pose.client.PoseClient;
import com.exiledradio.trailsandtalesbackport.archaeology.ItemBrush;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumHandSide;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 1.20's third-person brushing pose ({@code HumanoidModel.ArmPose.BRUSH}), on Backport Core's player model hook (Core's
 * patch P3, run after 1.12.2's own angles): {@code arm.xRot = arm.xRot * 0.5 - PI / 5}, {@code yRot = 0}. 1.12.2 draws
 * a used brush as any held item ({@code ITEM}: {@code * 0.5 - PI / 10}). 1.20.5's shield arm is Backport Core's since
 * Core 1.4.7.
 */
@SideOnly(Side.CLIENT)
public final class BrushPose implements PlayerModelHook {

    private static final float PI = (float) Math.PI;

    /** For the client self-test: how many arms the pose set. */
    public static volatile int brushPosed;

    public static void register() {
        PoseClient.addModelHook(new BrushPose());
    }

    @Override
    public void apply(ModelBiped model, EntityPlayer player) {
        if (player.isHandActive() && player.getActiveItemStack().getItem() instanceof ItemBrush) {
            EnumHandSide side = player.getActiveHand() == EnumHand.MAIN_HAND ? player.getPrimaryHand() : player.getPrimaryHand().opposite();
            ModelRenderer arm = side == EnumHandSide.RIGHT ? model.bipedRightArm : model.bipedLeftArm;
            ModelBiped.ArmPose pose = side == EnumHandSide.RIGHT ? model.rightArmPose : model.leftArmPose;
            if (pose == ModelBiped.ArmPose.ITEM) {
                arm.rotateAngleX += -PI / 5.0F + PI / 10.0F;
                arm.rotateAngleY = 0.0F;
                brushPosed++;
            }
        }
    }
}
