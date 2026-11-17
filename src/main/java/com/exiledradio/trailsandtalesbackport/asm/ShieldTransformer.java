package com.exiledradio.trailsandtalesbackport.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * P8, 1.20.3's shield facing: {@code EntityLivingBase.canBlockDamageSource}'s one {@code this.getLook(1.0F)} becomes
 * {@code MechanicsHooks.shieldLook(this, 1.0F)} (same stack: the entity and a float in, a vector out).
 */
public final class ShieldTransformer implements IClassTransformer {

    static final String PATCH = "P8";
    static final String VEC = "Lnet/minecraft/util/math/Vec3d;";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !"net.minecraft.entity.EntityLivingBase".equals(transformedName)) {
            return bytes;
        }
        ClassNode node = Asm.read(bytes);
        return patch(node) ? Asm.write(node) : bytes;
    }

    public static boolean patch(ClassNode node) {
        MethodNode m = Asm.method(node, "canBlockDamageSource", "func_184583_d", "(Lnet/minecraft/util/DamageSource;)Z");
        if (m == null) {
            Asm.LOG.warn("{} skipped: EntityLivingBase.canBlockDamageSource not found", PATCH);
            return false;
        }
        MethodInsnNode call = null;
        int calls = 0;
        for (AbstractInsnNode insn : m.instructions.toArray()) {
            if (insn.getOpcode() == Opcodes.INVOKEVIRTUAL
                    && (Asm.isCall(insn, "net/minecraft/entity/EntityLivingBase", "getLook", "func_70676_i", "(F)" + VEC)
                    || Asm.isCall(insn, "net/minecraft/entity/Entity", "getLook", "func_70676_i", "(F)" + VEC))) {
                call = (MethodInsnNode) insn;
                calls++;
            }
        }
        if (calls != 1) {
            Asm.LOG.warn("{} skipped: {} getLook calls, expected 1", PATCH, calls);
            return false;
        }
        m.instructions.set(call, new MethodInsnNode(Opcodes.INVOKESTATIC, "com/exiledradio/trailsandtalesbackport/mechanics/MechanicsHooks",
                "shieldLook", "(Lnet/minecraft/entity/EntityLivingBase;F)" + VEC, false));
        Asm.applied(PATCH + " EntityLivingBase.canBlockDamageSource");
        return true;
    }
}
