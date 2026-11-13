package com.exiledradio.trailsandtalesbackport.camel;

import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import com.exiledradio.trailsandtalesbackport.common.Content;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 1.20 added the camel to <i>Two by Two</i> ({@code husbandry/bred_all_animals}): one more criterion,
 * {@code minecraft:camel} (bred two camels), as one more requirement.
 *
 * <p>1.12.2 cannot add to another namespace's advancement file, so the loaded advancement gets the criterion
 * (its two final fields, by reflection). The server builds new advancement objects whenever it loads them
 * (start, {@code /reload}); each server tick checks whether the loaded one is still the one changed, changes a
 * new one and reloads players' progress so their listeners include the criterion. Criteria other mods add the
 * same way are kept.
 */
public final class CamelAdvancements {

    public static final ResourceLocation TWO_BY_TWO = new ResourceLocation("minecraft", "husbandry/bred_all_animals");
    /** 1.20's criterion name. */
    public static final String CAMEL_CRITERION = "minecraft:camel";

    private static final Field CRITERIA = ReflectionHelper.findField(Advancement.class, "criteria", "field_192080_e");
    private static final Field REQUIREMENTS = ReflectionHelper.findField(Advancement.class, "requirements", "field_192081_f");

    private Advancement changedTwoByTwo;
    private boolean failed;

    @SubscribeEvent
    public void tick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
            if (server != null) {
                check(server);
            }
        }
    }

    /** Changes the loaded <i>Two by Two</i> if it is a new one. */
    void check(MinecraftServer server) {
        if (this.failed || server.worlds.length == 0 || server.worlds[0] == null) {
            return;
        }
        Advancement two = server.getAdvancementManager().getAdvancement(TWO_BY_TWO);
        if (two == null || two == this.changedTwoByTwo) {
            return;
        }
        try {
            if (addCamel(two)) {
                server.getPlayerList().reloadResources();
            }
        } catch (RuntimeException | IllegalAccessException e) {
            this.failed = true;
            TrailsAndTalesBackport.LOGGER.warn("Could not add the camel to Two by Two; it keeps 1.12.2's criteria.", e);
            return;
        }
        this.changedTwoByTwo = two;
    }

    /** @return true if added (false if it already had it) */
    @SuppressWarnings("unchecked")
    static boolean addCamel(Advancement advancement) throws IllegalAccessException {
        Map<String, Criterion> criteria = (Map<String, Criterion>) CRITERIA.get(advancement);
        if (criteria.containsKey(CAMEL_CRITERION)) {
            return false;
        }
        JsonObject type = new JsonObject();
        type.addProperty("type", Content.id("camel").toString());
        JsonObject conditions = new JsonObject();
        conditions.add("parent", type);
        conditions.add("partner", type);
        Map<String, Criterion> more = new LinkedHashMap<String, Criterion>(criteria);
        more.put(CAMEL_CRITERION, new Criterion(CriteriaTriggers.BRED_ANIMALS.deserializeInstance(conditions, null)));
        String[][] requirements = (String[][]) REQUIREMENTS.get(advancement);
        String[][] changed = Arrays.copyOf(requirements, requirements.length + 1);
        changed[requirements.length] = new String[] {CAMEL_CRITERION};
        CRITERIA.set(advancement, ImmutableMap.copyOf(more));
        REQUIREMENTS.set(advancement, changed);
        return true;
    }
}
