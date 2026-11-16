package com.exiledradio.trailsandtalesbackport.migrate;

import com.exiledradio.backportcore.duplicate.DuplicateBlocks;
import com.exiledradio.backportcore.migrate.FutureMcTable;
import com.exiledradio.trailsandtalesbackport.ModConfig;
import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import com.exiledradio.trailsandtalesbackport.common.wood.AxeStripping;

import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * This mod's entries in Backport Core's tables for the other mod that adds some of the same blocks ({@code futuremc},
 * 0.2.21), so a world can move over and duplicates stay out of sight (Rule 3). Quiet compat: no user-facing text
 * names that mod.
 *
 * <h2>What it has of ours</h2>
 * Nothing of 1.20: no cherry or bamboo wood, hanging signs, chiseled bookshelf, archaeology, sniffer, camel,
 * armadillo, wolf armor, smithing templates or piglin head (read from its 0.2.21 registry classes, {@code FBlocks},
 * {@code FItems}, {@code FEntities}). What it shares is the copies this mod makes of other family mods' blocks when
 * their owners are absent:
 * <ul>
 *   <li>the six stripped logs and six stripped woods (Village &amp; Pillage's; Update Aquatic copies them too);</li>
 *   <li>{@code bamboo}, the plant (Village &amp; Pillage's): its {@code thick} and {@code mature} are our {@code
 *       age} and {@code stage}, counted, as Village &amp; Pillage declares; its item is the block's;</li>
 *   <li>{@code chain} (Nether Update's), vertical only in that mod;</li>
 *   <li>{@code smithing_table} (Village &amp; Pillage's), for the smithing stream's copy: its {@code facing} has nothing
 *       to go to, as Village &amp; Pillage declares. A pair only once that copy registers.</li>
 * </ul>
 * Each pair counts only while both halves exist: with an owner installed this mod registers no copy, and the owner's
 * own pairs take over.
 *
 * <p>Its axe turns vanilla's logs into its own stripped logs before this mod's stripping is asked (its handler runs at
 * normal priority, this mod's at low). With ours kept, this mod strips first, so a stripped log is the visible one.
 *
 * <p>Metadata meanings: {@code migration/futuremc_states.json} ({@code tools/futuremc/states.py}), the owners' recorded
 * entries.
 */
@Mod.EventBusSubscriber(modid = TrailsAndTalesBackport.MOD_ID)
public final class FutureMcPairs {

    static final String[] WOODS = {"oak", "spruce", "birch", "jungle", "acacia", "dark_oak"};

    /** Blocks of the same name in both. */
    static final List<String> SAME_BLOCKS;

    static {
        List<String> same = new ArrayList<String>();
        for (String wood : WOODS) {
            same.add("stripped_" + wood + "_log");
            same.add("stripped_" + wood + "_wood");
        }
        same.addAll(Arrays.asList("bamboo", "chain", "smithing_table"));
        SAME_BLOCKS = Collections.unmodifiableList(same);
    }

    private FutureMcPairs() {
    }

    /** @return every {our block, their block} pair */
    public static List<String[]> blockPairs() {
        List<String[]> pairs = new ArrayList<String[]>();
        for (String name : SAME_BLOCKS) {
            pairs.add(new String[] {name, name});
        }
        return pairs;
    }

    /** From the mod's pre-init. */
    public static void preInit() {
        FutureMcTable.Contributor table = FutureMcTable.contributor(TrailsAndTalesBackport.MOD_ID, () -> ModConfig.compat.convertRemoved);
        // Village & Pillage's bamboo, which ours copies, counts thickness as age 0/1 and maturity as stage 0/1.
        table.renameProperty("bamboo", "thick", "age", true);
        table.renameProperty("bamboo", "mature", "stage", true);
        // Village & Pillage's smithing table, which the smithing stream copies, does not turn.
        table.ignoreProperty("smithing_table", "facing");
        table.recordedStates("/assets/trailsandtalesbackport/migration/futuremc_states.json");

        DuplicateBlocks.register(TrailsAndTalesBackport.MOD_ID, FutureMcTable.FUTURE_MC, () -> ModConfig.compat.duplicates,
                FutureMcPairs::blockPairs);
    }

    /** With ours kept, our stripping goes ahead of theirs; it cancels the click when it strips, so theirs never runs. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void stripFirst(PlayerInteractEvent.RightClickBlock event) {
        if (!event.isCanceled() && ModConfig.compat.duplicates == DuplicateBlocks.Keep.OURS
                && Loader.isModLoaded(FutureMcTable.FUTURE_MC)) {
            AxeStripping.onRightClickBlock(event);
        }
    }
}
