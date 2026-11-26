package com.exiledradio.trailsandtalesbackport.archaeology;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import javax.annotation.Nullable;
import java.util.List;

/**
 * The brush: 1.20's {@code BrushItem}, 64 durability.
 *
 * <p>Using it on a block starts a 200-tick use. While the player keeps looking at a block (an entity in the
 * way, or nothing, stops it), every 10th tick (the 5th, 15th, ...) is a stroke: dust particles off the
 * face, the block's brushing sound, and on a suspicious block one brush of its tile entity, which costs one
 * durability when it finishes the block. Holding the button starts the next use when one runs out.
 */
public class ItemBrush extends Item {

    /** Vanilla's {@code ANIMATION_DURATION}: one stroke every 10 ticks. */
    public static final int ANIMATION_DURATION = 10;
    private static final int USE_DURATION = 200;

    private final SoundEvent generic;

    public ItemBrush(SoundEvent generic) {
        this.generic = generic;
        setMaxStackSize(1);
        setMaxDamage(64);
    }

    /**
     * Starts brushing when a block is in reach. FAIL keeps 1.12.2 from swinging the arm (vanilla's
     * CONSUME); the other hand is kept from using its item meanwhile by {@link ArchaeologyContent}.
     */
    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing,
                                      float hitX, float hitY, float hitZ) {
        RayTraceResult hit = hitResult(player);
        if (hit != null && hit.typeOfHit == RayTraceResult.Type.BLOCK) {
            player.setActiveHand(hand);
        }
        return EnumActionResult.FAIL;
    }

    @Override
    public EnumAction getItemUseAction(ItemStack stack) {
        return EnumAction.NONE;
    }

    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        return USE_DURATION;
    }

    @Override
    public void onUsingTick(ItemStack stack, EntityLivingBase entity, int remaining) {
        if (remaining < 0 || !(entity instanceof EntityPlayer)) {
            entity.stopActiveHand();
            return;
        }
        EntityPlayer player = (EntityPlayer) entity;
        RayTraceResult hit = hitResult(player);
        if (hit == null || hit.typeOfHit != RayTraceResult.Type.BLOCK) {
            entity.stopActiveHand();
            return;
        }
        int elapsed = getMaxItemUseDuration(stack) - remaining + 1;
        if (elapsed % ANIMATION_DURATION != 5) {
            return;
        }
        World world = player.world;
        BlockPos pos = hit.getBlockPos();
        IBlockState state = world.getBlockState(pos);
        EnumHandSide arm = player.getActiveHand() == EnumHand.MAIN_HAND ? player.getPrimaryHand()
                : player.getPrimaryHand() == EnumHandSide.RIGHT ? EnumHandSide.LEFT : EnumHandSide.RIGHT; // opposite() is client-only
        if (world.isRemote && state.getRenderType() != EnumBlockRenderType.INVISIBLE && state.getBlock() != Blocks.BARRIER) {
            spawnDustParticles(world, hit, state, player.getLook(0.0F), arm);
        }
        Block block = state.getBlock();
        SoundEvent sound = block instanceof BlockSuspicious ? ((BlockSuspicious) block).brushSound() : this.generic;
        world.playSound(player, pos, sound, SoundCategory.BLOCKS, 1.0F, 1.0F);
        if (world instanceof WorldServer) {
            TileEntity te = world.getTileEntity(pos);
            if (te instanceof TileSuspicious
                    && ((TileSuspicious) te).brush(world.getTotalWorldTime(), player, hit.sideHit)) {
                stack.damageItem(1, player);
            }
        }
    }

    /**
     * What the player is looking at within block reach, entities included (vanilla's
     * {@code getHitResultOnViewVector}): the block hit, or an ENTITY or MISS result.
     */
    @Nullable
    static RayTraceResult hitResult(EntityPlayer player) {
        double reach = player.getEntityAttribute(EntityPlayer.REACH_DISTANCE).getAttributeValue();
        if (!player.capabilities.isCreativeMode) {
            reach -= 0.5D;
        }
        Vec3d eyes = player.getPositionEyes(1.0F);
        Vec3d look = player.getLook(1.0F);
        Vec3d end = eyes.add(look.x * reach, look.y * reach, look.z * reach);
        RayTraceResult block = player.world.rayTraceBlocks(eyes, end, false, false, false);
        double best = block != null ? block.hitVec.distanceTo(eyes) : reach;
        AxisAlignedBB sweep = player.getEntityBoundingBox().expand(look.x * reach, look.y * reach, look.z * reach)
                .grow(1.0D);
        List<Entity> entities = player.world.getEntitiesInAABBexcluding(player, sweep,
                e -> e != null && e.canBeCollidedWith() && !(e instanceof EntityPlayer && ((EntityPlayer) e).isSpectator()));
        for (Entity e : entities) {
            AxisAlignedBB box = e.getEntityBoundingBox().grow(e.getCollisionBorderSize());
            RayTraceResult through = box.calculateIntercept(eyes, end);
            if (box.contains(eyes)) {
                return new RayTraceResult(e, eyes);
            }
            if (through != null && eyes.distanceTo(through.hitVec) < best) {
                return new RayTraceResult(e, through.hitVec);
            }
        }
        return block;
    }

    /** Vanilla's dust spray: 7-11 block particles off the hit point, swept along the face. */
    private static void spawnDustParticles(World world, RayTraceResult hit, IBlockState state, Vec3d view, EnumHandSide arm) {
        int side = arm == EnumHandSide.RIGHT ? 1 : -1;
        int count = 7 + world.rand.nextInt(5);
        EnumFacing face = hit.sideHit;
        double xd;
        double zd;
        switch (face) {
            case DOWN:
            case UP:
                xd = view.z;
                zd = -view.x;
                break;
            case NORTH:
                xd = 1.0D;
                zd = -0.1D;
                break;
            case SOUTH:
                xd = -1.0D;
                zd = 0.1D;
                break;
            case WEST:
                xd = -0.1D;
                zd = -1.0D;
                break;
            default:
                xd = 0.1D;
                zd = 1.0D;
                break;
        }
        Vec3d at = hit.hitVec;
        int id = Block.getStateId(state);
        for (int i = 0; i < count; i++) {
            world.spawnParticle(EnumParticleTypes.BLOCK_CRACK,
                    at.x - (face == EnumFacing.WEST ? 1.0E-6F : 0.0F), at.y,
                    at.z - (face == EnumFacing.NORTH ? 1.0E-6F : 0.0F),
                    xd * side * 3.0D * world.rand.nextDouble(), 0.0D, zd * side * 3.0D * world.rand.nextDouble(), id);
        }
    }

    /** The {@code brushing} model predicate (1.20's item property): stroke progress, 0 when not in use. */
    public static float brushing(ItemStack stack, @Nullable EntityLivingBase entity) {
        if (entity == null || entity.getActiveItemStack() != stack) {
            return 0.0F;
        }
        return (float) (entity.getItemInUseCount() % ANIMATION_DURATION) / (float) ANIMATION_DURATION;
    }
}
