package com.exiledradio.trailsandtalesbackport.wolf;

import com.exiledradio.trailsandtalesbackport.network.NetworkHandler;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

/**
 * Stream W's packets (ids 70 and 71 of its block 70-74).
 *
 * <ul>
 *   <li>70, {@link State}: a wolf's variant and body armor, which 1.20 keeps in synced entity data. Sent to a
 *       player who starts tracking the wolf and to every tracker when either changes.</li>
 *   <li>71, {@link ArmorBroke}: the body armor broke - 1.20's entity event 65 (break sound and item particles,
 *       played by each client).</li>
 * </ul>
 * Handlers run on the client thread through {@code wolf.client.WolfClient}, named only inside method bodies.
 */
public final class WolfNetwork {

    public static final int STATE = 70;
    public static final int ARMOR_BROKE = 71;

    private WolfNetwork() {
    }

    public static void register(SimpleNetworkWrapper channel) {
        channel.registerMessage(State.Handler.class, State.class, STATE, Side.CLIENT);
        channel.registerMessage(ArmorBroke.Handler.class, ArmorBroke.class, ARMOR_BROKE, Side.CLIENT);
    }

    static void sendState(EntityWolf wolf, EntityPlayerMP to) {
        WolfData data = WolfData.of(wolf);
        if (data != null && NetworkHandler.channel != null) {
            NetworkHandler.channel.sendTo(new State(wolf.getEntityId(), data.variantOrPale().ordinal(), data.armor), to);
        }
    }

    /** Sends the wolf's state to every player tracking it. */
    static void broadcastState(EntityWolf wolf) {
        WolfData data = WolfData.of(wolf);
        if (data != null && wolf.world instanceof WorldServer && NetworkHandler.channel != null) {
            ((WorldServer) wolf.world).getEntityTracker().sendToTracking(wolf, NetworkHandler.channel.getPacketFrom(
                    new State(wolf.getEntityId(), data.variantOrPale().ordinal(), data.armor)));
        }
    }

    static void broadcastArmorBroke(EntityWolf wolf, ItemStack broken) {
        if (wolf.world instanceof WorldServer && NetworkHandler.channel != null) {
            ((WorldServer) wolf.world).getEntityTracker().sendToTracking(wolf, NetworkHandler.channel.getPacketFrom(
                    new ArmorBroke(wolf.getEntityId(), broken)));
        }
    }

    public static final class State implements IMessage {
        public int entity;
        public int variant;
        public ItemStack armor = ItemStack.EMPTY;

        public State() {
        }

        State(int entity, int variant, ItemStack armor) {
            this.entity = entity;
            this.variant = variant;
            this.armor = armor.copy();
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            this.entity = buf.readInt();
            this.variant = buf.readByte();
            this.armor = ByteBufUtils.readItemStack(buf);
        }

        @Override
        public void toBytes(ByteBuf buf) {
            buf.writeInt(this.entity);
            buf.writeByte(this.variant);
            ByteBufUtils.writeItemStack(buf, this.armor);
        }

        public static final class Handler implements IMessageHandler<State, IMessage> {
            @Override
            public IMessage onMessage(State message, MessageContext ctx) {
                com.exiledradio.trailsandtalesbackport.wolf.client.WolfClient.state(message.entity,
                        WolfVariant.byOrdinal(message.variant), message.armor);
                return null;
            }
        }
    }

    public static final class ArmorBroke implements IMessage {
        public int entity;
        public ItemStack armor = ItemStack.EMPTY;

        public ArmorBroke() {
        }

        ArmorBroke(int entity, ItemStack armor) {
            this.entity = entity;
            this.armor = armor.copy();
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            this.entity = buf.readInt();
            this.armor = ByteBufUtils.readItemStack(buf);
        }

        @Override
        public void toBytes(ByteBuf buf) {
            buf.writeInt(this.entity);
            ByteBufUtils.writeItemStack(buf, this.armor);
        }

        public static final class Handler implements IMessageHandler<ArmorBroke, IMessage> {
            @Override
            public IMessage onMessage(ArmorBroke message, MessageContext ctx) {
                com.exiledradio.trailsandtalesbackport.wolf.client.WolfClient.armorBroke(message.entity, message.armor);
                return null;
            }
        }
    }
}
