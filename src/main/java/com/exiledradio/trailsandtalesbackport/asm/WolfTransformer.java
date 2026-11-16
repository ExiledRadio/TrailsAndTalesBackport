package com.exiledradio.trailsandtalesbackport.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * P5, 1.20.5's wolf variant in spawn packs and in the entity's own tag ({@code wolf.WolfHooks}):
 * <ul>
 *   <li>{@code EntityWolf.readEntityFromNBT} / {@code writeEntityToNBT}: first line, {@code WolfHooks.readNbt(this, tag)}
 *       / {@code writeNbt(this, tag)};</li>
 *   <li>adds {@code onInitialSpawn} (1.12.2's {@code EntityWolf} has none): {@code return
 *       WolfHooks.initialSpawn(this, super.onInitialSpawn(difficulty, data));}. Named as the class names its own methods
 *       (MCP in dev, SRG in a real instance); skipped if the class already has one (another coremod added it).</li>
 * </ul>
 * No jump anywhere; parameters are read on the first line only.
 */
public final class WolfTransformer implements IClassTransformer {

    static final String PATCH = "P5";
    static final String WOLF = "net/minecraft/entity/passive/EntityWolf";
    static final String HOOKS = "com/exiledradio/trailsandtalesbackport/wolf/WolfHooks";
    static final String NBT = "Lnet/minecraft/nbt/NBTTagCompound;";
    static final String SPAWN_DESC = "(Lnet/minecraft/world/DifficultyInstance;Lnet/minecraft/entity/IEntityLivingData;)Lnet/minecraft/entity/IEntityLivingData;";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !"net.minecraft.entity.passive.EntityWolf".equals(transformedName)) {
            return bytes;
        }
        ClassNode node = Asm.read(bytes);
        return patch(node) ? Asm.write(node) : bytes;
    }

    public static boolean patch(ClassNode node) {
        MethodNode read = Asm.method(node, "readEntityFromNBT", "func_70037_a", "(" + NBT + ")V");
        MethodNode write = Asm.method(node, "writeEntityToNBT", "func_70014_b", "(" + NBT + ")V");
        if (read == null || write == null) {
            Asm.LOG.warn("{} skipped: EntityWolf's NBT methods not found; wolves keep 1.12.2's spawn and tag", PATCH);
            return false;
        }
        head(read, "readNbt");
        head(write, "writeNbt");
        boolean mcp = "readEntityFromNBT".equals(read.name);
        String spawnName = mcp ? "onInitialSpawn" : "func_180482_a";
        if (Asm.method(node, "onInitialSpawn", "func_180482_a", SPAWN_DESC) != null) {
            Asm.LOG.warn("{}: EntityWolf already has onInitialSpawn (another coremod); spawn packs are not given one variant", PATCH);
        } else {
            MethodNode spawn = new MethodNode(Opcodes.ACC_PUBLIC, spawnName, SPAWN_DESC, null, null);
            InsnList in = spawn.instructions;
            in.add(new VarInsnNode(Opcodes.ALOAD, 0));
            in.add(new VarInsnNode(Opcodes.ALOAD, 0));
            in.add(new VarInsnNode(Opcodes.ALOAD, 1));
            in.add(new VarInsnNode(Opcodes.ALOAD, 2));
            in.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, node.superName, spawnName, SPAWN_DESC, false));
            in.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "initialSpawn",
                    "(L" + WOLF + ";Lnet/minecraft/entity/IEntityLivingData;)Lnet/minecraft/entity/IEntityLivingData;", false));
            in.add(new InsnNode(Opcodes.ARETURN));
            spawn.maxStack = 4;
            spawn.maxLocals = 3;
            node.methods.add(spawn);
        }
        Asm.applied(PATCH + " EntityWolf");
        return true;
    }

    private static void head(MethodNode m, String hook) {
        InsnList in = new InsnList();
        in.add(new VarInsnNode(Opcodes.ALOAD, 0));
        in.add(new VarInsnNode(Opcodes.ALOAD, 1));
        in.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, hook, "(L" + WOLF + ";" + NBT + ")V", false));
        m.instructions.insert(in);
    }
}
