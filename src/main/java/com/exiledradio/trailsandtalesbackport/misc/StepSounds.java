package com.exiledradio.trailsandtalesbackport.misc;

import com.exiledradio.backportcore.presentation.Presentation;
import com.exiledradio.trailsandtalesbackport.ModConfig;
import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import net.minecraft.block.Block;
import net.minecraft.block.BlockCarpet;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 1.20's step sounds for players (patch P2, 1.20.6's {@code Player.playStepSound} and {@code Entity}'s
 * {@code getPrimaryStepSoundBlockPos}, {@code playCombinationStepSounds}, {@code playMuffledStepSound}): with a
 * {@code #combination_step_sound_blocks} block on top of the one walked on (carpets, moss carpet, snow, nether sprouts
 * and the roots) both play, the top one at 0.15 of its volume and the one below muffled (0.05, pitch 0.8); with an
 * {@code #inside_step_sound_blocks} block there (powder snow, sculk vein, glow lichen, lily pad, small amethyst bud,
 * pink petals) only that one plays. 1.12.2 played the block below a carpet, and only snow layers on top.
 *
 * <p>Other mods' copies of the tagged blocks match by their modern id as the registry path (Caves &amp; Cliffs' moss
 * carpet, Nether Update's sprouts and roots ...). Every entity that is not a player goes to its own
 * {@code playStepSound}, as in 1.20.6.
 */
public final class StepSounds {

    private static final Set<String> COMBINATION = new HashSet<String>(Arrays.asList("moss_carpet",
            "nether_sprouts", "warped_roots", "crimson_roots"));
    private static final Set<String> INSIDE = new HashSet<String>(Arrays.asList("powder_snow", "sculk_vein", "glow_lichen",
            "waterlily", "lily_pad", "small_amethyst_bud", "pink_petals"));

    private static MethodHandle own;
    private static boolean broken;

    /** For the self-test: the last combination played ({@code top>below}), or null. */
    public static volatile String lastCombination;

    private StepSounds() {
    }

    /** @return true while 1.20's step sounds are on */
    public static boolean enabled() {
        return ModConfig.misc.enabled && ModConfig.misc.stepSounds && Presentation.isAtLeast("1.20");
    }

    public static boolean isCombination(Block block) {
        return block instanceof BlockCarpet || block == Blocks.SNOW_LAYER || tagged(COMBINATION, block);
    }

    public static boolean isInside(Block block) {
        return block == Blocks.WATERLILY || tagged(INSIDE, block);
    }

    private static boolean tagged(Set<String> set, Block block) {
        ResourceLocation id = block.getRegistryName();
        return id != null && set.contains(id.getPath());
    }

    /** P2: {@code Entity.move}'s step sound. */
    public static void playStepSound(Entity entity, BlockPos pos, Block block) {
        if (entity instanceof EntityPlayer && enabled()) {
            BlockPos above = pos.up();
            IBlockState top = entity.world.getBlockState(above);
            Block topBlock = top.getBlock();
            if (isCombination(topBlock)) {
                IBlockState below = entity.world.getBlockState(pos);
                SoundType t = topBlock.getSoundType(top, entity.world, above, entity);
                entity.playSound(t.getStepSound(), t.getVolume() * 0.15F, t.getPitch());
                SoundType b = block.getSoundType(below, entity.world, pos, entity);
                entity.playSound(b.getStepSound(), b.getVolume() * 0.05F, b.getPitch() * 0.8F);
                lastCombination = topBlock.getRegistryName() + ">" + block.getRegistryName();
                return;
            }
            if (isInside(topBlock)) {
                SoundType t = topBlock.getSoundType(top, entity.world, above, entity);
                entity.playSound(t.getStepSound(), t.getVolume() * 0.15F, t.getPitch());
                return;
            }
        }
        callOwn(entity, pos, block);
    }

    private static void callOwn(Entity entity, BlockPos pos, Block block) {
        try {
            if (own == null && !broken) {
                Method m = ReflectionHelper.findMethod(Entity.class, "playStepSound", "func_180429_a", BlockPos.class, Block.class);
                own = MethodHandles.lookup().unreflect(m);
            }
            if (own != null) {
                own.invoke(entity, pos, block);
            }
        } catch (Throwable e) {
            if (!broken) {
                broken = true;
                TrailsAndTalesBackport.LOGGER.error("Step sounds: Entity.playStepSound could not be called; entities walk silently.", e);
            }
        }
    }
}
