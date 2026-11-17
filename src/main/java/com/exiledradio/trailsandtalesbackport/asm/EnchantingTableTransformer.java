package com.exiledradio.trailsandtalesbackport.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * P13, 1.20's enchanting table. In {@code ContainerEnchantment.onCraftMatrixChanged}, just before
 * {@code this.rand.setSeed(this.xpSeed)}, the bookshelf power local becomes
 * {@code EnchantingTables.power(power, this.world, this.position)} (load, call, store into the same local: no new
 * local, no jump). In {@code BlockEnchantmentTable.randomDisplayTick} (client) the one {@code World.isAirBlock} call
 * becomes {@code EnchantingTables.particlesPass(world, pos)} (same stack).
 */
public final class EnchantingTableTransformer implements IClassTransformer {

    static final String PATCH = "P13";
    static final String HOOK = "com/exiledradio/trailsandtalesbackport/mechanics/EnchantingTables";
    static final String WORLD = "Lnet/minecraft/world/World;";
    static final String POS = "Lnet/minecraft/util/math/BlockPos;";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null) {
            return bytes;
        }
        if ("net.minecraft.inventory.ContainerEnchantment".equals(transformedName)) {
            ClassNode node = Asm.read(bytes);
            return patchContainer(node) ? Asm.write(node) : bytes;
        }
        if ("net.minecraft.block.BlockEnchantmentTable".equals(transformedName)) {
            ClassNode node = Asm.read(bytes);
            return patchBlock(node) ? Asm.write(node) : bytes;
        }
        return bytes;
    }

    private static String field(ClassNode node, String desc) {
        String found = null;
        for (FieldNode f : node.fields) {
            if (desc.equals(f.desc) && (f.access & Opcodes.ACC_STATIC) == 0) {
                if (found != null) {
                    return null;
                }
                found = f.name;
            }
        }
        return found;
    }

    private static AbstractInsnNode realPrevious(AbstractInsnNode insn) {
        AbstractInsnNode p = insn.getPrevious();
        while (p != null && p.getOpcode() < 0) {
            p = p.getPrevious();
        }
        return p;
    }

    public static boolean patchContainer(ClassNode node) {
        MethodNode m = Asm.method(node, "onCraftMatrixChanged", "func_75130_a", "(Lnet/minecraft/inventory/IInventory;)V");
        String world = field(node, WORLD);
        String position = field(node, POS);
        if (m == null || world == null || position == null) {
            Asm.LOG.warn("{} skipped: ContainerEnchantment.onCraftMatrixChanged, its world or its position not found", PATCH);
            return false;
        }
        // The bookshelf power: the first float local set to 0.
        int power = -1;
        for (AbstractInsnNode insn : m.instructions.toArray()) {
            if (insn.getOpcode() == Opcodes.FSTORE && realPrevious(insn) != null && realPrevious(insn).getOpcode() == Opcodes.FCONST_0) {
                power = ((VarInsnNode) insn).var;
                break;
            }
        }
        MethodInsnNode seed = null;
        int seeds = 0;
        for (AbstractInsnNode insn : m.instructions.toArray()) {
            if (Asm.isCall(insn, "java/util/Random", "setSeed", "setSeed", "(J)V")) {
                if (seed == null) {
                    seed = (MethodInsnNode) insn;
                }
                seeds++;
            }
        }
        if (power < 0 || seed == null) {
            Asm.LOG.warn("{} skipped: power local {} or setSeed ({} calls) not found", PATCH, power, seeds);
            return false;
        }
        // this.rand.setSeed((long) this.xpSeed): ALOAD 0, GETFIELD rand, ALOAD 0, GETFIELD xpSeed, I2L, INVOKEVIRTUAL.
        AbstractInsnNode i2l = realPrevious(seed);
        AbstractInsnNode xp = i2l == null ? null : realPrevious(i2l);
        AbstractInsnNode this2 = xp == null ? null : realPrevious(xp);
        AbstractInsnNode rand = this2 == null ? null : realPrevious(this2);
        AbstractInsnNode start = rand == null ? null : realPrevious(rand);
        if (i2l == null || i2l.getOpcode() != Opcodes.I2L || xp.getOpcode() != Opcodes.GETFIELD || this2.getOpcode() != Opcodes.ALOAD
                || rand.getOpcode() != Opcodes.GETFIELD || !"Ljava/util/Random;".equals(((FieldInsnNode) rand).desc)
                || start.getOpcode() != Opcodes.ALOAD || ((VarInsnNode) start).var != 0) {
            Asm.LOG.warn("{} skipped: this.rand.setSeed(this.xpSeed) has another shape", PATCH);
            return false;
        }
        InsnList hook = new InsnList();
        hook.add(new VarInsnNode(Opcodes.FLOAD, power));
        hook.add(new VarInsnNode(Opcodes.ALOAD, 0));
        hook.add(new FieldInsnNode(Opcodes.GETFIELD, node.name, world, WORLD));
        hook.add(new VarInsnNode(Opcodes.ALOAD, 0));
        hook.add(new FieldInsnNode(Opcodes.GETFIELD, node.name, position, POS));
        hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK, "power", "(F" + WORLD + POS + ")F", false));
        hook.add(new VarInsnNode(Opcodes.FSTORE, power));
        m.instructions.insertBefore(start, hook);
        Asm.applied(PATCH + " ContainerEnchantment.onCraftMatrixChanged");
        return true;
    }

    public static boolean patchBlock(ClassNode node) {
        MethodNode m = Asm.method(node, "randomDisplayTick", "func_180655_c",
                "(Lnet/minecraft/block/state/IBlockState;" + WORLD + POS + "Ljava/util/Random;)V");
        if (m == null) {
            // Client-only: a dedicated server's class has no randomDisplayTick.
            Asm.LOG.info("{}: BlockEnchantmentTable.randomDisplayTick not here (a dedicated server), particles not patched", PATCH);
            return false;
        }
        MethodInsnNode call = null;
        int calls = 0;
        for (AbstractInsnNode insn : m.instructions.toArray()) {
            if (insn.getOpcode() == Opcodes.INVOKEVIRTUAL && Asm.isCall(insn, "net/minecraft/world/World", "isAirBlock", "func_175623_d", "(" + POS + ")Z")) {
                call = (MethodInsnNode) insn;
                calls++;
            }
        }
        if (calls != 1) {
            Asm.LOG.warn("{} skipped: {} isAirBlock calls in randomDisplayTick, expected 1", PATCH, calls);
            return false;
        }
        m.instructions.set(call, new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK, "particlesPass", "(" + WORLD + POS + ")Z", false));
        Asm.applied(PATCH + " BlockEnchantmentTable.randomDisplayTick");
        return true;
    }
}
