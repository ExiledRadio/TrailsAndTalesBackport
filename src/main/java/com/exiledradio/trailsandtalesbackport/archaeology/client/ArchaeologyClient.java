package com.exiledradio.trailsandtalesbackport.archaeology.client;

import com.exiledradio.trailsandtalesbackport.archaeology.ArchaeologyContent;
import com.exiledradio.trailsandtalesbackport.archaeology.BlockDecoratedPot;
import com.exiledradio.trailsandtalesbackport.archaeology.BlockSuspicious;
import com.exiledradio.trailsandtalesbackport.archaeology.ItemBrush;
import com.exiledradio.trailsandtalesbackport.archaeology.TileDecoratedPot;
import com.exiledradio.trailsandtalesbackport.archaeology.TileSuspicious;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.client.event.RenderSpecificHandEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Stream A's client side: the pot and suspicious-block renderers, the pot item's renderer, the brush's
 * {@code brushing} model property and first-person brushing motion, and the two effect packets.
 */
public final class ArchaeologyClient {

    private ArchaeologyClient() {
    }

    public static void init() {
        if (!ArchaeologyContent.enabled()) {
            return;
        }
        ClientRegistry.bindTileEntitySpecialRenderer(TileDecoratedPot.class, new RenderDecoratedPot());
        ClientRegistry.bindTileEntitySpecialRenderer(TileSuspicious.class, new RenderSuspicious());
        BlockDecoratedPot.localShattering = ArchaeologyClient::localShattering;
        MinecraftForge.EVENT_BUS.register(new ArchaeologyClient());
    }

    /** Called from the item registry event's end: the items exist by then. */
    @SubscribeEvent
    public void registerModels(net.minecraftforge.client.event.ModelRegistryEvent event) {
        if (ArchaeologyContent.potItem != null) {
            ArchaeologyContent.potItem.setTileEntityItemStackRenderer(new RenderDecoratedPot.ItemRenderer());
        }
        if (ArchaeologyContent.brush != null) {
            ArchaeologyContent.brush.addPropertyOverride(new ResourceLocation("brushing"),
                    (stack, world, entity) -> ItemBrush.brushing(stack, entity));
        }
    }

    private static boolean localShattering(BlockPos pos) {
        Minecraft mc = Minecraft.getMinecraft();
        RayTraceResult over = mc.objectMouseOver;
        return mc.player != null && over != null && over.typeOfHit == RayTraceResult.Type.BLOCK
                && pos.equals(over.getBlockPos()) && BlockDecoratedPot.shatters(mc.player.getHeldItemMainhand());
    }

    // --- packets ---------------------------------------------------------------------------------

    public static void brushCompleted(final BlockPos pos, final int stateId) {
        final Minecraft mc = Minecraft.getMinecraft();
        mc.addScheduledTask(() -> {
            if (mc.world == null) {
                return;
            }
            IBlockState state = Block.getStateById(stateId);
            if (state.getBlock() instanceof BlockSuspicious) {
                mc.world.playSound(pos, ((BlockSuspicious) state.getBlock()).brushCompletedSound(), SoundCategory.PLAYERS,
                        1.0F, 1.0F, false);
            }
            mc.effectRenderer.addBlockDestroyEffects(pos, state);
        });
    }

    public static void dustPlume(final BlockPos pos) {
        final Minecraft mc = Minecraft.getMinecraft();
        mc.addScheduledTask(() -> {
            if (mc.world == null) {
                return;
            }
            for (int i = 0; i < 7; i++) {
                mc.effectRenderer.addEffect(new ParticleDustPlume(mc.world, pos.getX() + 0.5D, pos.getY() + 1.2D,
                        pos.getZ() + 0.5D, 0.0D, 0.0D, 0.0D));
            }
        });
    }

    // --- first-person brushing -------------------------------------------------------------------

    /** 1.20's {@code applyBrushTransform}: the brush sweeps back and forth once per stroke. */
    @SubscribeEvent
    public void renderHand(RenderSpecificHandEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.player;
        if (player == null || ArchaeologyContent.brush == null || event.getItemStack().getItem() != ArchaeologyContent.brush
                || !player.isHandActive() || player.getActiveHand() != event.getHand() || player.getItemInUseCount() <= 0) {
            return;
        }
        event.setCanceled(true);
        EnumHandSide side = event.getHand() == EnumHand.MAIN_HAND ? player.getPrimaryHand() : player.getPrimaryHand().opposite();
        boolean right = side == EnumHandSide.RIGHT;
        float partial = event.getPartialTicks();
        GlStateManager.pushMatrix();
        int i = right ? 1 : -1;
        GlStateManager.translate((float) i * 0.56F, -0.52F + event.getEquipProgress() * -0.6F, -0.72F);
        float f = (float) (player.getItemInUseCount() % 10);
        float stroke = f - partial + 1.0F;
        float phase = 1.0F - stroke / 10.0F;
        float angle = -15.0F + 75.0F * MathHelper.cos(phase * 2.0F * (float) Math.PI);
        if (!right) {
            GlStateManager.translate(0.1F, 0.83F, 0.35F);
            GlStateManager.rotate(-80.0F, 1.0F, 0.0F, 0.0F);
            GlStateManager.rotate(-90.0F, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(angle, 1.0F, 0.0F, 0.0F);
            GlStateManager.translate(-0.3F, 0.22F, 0.35F);
        } else {
            GlStateManager.translate(-0.25F, 0.22F, 0.35F);
            GlStateManager.rotate(-80.0F, 1.0F, 0.0F, 0.0F);
            GlStateManager.rotate(90.0F, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(angle, 1.0F, 0.0F, 0.0F);
        }
        mc.getItemRenderer().renderItemSide(player, event.getItemStack(), right
                ? ItemCameraTransforms.TransformType.FIRST_PERSON_RIGHT_HAND
                : ItemCameraTransforms.TransformType.FIRST_PERSON_LEFT_HAND, !right);
        GlStateManager.popMatrix();
    }
}
