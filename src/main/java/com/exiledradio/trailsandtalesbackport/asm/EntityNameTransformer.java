package com.exiledradio.trailsandtalesbackport.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * P20 (client), 1.20.3's names of looked-at entities: {@code Render.canRenderName}'s body becomes
 * {@code return MechanicsClient.canRenderName(entity)} (straight-line code, no frame). Renderers that override it
 * (every mob's, item frames') are untouched.
 */
public final class EntityNameTransformer implements IClassTransformer {

    static final String PATCH = "P20";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !"net.minecraft.client.renderer.entity.Render".equals(transformedName)) {
            return bytes;
        }
        ClassNode node = Asm.read(bytes);
        return patch(node) ? Asm.write(node) : bytes;
    }

    public static boolean patch(ClassNode node) {
        MethodNode m = Asm.method(node, "canRenderName", "func_177070_b", "(Lnet/minecraft/entity/Entity;)Z");
        if (m == null || (m.access & Opcodes.ACC_ABSTRACT) != 0) {
            Asm.LOG.warn("{} skipped: Render.canRenderName not found", PATCH);
            return false;
        }
        m.instructions.clear();
        m.tryCatchBlocks.clear();
        if (m.localVariables != null) {
            m.localVariables.clear();
        }
        m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        m.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "com/exiledradio/trailsandtalesbackport/mechanics/client/MechanicsClient",
                "canRenderName", "(Lnet/minecraft/entity/Entity;)Z", false));
        m.instructions.add(new InsnNode(Opcodes.IRETURN));
        Asm.applied(PATCH + " Render.canRenderName");
        return true;
    }
}
