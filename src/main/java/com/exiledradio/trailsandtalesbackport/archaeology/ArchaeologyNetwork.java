package com.exiledradio.trailsandtalesbackport.archaeology;

import com.exiledradio.trailsandtalesbackport.network.NetworkHandler;

import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

/**
 * Stream A's two packets (ids 40 and 41): the effects 1.12.2 has no level event or particle for.
 *
 * <ul>
 *   <li>40, {@link BrushCompleted}: vanilla's level event 3008, the brush-completed sound of a suspicious
 *       block and its break particles, for everyone within 64 blocks.</li>
 *   <li>41, {@link DustPlume}: the seven {@code dust_plume} particles above a pot that takes an item.</li>
 * </ul>
 * Handlers run on the client thread through {@code archaeology.client.ArchaeologyClient}, named only inside
 * method bodies, so a dedicated server never loads it.
 */
public final class ArchaeologyNetwork {

    public static final int BRUSH_COMPLETED = 40;
    public static final int DUST_PLUME = 41;

    private ArchaeologyNetwork() {
    }

    public static void register(SimpleNetworkWrapper channel) {
        channel.registerMessage(BrushCompleted.Handler.class, BrushCompleted.class, BRUSH_COMPLETED, Side.CLIENT);
        channel.registerMessage(DustPlume.Handler.class, DustPlume.class, DUST_PLUME, Side.CLIENT);
    }

    private static NetworkRegistry.TargetPoint around(World world, BlockPos pos) {
        return new NetworkRegistry.TargetPoint(world.provider.getDimension(), pos.getX() + 0.5D, pos.getY() + 0.5D,
                pos.getZ() + 0.5D, 64.0D);
    }

    static void brushCompleted(World world, BlockPos pos, IBlockState state) {
        if (!world.isRemote && NetworkHandler.channel != null) {
            NetworkHandler.channel.sendToAllAround(new BrushCompleted(pos, Block.getStateId(state)), around(world, pos));
        }
    }

    static void dustPlume(World world, BlockPos pos) {
        if (!world.isRemote && NetworkHandler.channel != null) {
            NetworkHandler.channel.sendToAllAround(new DustPlume(pos), around(world, pos));
        }
    }

    public static final class BrushCompleted implements IMessage {
        public BlockPos pos;
        public int state;

        public BrushCompleted() {
        }

        BrushCompleted(BlockPos pos, int state) {
            this.pos = pos;
            this.state = state;
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            this.pos = BlockPos.fromLong(buf.readLong());
            this.state = buf.readInt();
        }

        @Override
        public void toBytes(ByteBuf buf) {
            buf.writeLong(this.pos.toLong());
            buf.writeInt(this.state);
        }

        public static final class Handler implements IMessageHandler<BrushCompleted, IMessage> {
            @Override
            public IMessage onMessage(BrushCompleted message, MessageContext ctx) {
                com.exiledradio.trailsandtalesbackport.archaeology.client.ArchaeologyClient.brushCompleted(message.pos, message.state);
                return null;
            }
        }
    }

    public static final class DustPlume implements IMessage {
        public BlockPos pos;

        public DustPlume() {
        }

        DustPlume(BlockPos pos) {
            this.pos = pos;
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            this.pos = BlockPos.fromLong(buf.readLong());
        }

        @Override
        public void toBytes(ByteBuf buf) {
            buf.writeLong(this.pos.toLong());
        }

        public static final class Handler implements IMessageHandler<DustPlume, IMessage> {
            @Override
            public IMessage onMessage(DustPlume message, MessageContext ctx) {
                com.exiledradio.trailsandtalesbackport.archaeology.client.ArchaeologyClient.dustPlume(message.pos);
                return null;
            }
        }
    }
}
