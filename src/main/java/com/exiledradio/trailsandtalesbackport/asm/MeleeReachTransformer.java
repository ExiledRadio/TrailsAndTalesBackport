package com.exiledradio.trailsandtalesbackport.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * P7, 1.20.2's mob melee reach: the first line of every {@code checkAndPerformAttack(EntityLivingBase, double)} in
 * {@code EntityAIAttackMelee} and in each class that extends it or {@code EntityAIZombieAttack} (vanilla's polar bear,
 * spider, rabbit and other mods' melee AIs) becomes
 * {@code distSq = MeleeReach.distance(this.attacker, target, distSq, this.getAttackReachSqr(target))}.
 * Parameters only, on the first line; no jump, no new local.
 */
public final class MeleeReachTransformer implements IClassTransformer {

    static final String PATCH = "P7";
    static final String MELEE = "net/minecraft/entity/ai/EntityAIAttackMelee";
    static final String ZOMBIE = "net/minecraft/entity/ai/EntityAIZombieAttack";
    static final String TARGET = "Lnet/minecraft/entity/EntityLivingBase;";
    static final String DESC = "(" + TARGET + "D)V";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || transformedName.startsWith("com.exiledradio.trailsandtalesbackport.asm.")) {
            return bytes;
        }
        boolean base = "net.minecraft.entity.ai.EntityAIAttackMelee".equals(transformedName);
        if (!base) {
            String superName;
            try {
                superName = new ClassReader(bytes).getSuperName();
            } catch (RuntimeException e) {
                return bytes;
            }
            if (!MELEE.equals(superName) && !ZOMBIE.equals(superName)) {
                return bytes;
            }
        }
        ClassNode node = Asm.read(bytes);
        return patch(node) ? Asm.write(node) : bytes;
    }

    public static boolean patch(ClassNode node) {
        MethodNode m = Asm.method(node, "checkAndPerformAttack", "func_190102_a", DESC);
        if (m == null || (m.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_STATIC)) != 0) {
            return false;
        }
        boolean mcp = "checkAndPerformAttack".equals(m.name);
        InsnList in = new InsnList();
        in.add(new VarInsnNode(Opcodes.ALOAD, 0));
        in.add(new FieldInsnNode(Opcodes.GETFIELD, MELEE, mcp ? "attacker" : "field_75441_b", "Lnet/minecraft/entity/EntityCreature;"));
        in.add(new VarInsnNode(Opcodes.ALOAD, 1));
        in.add(new VarInsnNode(Opcodes.DLOAD, 2));
        in.add(new VarInsnNode(Opcodes.ALOAD, 0));
        in.add(new VarInsnNode(Opcodes.ALOAD, 1));
        in.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, MELEE, mcp ? "getAttackReachSqr" : "func_179512_a", "(" + TARGET + ")D", false));
        in.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "com/exiledradio/trailsandtalesbackport/mechanics/MeleeReach", "distance",
                "(Lnet/minecraft/entity/EntityLiving;" + TARGET + "DD)D", false));
        in.add(new VarInsnNode(Opcodes.DSTORE, 2));
        m.instructions.insert(in);
        Asm.applied(PATCH + " " + node.name.replace('/', '.') + ".checkAndPerformAttack");
        return true;
    }
}
