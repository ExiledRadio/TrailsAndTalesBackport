package com.exiledradio.trailsandtalesbackport.sign;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

/**
 * Stream Sg's packets (ids 30 and 31 of its block 30-34). Vanilla's sign packets have no face, so the
 * editor uses these instead.
 *
 * <ul>
 *   <li>30, {@link Update} (to the server): 1.20's {@code ServerboundSignUpdatePacket} - position, face,
 *       four lines of at most 384 characters.</li>
 *   <li>31, {@link Open} (to the client): 1.20's {@code ClientboundOpenSignEditorPacket} - position, face.</li>
 *   <li>32, {@link WaxOn} (to the clients within 64 blocks): level event 3003's particles - position.</li>
 * </ul>
 * The client handler runs through {@code sign.client.SignClient}, named only inside a method body.
 */
public final class SignNetwork {

    public static final int UPDATE = 30;
    public static final int OPEN = 31;
    public static final int WAX_ON = 32;

    private SignNetwork() {
    }

    public static void register(SimpleNetworkWrapper channel) {
        if (!SignsContent.enabled()) {
            return;
        }
        channel.registerMessage(Update.Handler.class, Update.class, UPDATE, Side.SERVER);
        channel.registerMessage(Open.Handler.class, Open.class, OPEN, Side.CLIENT);
        channel.registerMessage(WaxOn.Handler.class, WaxOn.class, WAX_ON, Side.CLIENT);
    }

    public static final class Update implements IMessage {
        public BlockPos pos = BlockPos.ORIGIN;
        public boolean front = true;
        public String[] lines = {"", "", "", ""};

        public Update() {
        }

        public Update(BlockPos pos, boolean front, String[] lines) {
            this.pos = pos;
            this.front = front;
            this.lines = lines.clone();
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            PacketBuffer b = new PacketBuffer(buf);
            this.pos = b.readBlockPos();
            this.front = b.readBoolean();
            this.lines = new String[4];
            for (int i = 0; i < 4; i++) {
                this.lines[i] = b.readString(SignEditing.MAX_LINE);
            }
        }

        @Override
        public void toBytes(ByteBuf buf) {
            PacketBuffer b = new PacketBuffer(buf);
            b.writeBlockPos(this.pos);
            b.writeBoolean(this.front);
            for (int i = 0; i < 4; i++) {
                String s = i < this.lines.length && this.lines[i] != null ? this.lines[i] : "";
                b.writeString(s.length() > SignEditing.MAX_LINE ? s.substring(0, SignEditing.MAX_LINE) : s);
            }
        }

        public static final class Handler implements IMessageHandler<Update, IMessage> {
            @Override
            public IMessage onMessage(final Update message, MessageContext ctx) {
                final EntityPlayerMP player = ctx.getServerHandler().player;
                ((WorldServer) player.world).addScheduledTask(() -> {
                    player.markPlayerActive();
                    SignEditing.update(player, message.pos, message.front, message.lines);
                });
                return null;
            }
        }
    }

    public static final class Open implements IMessage {
        public BlockPos pos = BlockPos.ORIGIN;
        public boolean front = true;

        public Open() {
        }

        public Open(BlockPos pos, boolean front) {
            this.pos = pos;
            this.front = front;
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            this.pos = BlockPos.fromLong(buf.readLong());
            this.front = buf.readBoolean();
        }

        @Override
        public void toBytes(ByteBuf buf) {
            buf.writeLong(this.pos.toLong());
            buf.writeBoolean(this.front);
        }

        public static final class Handler implements IMessageHandler<Open, IMessage> {
            @Override
            public IMessage onMessage(Open message, MessageContext ctx) {
                com.exiledradio.trailsandtalesbackport.sign.client.SignClient.open(message.pos, message.front);
                return null;
            }
        }
    }

    /** Level event 3003's particles: wax-on particles on every face of the waxed block. */
    public static final class WaxOn implements IMessage {
        public BlockPos pos = BlockPos.ORIGIN;

        public WaxOn() {
        }

        public WaxOn(BlockPos pos) {
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

        public static final class Handler implements IMessageHandler<WaxOn, IMessage> {
            @Override
            public IMessage onMessage(WaxOn message, MessageContext ctx) {
                com.exiledradio.trailsandtalesbackport.sign.client.WaxParticles.receive(message.pos);
                return null;
            }
        }
    }
}
