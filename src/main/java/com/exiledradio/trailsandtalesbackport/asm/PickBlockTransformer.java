package com.exiledradio.trailsandtalesbackport.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.List;

/**
 * P1, 1.20.5's Ctrl + pick block: {@code Minecraft.storeTEInStack} returns its stack through
 * {@code LookHooks.pickedBlock}, which takes 1.12.2's "(+NBT)" lore line off and names the item after a named block.
 */
public final class PickBlockTransformer implements IClassTransformer {

    static final String PATCH = "P1";
    static final String HOOKS = "com/exiledradio/trailsandtalesbackport/lookfeel/client/LookHooks";
    static final String STACK = "Lnet/minecraft/item/ItemStack;";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !"net.minecraft.client.Minecraft".equals(transformedName)) {
            return bytes;
        }
        ClassNode node = Asm.read(bytes);
        return patch(node) ? Asm.write(node) : bytes;
    }

    /** @return true when patched; for the self-test, which runs it on the dev and SRG classes */
    public static boolean patch(ClassNode node) {
        MethodNode m = Asm.method(node, "storeTEInStack", "func_184119_a",
                "(" + STACK + "Lnet/minecraft/tileentity/TileEntity;)" + STACK);
        if (m == null) {
            Asm.LOG.warn("{} skipped: Minecraft.storeTEInStack not found; Ctrl + pick block keeps \"(+NBT)\"", PATCH);
            return false;
        }
        List<AbstractInsnNode> returns = new ArrayList<AbstractInsnNode>();
        for (AbstractInsnNode insn : m.instructions.toArray()) {
            if (insn.getOpcode() == Opcodes.ARETURN) {
                returns.add(insn);
            }
        }
        if (returns.isEmpty()) {
            Asm.LOG.warn("{} skipped: Minecraft.storeTEInStack has no return", PATCH);
            return false;
        }
        for (AbstractInsnNode ret : returns) {
            m.instructions.insertBefore(ret, new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "pickedBlock",
                    "(" + STACK + ")" + STACK, false));
        }
        Asm.applied(PATCH + " Minecraft.storeTEInStack (" + returns.size() + " returns)");
        return true;
    }
}
