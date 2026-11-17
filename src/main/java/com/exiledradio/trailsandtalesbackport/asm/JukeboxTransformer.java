package com.exiledradio.trailsandtalesbackport.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * P7, Relic's comparator signal (stream St): every {@code IRETURN} of {@code BlockJukebox.getComparatorInputOverride}
 * first passes its value through {@code JukeboxHooks.signal(I)I}.
 *
 * <p>1.12.2 counts the signal from the disc's item id ({@code id + 1 - id(record_13)}), so the value it returns for
 * Relic is Relic's id offset; the hook turns exactly that value into 1.20's 14 and hands every other value back. It reads
 * no local and adds no branch, so no stack-map frame changes, and it stacks with Nether Update's P2 (a head that answers
 * for its own discs) in either order: P2's own return of 13 for Pigstep passes through unchanged.
 */
public final class JukeboxTransformer implements IClassTransformer {

    static final String PATCH = "P7";
    static final String HOOKS = "com/exiledradio/trailsandtalesbackport/structure/JukeboxHooks";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !"net.minecraft.block.BlockJukebox".equals(transformedName)) {
            return bytes;
        }
        ClassNode node = Asm.read(bytes);
        return patch(node) ? Asm.write(node) : bytes;
    }

    public static boolean patch(ClassNode node) {
        MethodNode m = Asm.method(node, "getComparatorInputOverride", "func_180641_l",
                "(Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;)I");
        if (m == null || (m.access & Opcodes.ACC_STATIC) != 0) {
            Asm.LOG.warn("{} skipped: BlockJukebox.getComparatorInputOverride not found; Relic gives 1.12.2's id-offset signal", PATCH);
            return false;
        }
        int returns = 0;
        for (AbstractInsnNode insn : m.instructions.toArray()) {
            if (insn.getOpcode() == Opcodes.IRETURN) {
                m.instructions.insertBefore(insn, new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "signal", "(I)I", false));
                returns++;
            }
        }
        if (returns == 0) {
            Asm.LOG.warn("{} skipped: BlockJukebox.getComparatorInputOverride has no int return", PATCH);
            return false;
        }
        Asm.applied(PATCH + " BlockJukebox.getComparatorInputOverride");
        return true;
    }
}
