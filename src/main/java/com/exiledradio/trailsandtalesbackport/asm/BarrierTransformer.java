package com.exiledradio.trailsandtalesbackport.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * P16, 1.20.2's barriers: adds Forge's {@code addLandingEffects} to {@code BlockBarrier} as
 * {@code return SmallTweaks.barrierLanding()} (true: handled, so no dust), skipped if the class already has one.
 */
public final class BarrierTransformer implements IClassTransformer {

    static final String PATCH = "P16";
    static final String DESC = "(Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/world/WorldServer;Lnet/minecraft/util/math/BlockPos;"
            + "Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/entity/EntityLivingBase;I)Z";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !"net.minecraft.block.BlockBarrier".equals(transformedName)) {
            return bytes;
        }
        ClassNode node = Asm.read(bytes);
        return patch(node) ? Asm.write(node) : bytes;
    }

    public static boolean patch(ClassNode node) {
        if (Asm.method(node, "addLandingEffects", "addLandingEffects", DESC) != null) {
            Asm.LOG.warn("{} skipped: BlockBarrier already has addLandingEffects (another coremod)", PATCH);
            return false;
        }
        MethodNode m = new MethodNode(Opcodes.ACC_PUBLIC, "addLandingEffects", DESC, null, null);
        m.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "com/exiledradio/trailsandtalesbackport/mechanics/SmallTweaks",
                "barrierLanding", "()Z", false));
        m.instructions.add(new InsnNode(Opcodes.IRETURN));
        m.maxStack = 1;
        m.maxLocals = 7;
        node.methods.add(m);
        Asm.applied(PATCH + " BlockBarrier.addLandingEffects");
        return true;
    }
}
