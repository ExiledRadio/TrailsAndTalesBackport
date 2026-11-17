package com.exiledradio.trailsandtalesbackport.mechanics;

import com.exiledradio.trailsandtalesbackport.ModConfig;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;

/**
 * 1.20's enchanting table (23w12a, MC-2474), patch P13. 1.20's {@code EnchantingTableBlock.isValidBookShelf}: a
 * bookshelf at one of the 32 places two blocks out (rows y 0 and 1) counts when the one block halfway to it, at the
 * shelf's own height ({@code offset / 2}, rounded toward zero), is in {@code #enchantment_power_transmitter}
 * ({@code #replaceable}: air, water, lava, grass, ferns, dead bushes, vines, fire, snow layers, structure voids and
 * the like). 1.12.2 asks for air in both rows of the ring next to the table, by eight directions.
 */
public final class EnchantingTables {

    private EnchantingTables() {
    }

    public static boolean enabled() {
        return ModConfig.mechanics.enchantingGaps;
    }

    /** 1.20's {@code #replaceable}, read as 1.12.2's replaceable materials (and any air block). */
    public static boolean transmits(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        return state.getBlock().isAir(state, world, pos) || state.getMaterial().isReplaceable();
    }

    /**
     * P13, before {@code ContainerEnchantment.onCraftMatrixChanged} seeds its random: 1.12.2's bookshelf power in,
     * 1.20's out. Forge's {@code getEnchantPower} is still each shelf's power, so modded bookshelves keep theirs.
     */
    public static float power(float vanilla, World world, BlockPos table) {
        if (!enabled()) {
            return vanilla;
        }
        float power = 0.0F;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (Math.abs(dx) != 2 && Math.abs(dz) != 2) {
                    continue;
                }
                for (int dy = 0; dy <= 1; dy++) {
                    float shelf = ForgeHooks.getEnchantPower(world, table.add(dx, dy, dz));
                    if (shelf > 0.0F && transmits(world, table.add(dx / 2, dy, dz / 2))) {
                        power += shelf;
                    }
                }
            }
        }
        return power;
    }

    /**
     * P13, {@code BlockEnchantmentTable.randomDisplayTick}'s {@code worldIn.isAirBlock(pos.add(i / 2, 0, j / 2))}: whether
     * a shelf's particles fly to the table. 1.12.2's loop asks this for the lower row's halfway block only, for both
     * rows' shelves.
     */
    public static boolean particlesPass(World world, BlockPos between) {
        return enabled() ? transmits(world, between) : world.isAirBlock(between);
    }
}
