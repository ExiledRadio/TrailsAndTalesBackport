package com.exiledradio.trailsandtalesbackport.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * P10, 1.20.5's spawn chunks. {@code World.isSpawnChunk(II)Z}'s body becomes
 * {@code return SpawnChunks.isSpawnChunk(this, x, z)} (straight-line code: no frame, no local but the parameters).
 * In {@code MinecraftServer.initialWorldChunkLoad}, the constants {@code 192}, {@code -192} and {@code 625}
 * (each a {@code SIPUSH}) become calls that push the start area's reach, start and chunk count.
 */
public final class SpawnChunkTransformer implements IClassTransformer {

    static final String PATCH = "P10";
    static final String HOOK = "com/exiledradio/trailsandtalesbackport/mechanics/SpawnChunks";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null) {
            return bytes;
        }
        if ("net.minecraft.world.World".equals(transformedName)) {
            ClassNode node = Asm.read(bytes);
            return patchWorld(node) ? Asm.write(node) : bytes;
        }
        if ("net.minecraft.server.MinecraftServer".equals(transformedName)) {
            ClassNode node = Asm.read(bytes);
            return patchServer(node) ? Asm.write(node) : bytes;
        }
        return bytes;
    }

    public static boolean patchWorld(ClassNode node) {
        MethodNode m = Asm.method(node, "isSpawnChunk", "func_72916_c", "(II)Z");
        if (m == null || (m.access & Opcodes.ACC_ABSTRACT) != 0) {
            Asm.LOG.warn("{} skipped: World.isSpawnChunk not found", PATCH);
            return false;
        }
        m.instructions.clear();
        m.tryCatchBlocks.clear();
        if (m.localVariables != null) {
            m.localVariables.clear();
        }
        m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        m.instructions.add(new VarInsnNode(Opcodes.ILOAD, 1));
        m.instructions.add(new VarInsnNode(Opcodes.ILOAD, 2));
        m.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK, "isSpawnChunk", "(Lnet/minecraft/world/World;II)Z", false));
        m.instructions.add(new InsnNode(Opcodes.IRETURN));
        Asm.applied(PATCH + " World.isSpawnChunk");
        return true;
    }

    public static boolean patchServer(ClassNode node) {
        MethodNode m = Asm.method(node, "initialWorldChunkLoad", "func_71222_d", "()V");
        if (m == null) {
            Asm.LOG.warn("{} skipped: MinecraftServer.initialWorldChunkLoad not found", PATCH);
            return false;
        }
        int reach = 0;
        int start = 0;
        for (AbstractInsnNode insn : m.instructions.toArray()) {
            if (insn.getOpcode() == Opcodes.SIPUSH) {
                int v = ((IntInsnNode) insn).operand;
                reach += v == 192 ? 1 : 0;
                start += v == -192 ? 1 : 0;
            }
        }
        if (reach < 2 || start != 2) {
            // Another coremod changed the loops: leave the start area as it is (isSpawnChunk is patched apart).
            Asm.LOG.warn("{} skipped initialWorldChunkLoad: {} x 192 and {} x -192, expected 2 or more and 2", PATCH, reach, start);
            return false;
        }
        for (AbstractInsnNode insn : m.instructions.toArray()) {
            if (insn.getOpcode() != Opcodes.SIPUSH) {
                continue;
            }
            int v = ((IntInsnNode) insn).operand;
            String hook = v == 192 ? "prepareBlocks" : v == -192 ? "prepareStart" : v == 625 ? "prepareCount" : null;
            if (hook != null) {
                m.instructions.set(insn, new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK, hook, "()I", false));
            }
        }
        Asm.applied(PATCH + " MinecraftServer.initialWorldChunkLoad");
        return true;
    }
}
