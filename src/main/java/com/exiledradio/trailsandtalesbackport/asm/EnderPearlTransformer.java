package com.exiledradio.trailsandtalesbackport.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * P17, 1.20.2's {@code enderPearlsVanishOnDeath}: {@code EntityEnderPearl.onUpdate}'s one
 * {@code thrower.isEntityAlive()} becomes {@code NewGameRules.pearlOwnerAlive(thrower)} (same stack).
 */
public final class EnderPearlTransformer implements IClassTransformer {

    static final String PATCH = "P17";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !"net.minecraft.entity.item.EntityEnderPearl".equals(transformedName)) {
            return bytes;
        }
        ClassNode node = Asm.read(bytes);
        return patch(node) ? Asm.write(node) : bytes;
    }

    public static boolean patch(ClassNode node) {
        MethodNode m = Asm.method(node, "onUpdate", "func_70071_h_", "()V");
        if (m == null) {
            Asm.LOG.warn("{} skipped: EntityEnderPearl.onUpdate not found", PATCH);
            return false;
        }
        MethodInsnNode call = null;
        int calls = 0;
        for (AbstractInsnNode insn : m.instructions.toArray()) {
            if (insn.getOpcode() == Opcodes.INVOKEVIRTUAL
                    && (Asm.isCall(insn, "net/minecraft/entity/EntityLivingBase", "isEntityAlive", "func_70089_S", "()Z")
                    || Asm.isCall(insn, "net/minecraft/entity/Entity", "isEntityAlive", "func_70089_S", "()Z"))) {
                call = (MethodInsnNode) insn;
                calls++;
            }
        }
        if (calls != 1) {
            Asm.LOG.warn("{} skipped: {} isEntityAlive calls, expected 1", PATCH, calls);
            return false;
        }
        m.instructions.set(call, new MethodInsnNode(Opcodes.INVOKESTATIC, "com/exiledradio/trailsandtalesbackport/mechanics/NewGameRules",
                "pearlOwnerAlive", "(Lnet/minecraft/entity/EntityLivingBase;)Z", false));
        Asm.applied(PATCH + " EntityEnderPearl.onUpdate");
        return true;
    }
}
