package com.exiledradio.trailsandtalesbackport.selftest;

import com.exiledradio.backportcore.duplicate.DuplicateBlocks;
import com.exiledradio.backportcore.migrate.FutureMcConvert;
import com.exiledradio.backportcore.migrate.FutureMcTable;
import com.exiledradio.trailsandtalesbackport.ModConfig;
import com.exiledradio.trailsandtalesbackport.bamboo.plant.BlockBamboo;
import com.exiledradio.trailsandtalesbackport.common.Owners;
import com.exiledradio.trailsandtalesbackport.migrate.FutureMcPairs;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLog;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;

import java.util.Map;

/** The other mod's blocks and items that this mod also has, becoming ours, and duplicates. */
public final class FutureMcSelfTest implements SelfTest {

    @Override
    public String name() {
        return "futuremc";
    }

    @Override
    public void run(Check c) throws Exception {
        c.ok(!Loader.isModLoaded(FutureMcTable.FUTURE_MC), "the test game does not have the other mod");
        c.eq(DuplicateBlocks.Keep.OURS, ModConfig.compat.duplicates, "compat.duplicates defaults to OURS");
        c.ok(ModConfig.compat.convertRemoved, "compat.convertRemoved defaults to on");
        c.ok(!Loader.isModLoaded(Owners.VILLAGE_AND_PILLAGE) && !Loader.isModLoaded(Owners.NETHER_UPDATE),
                "no owner installed, so this mod's copies are registered");

        // Every recorded block of the pairs, each metadata value it saves, turns into ours with nothing lost.
        int pairs = 0;
        for (String[] pair : FutureMcPairs.blockPairs()) {
            String path = pair[1];
            c.ok(FutureMcTable.recorded(path), "their " + path + " is in the recorded table");
            Block ours = Owners.ourBlock(pair[0]);
            if (ours == null) {
                // The smithing stream's table copy is not in this build yet.
                c.eq("smithing_table", pair[0], "only the smithing table copy may be missing");
                c.ok(FutureMcTable.block(new ResourceLocation(FutureMcTable.FUTURE_MC, path)) == null
                        || !"trailsandtalesbackport".equals(FutureMcTable.block(new ResourceLocation(FutureMcTable.FUTURE_MC, path)).block.getRegistryName().getNamespace()),
                        "with no copy of ours, their " + path + " has no counterpart of ours");
                continue;
            }
            pairs++;
            FutureMcTable.Target target = FutureMcTable.block(new ResourceLocation(FutureMcTable.FUTURE_MC, path));
            if (!c.ok(target != null && target.block == ours, "their " + path + " becomes our " + pair[0])) {
                continue;
            }
            for (int meta = 0; meta < 16; meta++) {
                Map<String, String> props = FutureMcTable.recordedState(path, meta);
                if (props == null) {
                    continue;
                }
                FutureMcConvert.Converted converted = FutureMcConvert.state(target, props);
                c.ok(converted.dropped.isEmpty(), "their " + path + " meta " + meta + " " + props + " carries across: " + converted.dropped);
            }
            c.ok(!DuplicateBlocks.isHidden(ours), "without the other mod our " + pair[0] + " is not hidden");
        }
        c.eq(15, pairs, "all 15 pairs have our half here (12 stripped logs and woods, bamboo, chain, smithing table)");

        // States one by one.
        FutureMcTable.Target log = FutureMcTable.block(new ResourceLocation(FutureMcTable.FUTURE_MC, "stripped_spruce_log"));
        if (log != null) {
            String[] axes = {"y", "x", "z", "none"};
            BlockLog.EnumAxis[] want = {BlockLog.EnumAxis.Y, BlockLog.EnumAxis.X, BlockLog.EnumAxis.Z, BlockLog.EnumAxis.NONE};
            for (int i = 0; i < 4; i++) {
                IBlockState state = FutureMcConvert.state(log, FutureMcTable.recordedState("stripped_spruce_log", i * 4)).state;
                c.eq(want[i], state.getValue(BlockLog.LOG_AXIS), "their stripped spruce log axis " + axes[i]);
                c.eq(i * 4, state.getBlock().getMetaFromState(state), "and saves as the same metadata");
            }
        }
        FutureMcTable.Target bamboo = FutureMcTable.block(new ResourceLocation(FutureMcTable.FUTURE_MC, "bamboo"));
        if (bamboo != null) {
            for (int meta = 0; meta < 12; meta++) {
                Map<String, String> props = FutureMcTable.recordedState("bamboo", meta);
                IBlockState state = FutureMcConvert.state(bamboo, props).state;
                c.eq("true".equals(props.get("thick")) ? 1 : 0, state.getValue(BlockBamboo.AGE), "their bamboo meta " + meta + " thick -> age");
                c.eq("true".equals(props.get("mature")) ? 1 : 0, state.getValue(BlockBamboo.STAGE), "their bamboo meta " + meta + " mature -> stage");
                c.eq(props.get("leaves"), state.getValue(BlockBamboo.LEAVES).getName(), "their bamboo meta " + meta + " leaves");
            }
        }
        FutureMcTable.Target chain = FutureMcTable.block(new ResourceLocation(FutureMcTable.FUTURE_MC, "chain"));
        if (chain != null) {
            IBlockState state = FutureMcConvert.state(chain, FutureMcTable.recordedState("chain", 0)).state;
            c.eq(state.getBlock().getDefaultState(), state, "their chain (vertical only) becomes our vertical chain");
        }

        // Items: the block items through their blocks.
        String[] items = {"stripped_oak_log", "stripped_dark_oak_wood", "bamboo", "chain"};
        for (String path : items) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString("id", FutureMcTable.FUTURE_MC + ":" + path);
            tag.setByte("Count", (byte) 3);
            tag.setShort("Damage", (short) 0);
            int changed = FutureMcConvert.item(tag);
            Item ours = Owners.ours(path);
            c.ok(changed == 1 && ours != null && ours.getRegistryName().toString().equals(tag.getString("id")) && tag.getByte("Count") == 3,
                    "their item " + path + " becomes ours (" + tag.getString("id") + ")");
        }

        // Nothing of ours is hidden without the other mod.
        for (Item item : DuplicateBlocks.hiddenItems()) {
            c.ok(!"trailsandtalesbackport".equals(item.getRegistryName().getNamespace()), "our " + item.getRegistryName() + " is not hidden");
        }
    }
}
