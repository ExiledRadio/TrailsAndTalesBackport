package com.exiledradio.trailsandtalesbackport.selftest;

import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import com.exiledradio.trailsandtalesbackport.common.Owners;

import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.Loader;

/** The shared code every stream builds on. */
public final class ScaffoldSelfTest implements SelfTest {

    @Override
    public String name() {
        return "scaffold";
    }

    @Override
    public void run(Check check) {
        BlockPos area = check.area();
        check.ok(check.world.isAirBlock(area.up()), "test area is clear");
        check.ok(Loader.isModLoaded("backportcore"), "Backport Core is loaded");
        check.ok(Loader.isModLoaded(TrailsAndTalesBackport.MOD_ID), "this mod is loaded");
        // With nothing else installed, every copied block is ours to register.
        if (!Owners.present(Owners.NETHER_UPDATE)) {
            check.ok(Owners.shouldRegisterCopy("chain"), "chain copy registers without its owner");
        }
        check.eq(null, Owners.theirs(Owners.WILD_UPDATE, "sculk_sensor"), "absent owner gives no item");
    }
}
