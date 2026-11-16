package com.exiledradio.trailsandtalesbackport.effectparticles;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

/**
 * Packet 90 (block 90-94, effect particles): one entity's effect particles, 1.20.6's {@code DATA_EFFECT_PARTICLES}
 * and {@code DATA_EFFECT_AMBIENCE_ID}. Server to client. The handler runs on the client thread through {@code
 * effectparticles.client.EffectParticlesClient}, named only inside a method body.
 */
public final class EffectParticlesMessage implements IMessage {

    public static final int ID = 90;

    /** More than any game has; a longer list is cut here. */
    static final int MAX = 255;

    public int entity;
    public EffectParticles.Synced synced;

    public EffectParticlesMessage() {
    }

    public EffectParticlesMessage(int entity, EffectParticles.Synced synced) {
        this.entity = entity;
        this.synced = synced;
    }

    public static void register(SimpleNetworkWrapper channel) {
        channel.registerMessage(Handler.class, EffectParticlesMessage.class, ID, Side.CLIENT);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.entity = buf.readInt();
        boolean ambient = buf.readBoolean();
        int[] colors = new int[buf.readUnsignedByte()];
        for (int i = 0; i < colors.length; i++) {
            colors[i] = buf.readInt();
        }
        this.synced = new EffectParticles.Synced(colors, ambient);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.entity);
        buf.writeBoolean(this.synced.ambient);
        int n = Math.min(this.synced.colors.length, MAX);
        buf.writeByte(n);
        for (int i = 0; i < n; i++) {
            buf.writeInt(this.synced.colors[i]);
        }
    }

    public static final class Handler implements IMessageHandler<EffectParticlesMessage, IMessage> {
        @Override
        public IMessage onMessage(EffectParticlesMessage message, MessageContext ctx) {
            com.exiledradio.trailsandtalesbackport.effectparticles.client.EffectParticlesClient.schedule(message.entity, message.synced);
            return null;
        }
    }
}
