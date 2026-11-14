package com.exiledradio.trailsandtalesbackport.sign;

import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import com.exiledradio.trailsandtalesbackport.network.NetworkHandler;

import net.minecraft.block.state.IBlockState;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.world.World;

import java.util.UUID;

/**
 * The server's side of editing a sign, as 1.20's {@code SignBlockEntity} and {@code SignBlock}.
 *
 * <ul>
 *   <li>{@link #open}: the player may edit this face; the client is sent the sign and told to open the
 *       editor for that face (1.20's {@code ServerPlayer.openTextEdit}).</li>
 *   <li>{@link #update}: text from the editor, accepted only from the player who may edit, while the sign
 *       is not waxed and they are within 8 blocks (1.20's {@code updateSignText}); each line keeps its
 *       old style, its formatting codes are stripped.</li>
 *   <li>{@link #runCommands}: the click commands of one face (1.20 runs a face's own).</li>
 * </ul>
 * A sign placed from an item opens vanilla's editor (vanilla's {@code openEditSign} marks the placer); the
 * client swaps in our screen, whose text arrives here as a front update that vanilla's mark allows.
 */
public final class SignEditing {

    /** 1.20's {@code ServerboundSignUpdatePacket} reads lines of at most 384 characters. */
    public static final int MAX_LINE = 384;

    private SignEditing() {
    }

    public static void open(EntityPlayerMP player, TileEntitySign sign, boolean front) {
        SignData d = SignSides.data(sign);
        if (d == null) {
            return;
        }
        d.editor = player.getUniqueID();
        SPacketUpdateTileEntity packet = sign.getUpdatePacket();
        if (packet != null && player.connection != null) {
            player.connection.sendPacket(packet);
        }
        if (NetworkHandler.channel != null && player.connection != null && player.connection.getNetworkManager().channel() != null) {
            NetworkHandler.channel.sendTo(new SignNetwork.Open(sign.getPos(), front), player);
        }
    }

    /** 1.20's {@code otherPlayerIsEditingSign}, with its tick's clearing of a gone or far editor done here. */
    public static boolean otherPlayerEditing(TileEntitySign sign, EntityPlayer player) {
        SignData d = SignSides.data(sign);
        if (d != null && d.editor != null) {
            if (d.editor.equals(player.getUniqueID())) {
                return false;
            }
            EntityPlayer other = sign.getWorld().getPlayerEntityByUUID(d.editor);
            if (other != null && !tooFar(sign, other)) {
                return true;
            }
            d.editor = null;
        }
        EntityPlayer placer = sign.getPlayer();
        return placer != null && placer != player && !placer.isDead && placer.world == sign.getWorld() && !tooFar(sign, placer);
    }

    static boolean tooFar(TileEntitySign sign, EntityPlayer player) {
        return player.getDistanceSqToCenter(sign.getPos()) > SignSides.EDIT_DISTANCE_SQ;
    }

    /** @return true if the text was taken */
    public static boolean update(EntityPlayerMP player, BlockPos pos, boolean front, String[] lines) {
        World world = player.world;
        if (!world.isBlockLoaded(pos)) {
            return false;
        }
        TileEntity tile = world.getTileEntity(pos);
        if (!(tile instanceof TileEntitySign)) {
            return false;
        }
        TileEntitySign sign = (TileEntitySign) tile;
        SignData d = SignSides.data(sign);
        UUID id = player.getUniqueID();
        boolean mayEdit = d != null && id.equals(d.editor)
                || front && (d == null || d.editor == null) && sign.getPlayer() == player && sign.getIsEditable();
        if (!mayEdit || SignSides.waxed(sign) || tooFar(sign, player)) {
            TrailsAndTalesBackport.LOGGER.warn("Player {} just tried to change non-editable sign", player.getName());
            return false;
        }
        ITextComponent[] target = SignSides.lines(sign, front);
        for (int i = 0; i < target.length && i < lines.length; i++) {
            String s = lines[i] == null ? "" : lines[i];
            if (s.length() > MAX_LINE) {
                s = s.substring(0, MAX_LINE);
            }
            Style style = target[i] == null ? new Style() : target[i].getStyle().createShallowCopy();
            target[i] = new TextComponentString(TextFormatting.getTextWithoutFormattingCodes(s)).setStyle(style);
        }
        if (d != null) {
            d.editor = null;
        }
        sign.setPlayer(null);
        changed(sign);
        return true;
    }

    /** Saves and sends the sign to everyone watching it. */
    public static void changed(TileEntitySign sign) {
        sign.markDirty();
        IBlockState state = sign.getWorld().getBlockState(sign.getPos());
        sign.getWorld().notifyBlockUpdate(sign.getPos(), state, state, 3);
    }

    /** @return true if a line of this face has a click command (each is run, as 1.20's {@code executeClickCommandsIfPresent}) */
    public static boolean runCommands(final TileEntitySign sign, final EntityPlayer player, boolean front) {
        boolean any = false;
        for (ITextComponent line : SignSides.lines(sign, front)) {
            ClickEvent click = line == null ? null : line.getStyle().getClickEvent();
            if (click != null && click.getAction() == ClickEvent.Action.RUN_COMMAND) {
                any = true;
            }
        }
        if (!any || player.getServer() == null) {
            return any;
        }
        if (front) {
            sign.executeCommand(player);
            return true;
        }
        ICommandSender sender = new ICommandSender() {
            @Override
            public String getName() {
                return player.getName();
            }

            @Override
            public ITextComponent getDisplayName() {
                return player.getDisplayName();
            }

            @Override
            public void sendMessage(ITextComponent component) {
            }

            @Override
            public boolean canUseCommand(int permLevel, String commandName) {
                return permLevel <= 2;
            }

            @Override
            public BlockPos getPosition() {
                return sign.getPos();
            }

            @Override
            public Vec3d getPositionVector() {
                return new Vec3d(sign.getPos()).add(0.5D, 0.5D, 0.5D);
            }

            @Override
            public World getEntityWorld() {
                return player.getEntityWorld();
            }

            @Override
            public Entity getCommandSenderEntity() {
                return player;
            }

            @Override
            public boolean sendCommandFeedback() {
                return false;
            }

            @Override
            public MinecraftServer getServer() {
                return player.getServer();
            }
        };
        for (ITextComponent line : SignSides.lines(sign, false)) {
            ClickEvent click = line == null ? null : line.getStyle().getClickEvent();
            if (click != null && click.getAction() == ClickEvent.Action.RUN_COMMAND) {
                player.getServer().getCommandManager().executeCommand(sender, click.getValue());
            }
        }
        return true;
    }
}
