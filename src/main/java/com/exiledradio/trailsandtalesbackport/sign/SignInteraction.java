package com.exiledradio.trailsandtalesbackport.sign;

import com.exiledradio.backportcore.gameevent.GameEvent;
import com.exiledradio.trailsandtalesbackport.common.Vibrations;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.oredict.DyeUtils;

import javax.annotation.Nullable;

/**
 * Right-clicking a placed sign, as 1.20's {@code SignBlock.use}, on the face toward the player:
 * <ol>
 *   <li>its click commands run;</li>
 *   <li>waxed: the "waxed interact fail" sound and nothing more;</li>
 *   <li>holding a sign applicator, while the player may build and nobody else is editing: dye recolors the
 *       face, glow ink makes it glow, an ink sac stops the glow (each only on a face with text, and only if it
 *       changes anything), honeycomb waxes the sign; the item is used up outside creative;</li>
 *   <li>otherwise, unless it had commands, the editor opens for that face - if the player may build (not in
 *       Adventure mode), nobody else is editing and every line is plain text.</li>
 * </ol>
 * Sneaking with an item in either hand skips all of it, as for any block. Runs ahead of other handlers and
 * is cancelled on both sides, so Village & Pillage's front-only dyeing never also runs, and vanilla's
 * {@code BlockSign} (which would run the front's commands a second time) is never reached.
 *
 * <p>1.12.2's dye items: without 1.14's plain dyes (Village & Pillage), all sixteen are dyes, and the ink
 * sac is also black dye - it stops the glow on a glowing face, else dyes it black. With them, the ink sac,
 * cocoa beans, lapis and bone meal are what 1.20's are: the ink sac only stops the glow, the others open the
 * editor like any item.
 */
public final class SignInteraction {

    /** What an item does to a sign face (1.20's {@code SignApplicator}s). */
    enum Kind { DYE, GLOW, UNGLOW, WAX }

