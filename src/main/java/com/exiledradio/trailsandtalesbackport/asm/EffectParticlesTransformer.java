package com.exiledradio.trailsandtalesbackport.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Patch P3 (docs/PATCHES.md): 1.20.5's effect particles, one per effect in its own color.
 *
 * <p>{@code EntityLivingBase.updatePotionEffects} reads the blended color, {@code int i =
 * dataManager.get(POTION_EFFECTS)}, and draws a swirl while it is above 0. That read becomes
 * <pre>
 * int i = EffectParticles.effectColor(dataManager.get(POTION_EFFECTS), this);
 * </pre>
 * The hook answers the color unchanged on the server, while the feature is off, and for an entity the server sent no
 * list for; otherwise it draws 1.20.6's particle itself and answers 0. Two instructions on the stack after the
 * {@code intValue}: no jump, no new local, no frame changed ({@code this} is read where vanilla reads it next).
 */
public final class EffectParticlesTransformer implements IClassTransformer {

    static final String PATCH = "P3";
    private static final String TARGET = "net.minecraft.entity.EntityLivingBase";
    private static final String ELB = "net/minecraft/entity/EntityLivingBase";
    private static final String HOOKS = "com/exiledradio/trailsandtalesbackport/effectparticles/EffectParticles";

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (!TARGET.equals(transformedName) || basicClass == null) {
            return basicClass;
        }
        ClassNode node = Asm.read(basicClass);
        MethodNode m = Asm.method(node, "updatePotionEffects", "func_70679_bo", "()V");
        AbstractInsnNode read = m == null ? null : colorRead(m);
        if (read == null) {
            Asm.LOG.warn("{} effect particles: {}.updatePotionEffects has no read of POTION_EFFECTS of the expected shape; patch skipped.",
                    PATCH, TARGET);
            return basicClass;
        }
        InsnList hook = new InsnList();
        hook.add(new VarInsnNode(Opcodes.ALOAD, 0));
        hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "effectColor", "(IL" + ELB + ";)I", false));
        m.instructions.insert(read, hook);
        Asm.applied(PATCH);
        return Asm.write(node);
    }

    /**
     * @return the {@code Integer.intValue()} of {@code dataManager.get(POTION_EFFECTS)}: GETSTATIC, the
     * {@code EntityDataManager.get} call, a CHECKCAST to Integer and the unboxing, in that order
     */
    static AbstractInsnNode colorRead(MethodNode m) {
        for (AbstractInsnNode insn = m.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn.getOpcode() != Opcodes.GETSTATIC) {
                continue;
            }
            FieldInsnNode field = (FieldInsnNode) insn;
            if (!ELB.equals(field.owner) || !("POTION_EFFECTS".equals(field.name) || "field_184633_f".equals(field.name))) {
                continue;
            }
            AbstractInsnNode get = next(insn);
            AbstractInsnNode cast = next(get);
            AbstractInsnNode unbox = next(cast);
            if (Asm.isCall(get, "net/minecraft/network/datasync/EntityDataManager", "get", "func_187225_a",
                    "(Lnet/minecraft/network/datasync/DataParameter;)Ljava/lang/Object;")
                    && cast != null && cast.getOpcode() == Opcodes.CHECKCAST
                    && Asm.isCall(unbox, "java/lang/Integer", "intValue", "intValue", "()I")) {
                return unbox;
            }
            return null;
        }
        return null;
    }

    /** @return the next real instruction (labels, line numbers and frames skipped), or null */
    private static AbstractInsnNode next(AbstractInsnNode insn) {
        AbstractInsnNode n = insn == null ? null : insn.getNext();
        while (n != null && n.getOpcode() < 0) {
            n = n.getNext();
        }
        return n;
    }
}
