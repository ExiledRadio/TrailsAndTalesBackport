package com.exiledradio.trailsandtalesbackport.sniffer;

import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import com.exiledradio.trailsandtalesbackport.common.Content;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.ICriterionInstance;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The sniffer's advancements.
 *
 * <ul>
 *   <li><i>Smells Interesting</i>, <i>Little Sniffs</i> and <i>Planting the Past</i> are this mod's own files
 *       under the husbandry tab. 1.12.2 has no trigger for feeding a mob, so <i>Little Sniffs</i> is granted
 *       when a player feeds a snifflet torchflower seeds.</li>
 *   <li>1.20 added the sniffer to <i>Two by Two</i> ({@code bred_all_animals}: one more criterion and one more
 *       requirement, {@code minecraft:sniffer}, the parent being a sniffer) and both seeds to <i>A Seedy
 *       Place</i> ({@code plant_seed}: two more criteria in its one any-of requirement). 1.12.2 cannot add to
 *       another namespace's advancement file, so the loaded advancements get the criteria (their two final
 *       fields, by reflection). The server builds new advancement objects when it loads them (start,
 *       {@code /reload}); each server tick checks whether the loaded ones are still those changed, changes new
 *       ones and reloads players' progress so their listeners include the new criteria. Criteria another mod
 *       adds the same way are kept.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = TrailsAndTalesBackport.MOD_ID)
public final class SnifferAdvancements {

    public static final ResourceLocation TWO_BY_TWO = new ResourceLocation("minecraft", "husbandry/bred_all_animals");
    public static final ResourceLocation SEEDY_PLACE = new ResourceLocation("minecraft", "husbandry/plant_seed");
    public static final ResourceLocation FEED_SNIFFLET = Content.id("husbandry/feed_snifflet");
    /** 1.20's criterion names. */
    public static final String SNIFFER_CRITERION = "minecraft:sniffer";
    public static final String TORCHFLOWER_CRITERION = "torchflower";
    public static final String PITCHER_CRITERION = "pitcher_pod";

    private static final Field CRITERIA = ReflectionHelper.findField(Advancement.class, "criteria", "field_192080_e");
    private static final Field REQUIREMENTS = ReflectionHelper.findField(Advancement.class, "requirements", "field_192081_f");

    private static Advancement twoByTwo;
    private static Advancement seedyPlace;
    private static boolean failed;

    private SnifferAdvancements() {
    }

    static void grantFeedSnifflet(EntityPlayerMP player) {
        MinecraftServer server = player.getServer();
        Advancement a = server == null ? null : server.getAdvancementManager().getAdvancement(FEED_SNIFFLET);
        if (a != null) {
            player.getAdvancements().grantCriterion(a, "feed_snifflet");
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && SnifferContent.enabled()) {
            MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
            if (server != null) {
                check(server);
            }
        }
    }

    /** Changes the loaded vanilla advancements if they are new; public for the self-test. */
    public static void check(MinecraftServer server) {
        if (failed || server.worlds.length == 0 || server.worlds[0] == null) {
            return;
        }
        Advancement two = server.getAdvancementManager().getAdvancement(TWO_BY_TWO);
        Advancement seedy = server.getAdvancementManager().getAdvancement(SEEDY_PLACE);
        if (two == twoByTwo && seedy == seedyPlace) {
            return;
        }
        boolean changed = false;
        try {
            if (two != null && two != twoByTwo) {
                JsonObject type = new JsonObject();
                type.addProperty("type", Content.id("sniffer").toString());
                JsonObject conditions = new JsonObject();
                conditions.add("parent", type);
                conditions.add("partner", type);
                changed |= add(two, SNIFFER_CRITERION, CriteriaTriggers.BRED_ANIMALS.deserializeInstance(conditions, null), false);
            }
            if (seedy != null && seedy != seedyPlace) {
                changed |= add(seedy, TORCHFLOWER_CRITERION, placed("torchflower_crop"), true);
                changed |= add(seedy, PITCHER_CRITERION, placed("pitcher_crop"), true);
            }
        } catch (RuntimeException | IllegalAccessException e) {
            failed = true;
            TrailsAndTalesBackport.LOGGER.warn("Could not add the sniffer to Two by Two and A Seedy Place; they keep 1.12.2's criteria.", e);
            return;
        }
        twoByTwo = two;
        seedyPlace = seedy;
        if (changed) {
            server.getPlayerList().reloadResources();
        }
    }

    private static ICriterionInstance placed(String block) {
        JsonObject conditions = new JsonObject();
        conditions.addProperty("block", Content.id(block).toString());
        return CriteriaTriggers.PLACED_BLOCK.deserializeInstance(conditions, null);
    }

    /**
     * @param anyOf true to add it to the advancement's first requirement (any of), false as a requirement of its own
     * @return true if added (false if the advancement already had it)
     */
    @SuppressWarnings("unchecked")
    private static boolean add(Advancement advancement, String name, ICriterionInstance instance, boolean anyOf) throws IllegalAccessException {
        Map<String, Criterion> criteria = (Map<String, Criterion>) CRITERIA.get(advancement);
        if (criteria.containsKey(name)) {
            return false;
        }
        Map<String, Criterion> more = new LinkedHashMap<String, Criterion>(criteria);
        more.put(name, new Criterion(instance));
        String[][] requirements = (String[][]) REQUIREMENTS.get(advancement);
        String[][] changed;
        if (anyOf && requirements.length > 0) {
            changed = requirements.clone();
            changed[0] = Arrays.copyOf(requirements[0], requirements[0].length + 1);
            changed[0][requirements[0].length] = name;
        } else {
            changed = Arrays.copyOf(requirements, requirements.length + 1);
            changed[requirements.length] = new String[] {name};
        }
        CRITERIA.set(advancement, ImmutableMap.copyOf(more));
        REQUIREMENTS.set(advancement, changed);
        return true;
    }
}