    private SignInteraction() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!SignsContent.enabled() || event.getUseBlock() == Event.Result.DENY) {
            return;
        }
        World world = event.getWorld();
        TileEntity tile = world.getTileEntity(event.getPos());
        if (!(tile instanceof TileEntitySign)) {
            return;
        }
        EntityPlayer player = event.getEntityPlayer();
        if (player.isSneaking() && (!player.getHeldItemMainhand().isEmpty() || !player.getHeldItemOffhand().isEmpty())) {
            return;
        }
        event.setCanceled(true);
        if (world.isRemote) {
            // 1.20's client answers CONSUME or SUCCESS here: the server decides. Forge still sends the click.
            event.setCancellationResult(EnumActionResult.SUCCESS);
            return;
        }
        event.setCancellationResult(use((TileEntitySign) tile, player, event.getItemStack()));
    }

    /** The server's half: @return 1.20's result */
    public static EnumActionResult use(TileEntitySign sign, EntityPlayer player, ItemStack held) {
        boolean front = SignSides.isFacingFront(sign, player);
        boolean commands = SignEditing.runCommands(sign, player, front);
        if (SignSides.waxed(sign)) {
            sign.getWorld().playSound(null, sign.getPos(), SignsContent.WAXED_INTERACT_FAIL, SoundCategory.BLOCKS, 1.0F, 1.0F);
            return EnumActionResult.PASS;
        }
        boolean mayBuild = player.capabilities.allowEdit;
        boolean otherEditing = SignEditing.otherPlayerEditing(sign, player);
        Kind kind = kind(held, sign, front);
        if (kind != null && mayBuild && !otherEditing && apply(kind, held, sign, front, player)) {
            net.minecraft.item.Item used = held.getItem();
            if (!player.capabilities.isCreativeMode) {
                held.shrink(1);
            }
            if (StatList.getObjectUseStats(used) != null) {
                player.addStat(StatList.getObjectUseStats(used));
            }
            // 1.20: dye, glow ink, ink and wax all post block_change from the player.
            Vibrations.block(sign.getWorld(), GameEvent.BLOCK_CHANGE, sign.getPos(), player, sign.getWorld().getBlockState(sign.getPos()));
            SignEditing.changed(sign);
            return EnumActionResult.SUCCESS;
        }
        if (commands) {
            return EnumActionResult.SUCCESS;
        }
        if (!otherEditing && mayBuild && SignSides.editableText(sign, front)) {
            if (player instanceof EntityPlayerMP) {
                SignEditing.open((EntityPlayerMP) player, sign, front);
            }
            return EnumActionResult.SUCCESS;
        }
        return EnumActionResult.PASS;
    }

    /** @return what the held item does to this face, or null for nothing */
    @Nullable
    static Kind kind(ItemStack held, TileEntitySign sign, boolean front) {
        if (held.isEmpty()) {
            return null;
        }
        if (SignsContent.honeycomb != null && held.getItem() == SignsContent.honeycomb) {
            return Kind.WAX;
        }
        if (SignsContent.glowInkSac != null && held.getItem() == SignsContent.glowInkSac) {
            return Kind.GLOW;
        }
        if (held.getItem() == Items.DYE) {
            int meta = held.getMetadata();
            if (meta == EnumDyeColor.BLACK.getDyeDamage()) {
                // The ink sac.
                return SignsContent.modernDyes || SignSides.glowing(sign, front) ? Kind.UNGLOW : Kind.DYE;
            }
            if (SignsContent.modernDyes && (meta == EnumDyeColor.BROWN.getDyeDamage()
                    || meta == EnumDyeColor.BLUE.getDyeDamage() || meta == EnumDyeColor.WHITE.getDyeDamage())) {
                return null;
            }
            return Kind.DYE;
        }
        return DyeUtils.isDye(held) ? Kind.DYE : null;
    }

    /** 1.20's {@code canApplyToSign} and {@code tryApplyToSign}: @return true if the sign changed */
    static boolean apply(Kind kind, ItemStack held, TileEntitySign sign, boolean front, EntityPlayer player) {
        World world = sign.getWorld();
        if (kind == Kind.WAX) {
            // HoneycombItem: any sign, text or not. Level event 3003: the wax-on sound (and particles).
            if (!SignSides.setWaxed(sign, true)) {
                return false;
            }
            world.playSound(null, sign.getPos(), SignsContent.HONEYCOMB_WAX_ON, SoundCategory.BLOCKS, 1.0F, 1.0F);
            BlockPos at = sign.getPos();
            com.exiledradio.trailsandtalesbackport.network.NetworkHandler.channel.sendToAllAround(new SignNetwork.WaxOn(at),
                    new net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint(world.provider.getDimension(),
                            at.getX() + 0.5D, at.getY() + 0.5D, at.getZ() + 0.5D, 64.0D));
            return true;
        }
        if (!SignSides.hasMessage(sign, front)) {
            return false;
        }
        switch (kind) {
            case DYE:
                EnumDyeColor color = held.getItem() == Items.DYE ? EnumDyeColor.byDyeDamage(held.getMetadata())
                        : DyeUtils.colorFromStack(held).orElse(null);
                if (color == null || !SignSides.setColor(sign, front, color)) {
                    return false;
                }
                world.playSound(null, sign.getPos(), SignsContent.DYE_USE, SoundCategory.BLOCKS, 1.0F, 1.0F);
                return true;
            case GLOW:
                if (!SignSides.setGlowing(sign, front, true)) {
                    return false;
                }
                world.playSound(null, sign.getPos(), SignsContent.GLOW_INK_SAC_USE, SoundCategory.BLOCKS, 1.0F, 1.0F);
                return true;
            case UNGLOW:
                if (!SignSides.setGlowing(sign, front, false)) {
                    return false;
                }
                world.playSound(null, sign.getPos(), SignsContent.INK_SAC_USE, SoundCategory.BLOCKS, 1.0F, 1.0F);
                return true;
            default:
                return false;
        }
    }
}
