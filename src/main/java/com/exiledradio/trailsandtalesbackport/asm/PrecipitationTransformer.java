package com.exiledradio.trailsandtalesbackport.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * P19, 1.20.2's {@code randomTickSpeed} for snow and ice. In {@code WorldServer.updateBlocks}, the
 * {@code this.rand.nextInt(16)} right after {@code this.provider.canDoRainSnowIce(chunk)} becomes
 * {@code NewGameRules.precipitationRoll(this.rand, 16, this)} ({@code ALOAD 0} pushed before the call: no jump, no
 * local).
 */
public final class PrecipitationTransformer implements IClassTransformer {

    static final String PATCH = "P19";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !"net.minecraft.world.WorldServer".equals(transformedName)) {
            return bytes;
        }
        ClassNode node = Asm.read(bytes);
        return patch(node) ? Asm.write(node) : bytes;
    }

    private static AbstractInsnNode next(AbstractInsnNode insn) {
        AbstractInsnNode n = insn.getNext();
        while (n != null && n.getOpcode() < 0) {
            n = n.getNext();
        }
        return n;
    }

    public static boolean patch(ClassNode node) {
        MethodNode m = Asm.method(node, "updateBlocks", "func_147456_g", "()V");
        if (m == null) {
            Asm.LOG.warn("{} skipped: WorldServer.updateBlocks not found", PATCH);
            return false;
        }
        MethodInsnNode roll = null;
        int found = 0;
        for (AbstractInsnNode insn : m.instructions.toArray()) {
            if (!Asm.isCall(insn, "net/minecraft/world/WorldProvider", "canDoRainSnowIce", "canDoRainSnowIce",
                    "(Lnet/minecraft/world/chunk/Chunk;)Z")) {
                continue;
            }
            // IFEQ, ALOAD 0, GETFIELD rand, BIPUSH 16, INVOKEVIRTUAL Random.nextInt(I)I
            AbstractInsnNode jump = next(insn);
            AbstractInsnNode self = jump == null ? null : next(jump);
            AbstractInsnNode rand = self == null ? null : next(self);
            AbstractInsnNode sixteen = rand == null ? null : next(rand);
            AbstractInsnNode call = sixteen == null ? null : next(sixteen);
            if (jump != null && jump.getOpcode() == Opcodes.IFEQ && self.getOpcode() == Opcodes.ALOAD && rand.getOpcode() == Opcodes.GETFIELD
                    && sixteen.getOpcode() == Opcodes.BIPUSH && ((IntInsnNode) sixteen).operand == 16
                    && Asm.isCall(call, "java/util/Random", "nextInt", "nextInt", "(I)I")) {
                roll = (MethodInsnNode) call;
                found++;
            }
        }
        if (found != 1) {
            Asm.LOG.warn("{} skipped: {} canDoRainSnowIce && rand.nextInt(16) found, expected 1", PATCH, found);
            return false;
        }
        InsnList hook = new InsnList();
        hook.add(new VarInsnNode(Opcodes.ALOAD, 0));
        hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "com/exiledradio/trailsandtalesbackport/mechanics/NewGameRules",
                "precipitationRoll", "(Ljava/util/Random;ILnet/minecraft/world/World;)I", false));
        m.instructions.insertBefore(roll, hook);
        m.instructions.remove(roll);
        Asm.applied(PATCH + " WorldServer.updateBlocks");
        return true;
    }
}
