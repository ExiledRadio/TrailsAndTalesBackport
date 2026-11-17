package com.exiledradio.trailsandtalesbackport.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * P18, 1.20.3's portal delay game rules: {@code EntityPlayer.getMaxInPortalTime}'s body becomes
 * {@code return NewGameRules.portalTime(this)} (straight-line code, no frame).
 */
public final class PortalTimeTransformer implements IClassTransformer {

    static final String PATCH = "P18";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !"net.minecraft.entity.player.EntityPlayer".equals(transformedName)) {
            return bytes;
        }
        ClassNode node = Asm.read(bytes);
        return patch(node) ? Asm.write(node) : bytes;
    }

    public static boolean patch(ClassNode node) {
        MethodNode m = Asm.method(node, "getMaxInPortalTime", "func_82145_z", "()I");
        if (m == null || (m.access & Opcodes.ACC_ABSTRACT) != 0) {
            Asm.LOG.warn("{} skipped: EntityPlayer.getMaxInPortalTime not found", PATCH);
            return false;
        }
        m.instructions.clear();
        m.tryCatchBlocks.clear();
        if (m.localVariables != null) {
            m.localVariables.clear();
        }
        m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        m.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "com/exiledradio/trailsandtalesbackport/mechanics/NewGameRules",
                "portalTime", "(Lnet/minecraft/entity/player/EntityPlayer;)I", false));
        m.instructions.add(new InsnNode(Opcodes.IRETURN));
        Asm.applied(PATCH + " EntityPlayer.getMaxInPortalTime");
        return true;
    }
}
