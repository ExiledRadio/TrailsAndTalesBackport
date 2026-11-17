package com.exiledradio.trailsandtalesbackport.asm;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * This mod's own small coremod: the bytecode patches 1.20's behavior needs where 1.12.2 has no hook. One
 * transformer per patch, each documented in {@code docs/PATCHES.md}. No MixinBooter.
 *
 * <p>Named in the jar's manifest ({@code FMLCorePlugin}, with {@code FMLCorePluginContainsFMLMod}) and, in the dev
 * environment, by {@code -Dfml.coreMods.load} ({@code build.gradle}). Sorting index above 1000, so every class reaches
 * the transformers with SRG member names in a real instance (MCP names in dev); each transformer matches both.
 *
 * <p>Transformers only rewrite bytecode and name hook classes as strings: nothing here loads a Minecraft class or a
 * class of the mod itself. A patch is added to {@link #TRANSFORMERS}.
 */
@IFMLLoadingPlugin.Name("Trails and Tales Backport Core")
@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.TransformerExclusions("com.exiledradio.trailsandtalesbackport.asm.")
@IFMLLoadingPlugin.SortingIndex(1001)
public final class TrailsAndTalesCore implements IFMLLoadingPlugin {

    /** Every patch, in PATCHES.md order. */
    public static final String[] TRANSFORMERS = {
            "com.exiledradio.trailsandtalesbackport.asm.PickBlockTransformer",
            "com.exiledradio.trailsandtalesbackport.asm.StepSoundTransformer",
            "com.exiledradio.trailsandtalesbackport.asm.EffectParticlesTransformer", // P3
            "com.exiledradio.trailsandtalesbackport.asm.ChunkFailureTransformer", // P4
            "com.exiledradio.trailsandtalesbackport.asm.WolfTransformer", // P5
            "com.exiledradio.trailsandtalesbackport.asm.SpawnChanceTransformer", // P6
            "com.exiledradio.trailsandtalesbackport.asm.JukeboxTransformer", // P7
            "com.exiledradio.trailsandtalesbackport.asm.MeleeReachTransformer", // P8
            "com.exiledradio.trailsandtalesbackport.asm.ShieldTransformer", // P9
            "com.exiledradio.trailsandtalesbackport.asm.SpawnChunkTransformer", // P10
            "com.exiledradio.trailsandtalesbackport.asm.SlimeJumpTransformer", // P11
            "com.exiledradio.trailsandtalesbackport.asm.ArrowPickupTransformer", // P12
            "com.exiledradio.trailsandtalesbackport.asm.EnchantingTableTransformer", // P13
            "com.exiledradio.trailsandtalesbackport.asm.HopperTransformer", // P14
            "com.exiledradio.trailsandtalesbackport.asm.SpongeTransformer", // P15
            "com.exiledradio.trailsandtalesbackport.asm.BarrierTransformer", // P16
            "com.exiledradio.trailsandtalesbackport.asm.EnderPearlTransformer", // P17
            "com.exiledradio.trailsandtalesbackport.asm.PortalTimeTransformer", // P18
            "com.exiledradio.trailsandtalesbackport.asm.PrecipitationTransformer", // P19
            "com.exiledradio.trailsandtalesbackport.asm.EntityNameTransformer", // P20
    };

    @Override
    public String[] getASMTransformerClass() {
        return TRANSFORMERS.clone();
    }

    @Nullable
    @Override
    public String getModContainerClass() {
        return null;
    }

    @Nullable
    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
    }

    @Nullable
    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
