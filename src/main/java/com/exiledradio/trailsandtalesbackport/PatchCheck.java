package com.exiledradio.trailsandtalesbackport;

import net.minecraftforge.fml.common.FMLCommonHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The release-jar check behind {@code tools/release/boot-server.sh}: with
 * {@code -Dtrailsandtalesbackport.loadPatched=true} it loads and initializes every class this mod's coremod patches
 * (P1 to P20, {@code docs/PATCHES.md}), once the server has started, so the JVM verifies each one, and logs
 * {@code PATCHCHECK ok <n>} or one {@code PATCHCHECK FAIL} line per class.
 *
 * <p>Why: Mojang's classes that Forge does not patch keep ProGuard's stack-map frames, which mark a local TOP once
 * vanilla stops reading it. A patch that loads such a local is a VerifyError only in the real game, and only once the
 * class loads: the dev runs recompile Minecraft with javac and never show it, and some targets (a block, an AI) may
 * never load during a short boot. Without the property this does nothing.
 *
 * <p>A patch added to {@code TrailsAndTalesCore.TRANSFORMERS} adds its target classes here.
 */
public final class PatchCheck {

    static final String PROPERTY = TrailsAndTalesBackport.MOD_ID + ".loadPatched";

    /** Targets on both sides, by patch. */
    private static final String[] COMMON = {
            "net.minecraft.entity.Entity", // P2
            "net.minecraft.entity.EntityLivingBase", // P3, P9
            "net.minecraft.world.gen.ChunkProviderServer", // P4
            "net.minecraft.entity.passive.EntityWolf", // P5
            "net.minecraft.world.WorldEntitySpawner", // P6
            "net.minecraft.block.BlockJukebox", // P7
            "net.minecraft.entity.ai.EntityAIAttackMelee", // P8 and vanilla's subclasses
            "net.minecraft.entity.ai.EntityAIZombieAttack",
            "net.minecraft.entity.monster.EntityPolarBear$AIMeleeAttack",
            "net.minecraft.entity.monster.EntitySpider$AISpiderAttack",
            "net.minecraft.entity.passive.EntityRabbit$AIEvilAttack",
            "net.minecraft.world.World", // P10
            "net.minecraft.server.MinecraftServer",
            "net.minecraft.entity.monster.EntitySlime", // P11
            "net.minecraft.entity.monster.EntityMagmaCube",
            "net.minecraft.entity.projectile.EntityArrow", // P12
            "net.minecraft.inventory.ContainerEnchantment", // P13
            "net.minecraft.block.BlockEnchantmentTable",
            "net.minecraft.tileentity.TileEntityHopper", // P14
            "net.minecraft.block.BlockSponge", // P15
            "net.minecraft.block.BlockBarrier", // P16
            "net.minecraft.entity.item.EntityEnderPearl", // P17
            "net.minecraft.entity.player.EntityPlayer", // P18
            "net.minecraft.world.WorldServer", // P19
    };

    /** Client-only targets. */
    private static final String[] CLIENT = {
            "net.minecraft.client.Minecraft", // P1
            "net.minecraft.client.renderer.entity.Render", // P20
    };

    private PatchCheck() {
    }

    /** Every class a transformer of this mod patches on the given side. */
    public static List<String> targets(boolean client) {
        List<String> out = new ArrayList<String>(Arrays.asList(COMMON));
        if (client) {
            out.addAll(Arrays.asList(CLIENT));
        }
        return out;
    }

    /** Runs the check when the property asks for it. */
    static void runIfAsked() {
        if (!Boolean.getBoolean(PROPERTY)) {
            return;
        }
        boolean client = FMLCommonHandler.instance().getSide().isClient();
        List<String> targets = targets(client);
        int failed = 0;
        for (String target : targets) {
            try {
                Class.forName(target, true, PatchCheck.class.getClassLoader());
            } catch (Throwable t) {
                failed++;
                TrailsAndTalesBackport.LOGGER.error("PATCHCHECK FAIL {}: {}", target, t.toString().split("\n")[0], t);
            }
        }
        if (failed == 0) {
            TrailsAndTalesBackport.LOGGER.info("PATCHCHECK ok {} patched classes loaded and verified ({}).", targets.size(),
                    client ? "client" : "server");
        }
    }
}
