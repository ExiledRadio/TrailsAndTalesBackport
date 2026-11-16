package com.exiledradio.trailsandtalesbackport.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * P2, 1.20's step sounds for players: {@code Entity.move}'s one call {@code this.playStepSound(blockpos, block)}
 * becomes {@code StepSounds.playStepSound(this, blockpos, block)} (same arguments on the stack, nothing else moves).
 * The hook plays 1.20.6's {@code Player.playStepSound} for players and calls the entity's own method for every other
 * entity, so overrides keep working.
 */
public final class StepSoundTransformer implements IClassTransformer {

    static final String PATCH = "P2";
    static final String ENTITY = "net/minecraft/entity/Entity";
    static final String DESC = "(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/Block;)V";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !"net.minecraft.entity.Entity".equals(transformedName)) {
            return bytes;
        }
        ClassNode node = Asm.read(bytes);
        return patch(node) ? Asm.write(node) : bytes;
    }

    public static boolean patch(ClassNode node) {
        MethodNode move = Asm.method(node, "move", "func_70091_d", "(Lnet/minecraft/entity/MoverType;DDD)V");
        if (move == null) {
            Asm.LOG.warn("{} skipped: Entity.move not found; step sounds stay 1.12.2's", PATCH);
            return false;
        }
        MethodInsnNode call = null;
        int calls = 0;
        for (AbstractInsnNode insn : move.instructions.toArray()) {
            if (insn.getOpcode() == Opcodes.INVOKEVIRTUAL && Asm.isCall(insn, ENTITY, "playStepSound", "func_180429_a", DESC)) {
                call = (MethodInsnNode) insn;
                calls++;
            }
        }
        if (calls != 1) {
            Asm.LOG.warn("{} skipped: Entity.move has {} playStepSound calls, expected 1; step sounds stay 1.12.2's", PATCH, calls);
            return false;
        }
        move.instructions.set(call, new MethodInsnNode(Opcodes.INVOKESTATIC,
                "com/exiledradio/trailsandtalesbackport/misc/StepSounds", "playStepSound",
                "(L" + ENTITY + ";Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/Block;)V", false));
        Asm.applied(PATCH + " Entity.move playStepSound");
        return true;
    }
}
