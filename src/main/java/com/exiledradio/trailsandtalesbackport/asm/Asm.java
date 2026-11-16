package com.exiledradio.trailsandtalesbackport.asm;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Small helpers shared by the patches. Every member is matched by its MCP name (the dev environment) or its SRG name
 * (a real instance), and by descriptor, never by position alone.
 */
public final class Asm {

    public static final Logger LOG = LogManager.getLogger("Trails and Tales Backport Core");

    /** Which patches the running game applied, by number ("P1"), for the self-tests. */
    public static final List<String> APPLIED = new ArrayList<String>();

    private Asm() {
    }

    public static ClassNode read(byte[] bytes) {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        return node;
    }

    /**
     * Writes a class back without recomputing frames: computing them would load classes from inside a transformer.
     * The patches here change no frame (no jump, no new local; parameters read only on a method's first line).
     */
    public static byte[] write(ClassNode node) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }

    public static MethodNode method(ClassNode node, String mcp, String srg, String desc) {
        for (MethodNode m : node.methods) {
            if ((mcp.equals(m.name) || srg.equals(m.name)) && desc.equals(m.desc)) {
                return m;
            }
        }
        return null;
    }

    /** @return true for a call to {@code owner.(mcp|srg)desc} */
    public static boolean isCall(AbstractInsnNode insn, String owner, String mcp, String srg, String desc) {
        if (!(insn instanceof MethodInsnNode)) {
            return false;
        }
        MethodInsnNode call = (MethodInsnNode) insn;
        return owner.equals(call.owner) && (mcp.equals(call.name) || srg.equals(call.name)) && desc.equals(call.desc);
    }

    public static synchronized void applied(String patch) {
        APPLIED.add(patch);
        LOG.info("Applied {}", patch);
    }
}
