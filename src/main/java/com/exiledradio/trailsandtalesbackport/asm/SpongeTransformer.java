package com.exiledradio.trailsandtalesbackport.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * P15, 1.20.2's sponge sounds. Adds Forge's {@code getSoundType(IBlockState, World, BlockPos, Entity)} to
 * {@code BlockSponge} as {@code return BlockSounds.spongeSound(this, state)} (1.12.2's sponge has one sound type for
 * both states; a block of its own is the only way a state picks one), skipped if the class already has it; and in
 * {@code BlockSponge.tryAbsorb} the one {@code World.playEvent(int, BlockPos, int)} becomes
 * {@code BlockSounds.absorbed(world, type, pos, data)} (same stack), which also plays the absorb sound.
 */
public final class SpongeTransformer implements IClassTransformer {

    static final String PATCH = "P15";
    static final String HOOK = "com/exiledradio/trailsandtalesbackport/mechanics/BlockSounds";
    static final String SOUND_DESC = "(Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/world/World;"
            + "Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/Entity;)Lnet/minecraft/block/SoundType;";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !"net.minecraft.block.BlockSponge".equals(transformedName)) {
            return bytes;
        }
        ClassNode node = Asm.read(bytes);
        return patch(node) ? Asm.write(node) : bytes;
    }

    public static boolean patch(ClassNode node) {
        boolean changed = false;
        if (Asm.method(node, "getSoundType", "getSoundType", SOUND_DESC) != null) {
            Asm.LOG.warn("{}: BlockSponge already has getSoundType (another coremod); wet sponges keep its sounds", PATCH);
        } else {
            MethodNode m = new MethodNode(Opcodes.ACC_PUBLIC, "getSoundType", SOUND_DESC, null, null);
            InsnList in = m.instructions;
            in.add(new VarInsnNode(Opcodes.ALOAD, 0));
            in.add(new VarInsnNode(Opcodes.ALOAD, 1));
            in.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK, "spongeSound",
                    "(Lnet/minecraft/block/Block;Lnet/minecraft/block/state/IBlockState;)Lnet/minecraft/block/SoundType;", false));
            in.add(new InsnNode(Opcodes.ARETURN));
            m.maxStack = 2;
            m.maxLocals = 5;
            node.methods.add(m);
            Asm.applied(PATCH + " BlockSponge.getSoundType");
            changed = true;
        }
        MethodNode absorb = Asm.method(node, "tryAbsorb", "func_176311_e",
                "(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;)V");
        MethodInsnNode call = null;
        int calls = 0;
        if (absorb != null) {
            for (AbstractInsnNode insn : absorb.instructions.toArray()) {
                if (insn.getOpcode() == Opcodes.INVOKEVIRTUAL && Asm.isCall(insn, "net/minecraft/world/World", "playEvent", "func_175718_b",
                        "(ILnet/minecraft/util/math/BlockPos;I)V")) {
                    call = (MethodInsnNode) insn;
                    calls++;
                }
            }
        }
        if (calls != 1) {
            Asm.LOG.warn("{} skipped tryAbsorb: {} playEvent calls, expected 1", PATCH, calls);
        } else {
            absorb.instructions.set(call, new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK, "absorbed",
                    "(Lnet/minecraft/world/World;ILnet/minecraft/util/math/BlockPos;I)V", false));
            Asm.applied(PATCH + " BlockSponge.tryAbsorb");
            changed = true;
        }
        return changed;
    }
}
