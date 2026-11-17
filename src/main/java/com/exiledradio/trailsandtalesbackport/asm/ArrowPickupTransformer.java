package com.exiledradio.trailsandtalesbackport.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * P12, named arrows (1.20.3): each {@code this.getArrowStack()} in {@code EntityArrow.onCollideWithPlayer} is followed by
 * {@code ALOAD 0; MechanicsHooks.arrowPickup(stack, this)} (a stack in, a stack out), which gives back the named item
 * the arrow was shot from.
 */
public final class ArrowPickupTransformer implements IClassTransformer {

    static final String PATCH = "P12";
    static final String STACK = "Lnet/minecraft/item/ItemStack;";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !"net.minecraft.entity.projectile.EntityArrow".equals(transformedName)) {
            return bytes;
        }
        ClassNode node = Asm.read(bytes);
        return patch(node) ? Asm.write(node) : bytes;
    }

    public static boolean patch(ClassNode node) {
        MethodNode m = Asm.method(node, "onCollideWithPlayer", "func_70100_b_", "(Lnet/minecraft/entity/player/EntityPlayer;)V");
        if (m == null) {
            Asm.LOG.warn("{} skipped: EntityArrow.onCollideWithPlayer not found", PATCH);
            return false;
        }
        int calls = 0;
        for (AbstractInsnNode insn : m.instructions.toArray()) {
            if (insn.getOpcode() == Opcodes.INVOKEVIRTUAL
                    && Asm.isCall(insn, "net/minecraft/entity/projectile/EntityArrow", "getArrowStack", "func_184550_j", "()" + STACK)) {
                InsnList after = new InsnList();
                after.add(new VarInsnNode(Opcodes.ALOAD, 0));
                after.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "com/exiledradio/trailsandtalesbackport/mechanics/MechanicsHooks",
                        "arrowPickup", "(" + STACK + "Lnet/minecraft/entity/projectile/EntityArrow;)" + STACK, false));
                m.instructions.insert(insn, after);
                calls++;
            }
        }
        if (calls == 0) {
            Asm.LOG.warn("{} skipped: no getArrowStack call in EntityArrow.onCollideWithPlayer", PATCH);
            return false;
        }
        Asm.applied(PATCH + " EntityArrow.onCollideWithPlayer");
        return true;
    }
}
