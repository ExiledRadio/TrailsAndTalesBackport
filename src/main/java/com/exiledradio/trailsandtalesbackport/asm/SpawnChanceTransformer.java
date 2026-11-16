package com.exiledradio.trailsandtalesbackport.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * P6, 1.20.5's creature spawn chance at chunk generation in the badlands: {@code WorldEntitySpawner.performWorldGenSpawning}'s
 * one {@code biome.getSpawningChance()} becomes {@code WolfHooks.spawningChance(biome)} (same stack: a biome in, a float
 * out), which answers 0.03 for the badlands, 0.04 for the wooded badlands and the biome's own value elsewhere.
 */
public final class SpawnChanceTransformer implements IClassTransformer {

    static final String PATCH = "P6";
    static final String BIOME = "net/minecraft/world/biome/Biome";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !"net.minecraft.world.WorldEntitySpawner".equals(transformedName)) {
            return bytes;
        }
        ClassNode node = Asm.read(bytes);
        return patch(node) ? Asm.write(node) : bytes;
    }

    public static boolean patch(ClassNode node) {
        MethodNode m = Asm.method(node, "performWorldGenSpawning", "func_77191_a",
                "(Lnet/minecraft/world/World;L" + BIOME + ";IIIILjava/util/Random;)V");
        if (m == null) {
            Asm.LOG.warn("{} skipped: WorldEntitySpawner.performWorldGenSpawning not found", PATCH);
            return false;
        }
        MethodInsnNode call = null;
        int calls = 0;
        for (AbstractInsnNode insn : m.instructions.toArray()) {
            if (insn.getOpcode() == Opcodes.INVOKEVIRTUAL && Asm.isCall(insn, BIOME, "getSpawningChance", "func_76741_f", "()F")) {
                call = (MethodInsnNode) insn;
                calls++;
            }
        }
        if (calls != 1) {
            Asm.LOG.warn("{} skipped: {} getSpawningChance calls, expected 1", PATCH, calls);
            return false;
        }
        m.instructions.set(call, new MethodInsnNode(Opcodes.INVOKESTATIC, "com/exiledradio/trailsandtalesbackport/wolf/WolfHooks",
                "spawningChance", "(L" + BIOME + ";)F", false));
        Asm.applied(PATCH + " WorldEntitySpawner.performWorldGenSpawning");
        return true;
    }
}
