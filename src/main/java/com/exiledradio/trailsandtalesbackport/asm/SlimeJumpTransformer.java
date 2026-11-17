package com.exiledradio.trailsandtalesbackport.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * P11, Jump Boost on slimes and magma cubes (1.20): the one {@code this.motionY = v} in {@code EntitySlime.jump} and
 * {@code EntityMagmaCube.jump} becomes {@code MechanicsHooks.slimeJump(this, v)} (same stack: the entity and a double
 * consumed).
 */
public final class SlimeJumpTransformer implements IClassTransformer {

    static final String PATCH = "P11";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !("net.minecraft.entity.monster.EntitySlime".equals(transformedName)
                || "net.minecraft.entity.monster.EntityMagmaCube".equals(transformedName))) {
            return bytes;
        }
        ClassNode node = Asm.read(bytes);
        return patch(node) ? Asm.write(node) : bytes;
    }

    public static boolean patch(ClassNode node) {
        String cls = node.name.substring(node.name.lastIndexOf('/') + 1);
        MethodNode m = Asm.method(node, "jump", "func_70664_aZ", "()V");
        if (m == null) {
            Asm.LOG.warn("{} skipped: {}.jump not found", PATCH, cls);
            return false;
        }
        FieldInsnNode put = null;
        int puts = 0;
        for (AbstractInsnNode insn : m.instructions.toArray()) {
            if (insn.getOpcode() == Opcodes.PUTFIELD && "D".equals(((FieldInsnNode) insn).desc)
                    && ("motionY".equals(((FieldInsnNode) insn).name) || "field_70181_x".equals(((FieldInsnNode) insn).name))) {
                put = (FieldInsnNode) insn;
                puts++;
            }
        }
        if (puts != 1) {
            Asm.LOG.warn("{} skipped: {} motionY writes in {}.jump, expected 1", PATCH, puts, cls);
            return false;
        }
        m.instructions.set(put, new MethodInsnNode(Opcodes.INVOKESTATIC, "com/exiledradio/trailsandtalesbackport/mechanics/MechanicsHooks",
                "slimeJump", "(Lnet/minecraft/entity/Entity;D)V", false));
        Asm.applied(PATCH + " " + cls + ".jump");
        return true;
    }
}
