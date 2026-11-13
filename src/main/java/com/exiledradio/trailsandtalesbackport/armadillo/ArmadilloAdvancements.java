package com.exiledradio.trailsandtalesbackport.armadillo;

import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import com.exiledradio.trailsandtalesbackport.common.Content;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
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
 * The armadillo's advancements.
 *
 * <ul>
 *   <li><i>Isn't It Scute?</i> ({@code adventure/brush_armadillo}) is this mod's own file under the adventure
 *       tab. 1.12.2 has no trigger for using an item on a mob, so it is granted when a player brushes a scute
 *       off an armadillo.</li>
 *   <li>1.20.5 added the armadillo to <i>Two by Two</i> ({@code bred_all_animals}: one more criterion and
 *       requirement, {@code minecraft:armadillo}). 1.12.2 cannot add to another namespace's advancement file,
 *       so the loaded advancement gets the criterion by reflection whenever the server has loaded a new one
 *       (start, {@code /reload}), and players' progress is reloaded so their listeners include it. Criteria
 *       other mods add the same way are kept.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = TrailsAndTalesBackport.MOD_ID)
public final class ArmadilloAdvancements {

    public static final ResourceLocation TWO_BY_TWO = new ResourceLocation("minecraft", "husbandry/bred_all_animals");
    public static final ResourceLocation BRUSH_ARMADILLO = Content.id("adventure/brush_armadillo");
    /** 1.20.5's criterion name. */
    public static final String ARMADILLO_CRITERION = "minecraft:armadillo";

    private static final Field CRITERIA = ReflectionHelper.findField(Advancement.class, "criteria", "field_192080_e");
    private static final Field REQUIREMENTS = ReflectionHelper.findField(Advancement.class, "requirements", "field_192081_f");

    private static Advancement twoByTwo;
    private static boolean failed;

    private ArmadilloAdvancements() {
    }

    static void grantBrushArmadillo(EntityPlayerMP player) {
        MinecraftServer server = player.getServer();
        Advancement a = server == null ? null : server.getAdvancementManager().getAdvancement(BRUSH_ARMADILLO);
        if (a != null) {
            player.getAdvancements().grantCriterion(a, "brush_armadillo");
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && ArmadilloContent.enabled()) {
            MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
            if (server != null) {
                check(server);
            }
        }
    }

    /** Changes the loaded Two by Two if it is new; public for the self-test. */
    @SuppressWarnings("unchecked")
    public static void check(MinecraftServer server) {
        if (failed || server.worlds.length == 0 || server.worlds[0] == null) {
            return;
        }
        Advancement two = server.getAdvancementManager().getAdvancement(TWO_BY_TWO);
        if (two == null || two == twoByTwo) {
            return;
        }
        boolean changed;
        try {
            Map<String, Criterion> criteria = (Map<String, Criterion>) CRITERIA.get(two);
            changed = !criteria.containsKey(ARMADILLO_CRITERION);
            if (changed) {
                JsonObject type = new JsonObject();
                type.addProperty("type", Content.id("armadillo").toString());
                JsonObject conditions = new JsonObject();
                conditions.add("parent", type);
                conditions.add("partner", type);
                Map<String, Criterion> more = new LinkedHashMap<String, Criterion>(criteria);
                more.put(ARMADILLO_CRITERION, new Criterion(CriteriaTriggers.BRED_ANIMALS.deserializeInstance(conditions, null)));
                String[][] requirements = (String[][]) REQUIREMENTS.get(two);
                String[][] longer = Arrays.copyOf(requirements, requirements.length + 1);
                longer[requirements.length] = new String[] {ARMADILLO_CRITERION};
                CRITERIA.set(two, ImmutableMap.copyOf(more));
                REQUIREMENTS.set(two, longer);
            }
        } catch (RuntimeException | IllegalAccessException e) {
            failed = true;
            TrailsAndTalesBackport.LOGGER.warn("Could not add the armadillo to Two by Two; it keeps 1.12.2's criteria.", e);
            return;
        }
        twoByTwo = two;
        if (changed) {
            server.getPlayerList().reloadResources();
        }
    }
}
