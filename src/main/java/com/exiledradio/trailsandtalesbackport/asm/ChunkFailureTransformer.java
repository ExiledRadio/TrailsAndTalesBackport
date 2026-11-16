package com.exiledradio.trailsandtalesbackport.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * P20, 1.20.5's chunk toasts: the first line of {@code ChunkProviderServer.saveChunkData(chunk)} calls {@code
 * ChunkFailures.saving(chunk)}, and the first line of {@code loadChunkFromFile(x, z)} calls {@code
 * ChunkFailures.loading(x, z)}, so the log lines those methods' catch blocks write ("Failed to save chunk", "Couldn't
 * save chunk", "Couldn't load chunk", none naming the chunk) can be matched to their chunk. The hooks only remember
 * the position; nothing else changes. {@code ChunkProviderServer} is one of the classes Forge replaces, so its frames
 * are javac's; the inserted code reads only parameters, before anything else runs.
 */
public final class ChunkFailureTransformer implements IClassTransformer {

    static final String PATCH = "P20";
    static final String HOOKS = "com/exiledradio/trailsandtalesbackport/presentation1205/ChunkFailures";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !"net.minecraft.world.gen.ChunkProviderServer".equals(transformedName)) {
            return bytes;
        }
        ClassNode node = Asm.read(bytes);
        return patch(node) ? Asm.write(node) : bytes;
    }

    public static boolean patch(ClassNode node) {
        MethodNode save = Asm.method(node, "saveChunkData", "func_73242_b", "(Lnet/minecraft/world/chunk/Chunk;)V");
        MethodNode load = Asm.method(node, "loadChunkFromFile", "func_73239_e", "(II)Lnet/minecraft/world/chunk/Chunk;");
        if (save == null || load == null || (save.access & Opcodes.ACC_STATIC) != 0 || (load.access & Opcodes.ACC_STATIC) != 0) {
            Asm.LOG.warn("{} skipped: ChunkProviderServer.saveChunkData / loadChunkFromFile not found as expected; "
                    + "chunk toasts only for failures whose chunk the log names", PATCH);
            return false;
        }
        InsnList s = new InsnList();
        s.add(new VarInsnNode(Opcodes.ALOAD, 1));
        s.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "saving", "(Lnet/minecraft/world/chunk/Chunk;)V", false));
        save.instructions.insert(s);
        InsnList l = new InsnList();
        l.add(new VarInsnNode(Opcodes.ILOAD, 1));
        l.add(new VarInsnNode(Opcodes.ILOAD, 2));
        l.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "loading", "(II)V", false));
        load.instructions.insert(l);
        Asm.applied(PATCH + " ChunkProviderServer.saveChunkData");
        Asm.applied(PATCH + " ChunkProviderServer.loadChunkFromFile");
        return true;
    }
}
