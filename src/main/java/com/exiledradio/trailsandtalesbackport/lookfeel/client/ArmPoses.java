package com.exiledradio.trailsandtalesbackport.lookfeel.client;

import com.exiledradio.backportcore.pose.client.PlayerModelHook;
import com.exiledradio.backportcore.pose.client.PoseClient;
import com.exiledradio.backportcore.presentation.Presentation;
import com.exiledradio.trailsandtalesbackport.archaeology.ItemBrush;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Third-person arm poses of 1.20.x, on Backport Core's player model hook (Core's patch P3, run after 1.12.2's own
 * angles, so each pose here is 1.20.6's minus what 1.12.2 already did):
 * <ul>
 *   <li><b>Brushing</b> (1.20, {@code HumanoidModel.ArmPose.BRUSH}): {@code arm.xRot = arm.xRot * 0.5 - PI / 5},
 *       {@code yRot = 0}. 1.12.2 draws a used brush as any held item ({@code ITEM}: {@code * 0.5 - PI / 10}).</li>
 *   <li><b>Blocking</b> (1.20.5, {@code poseBlockingArm}): 1.12.2's shield arm, plus the head's pitch clamped to
 *       -80..25 degrees and its yaw clamped to -30..30 degrees, so the shield follows the look.</li>
 * </ul>
 */
@SideOnly(Side.CLIENT)
public final class ArmPoses implements PlayerModelHook {

    private static final float PI = (float) Math.PI;

    /** For the client self-test: how many arms each pose set. */
    public static volatile int brushPosed;
    public static volatile int blockPosed;

    public static void register() {
        PoseClient.addModelHook(new ArmPoses());
    }

    @Override
    public void apply(ModelBiped model, EntityPlayer player) {
        if (model.rightArmPose == ModelBiped.ArmPose.BLOCK || model.leftArmPose == ModelBiped.ArmPose.BLOCK) {
            if (Presentation.isAtLeast("1.20.5")) {
                if (model.rightArmPose == ModelBiped.ArmPose.BLOCK) {
                    block(model, model.bipedRightArm);
                }
                if (model.leftArmPose == ModelBiped.ArmPose.BLOCK) {
                    block(model, model.bipedLeftArm);
                }
            }
        }
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

    private static void block(ModelBiped model, ModelRenderer arm) {
        arm.rotateAngleX += MathHelper.clamp(model.bipedHead.rotateAngleX, -PI * 4.0F / 9.0F, 0.43633232F);
        arm.rotateAngleY += MathHelper.clamp(model.bipedHead.rotateAngleY, -PI / 6.0F, PI / 6.0F);
        blockPosed++;
    }
}
