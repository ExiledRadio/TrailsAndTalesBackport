package com.exiledradio.trailsandtalesbackport.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * P14, 1.20.5's hoppers under full blocks: {@code TileEntityHopper.pullItems}'s one
 * {@code getCaptureItems(world, x, y, z)} becomes {@code SmallTweaks.captureItems(world, x, y, z)} (same stack).
 */
public final class HopperTransformer implements IClassTransformer {

    static final String PATCH = "P14";
    static final String DESC = "(Lnet/minecraft/world/World;DDD)Ljava/util/List;";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !"net.minecraft.tileentity.TileEntityHopper".equals(transformedName)) {
            return bytes;
        }
        ClassNode node = Asm.read(bytes);
        return patch(node) ? Asm.write(node) : bytes;
    }

    public static boolean patch(ClassNode node) {
        MethodNode m = Asm.method(node, "pullItems", "func_145891_a", "(Lnet/minecraft/tileentity/IHopper;)Z");
        if (m == null) {
            Asm.LOG.warn("{} skipped: TileEntityHopper.pullItems not found", PATCH);
            return false;
        }
        MethodInsnNode call = null;
        int calls = 0;
        for (AbstractInsnNode insn : m.instructions.toArray()) {
            if (insn.getOpcode() == Opcodes.INVOKESTATIC
                    && Asm.isCall(insn, "net/minecraft/tileentity/TileEntityHopper", "getCaptureItems", "func_184292_a", DESC)) {
                call = (MethodInsnNode) insn;
                calls++;
            }
        }
        if (calls != 1) {
            Asm.LOG.warn("{} skipped: {} getCaptureItems calls, expected 1", PATCH, calls);
            return false;
        }
        m.instructions.set(call, new MethodInsnNode(Opcodes.INVOKESTATIC,
                "com/exiledradio/trailsandtalesbackport/mechanics/SmallTweaks", "captureItems", DESC, false));
        Asm.applied(PATCH + " TileEntityHopper.pullItems");
        return true;
    }
}
