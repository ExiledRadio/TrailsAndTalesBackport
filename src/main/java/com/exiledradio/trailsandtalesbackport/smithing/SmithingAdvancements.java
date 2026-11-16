package com.exiledradio.trailsandtalesbackport.smithing;

import com.exiledradio.trailsandtalesbackport.common.Content;

import net.minecraft.advancements.Advancement;
import net.minecraft.entity.player.EntityPlayerMP;

import javax.annotation.Nullable;

/**
 * 1.20's two trim advancements, whose {@code recipe_crafted} triggers 1.12.2 lacks: their criteria are
 * {@code minecraft:impossible} and granted here when a trimmed piece is taken from the table.
 * <ul>
 * <li><i>Crafting a New Look</i> ({@code adventure/trim_with_any_armor_pattern}): any trim.</li>
 * <li><i>Smithing with Style</i> ({@code adventure/trim_with_all_exclusive_armor_patterns}): one criterion per
 * exclusive pattern (spire, snout, rib, ward, silence, vex, tide, wayfinder), all required.</li>
 * </ul>
 */
final class SmithingAdvancements {

    private SmithingAdvancements() {
    }

    static void trimmed(EntityPlayerMP player, @Nullable Templates template) {
        grant(player, "adventure/trim_with_any_armor_pattern", "trimmed");
        if (template != null && Boolean.TRUE.equals(template.exclusive)) {
            grant(player, "adventure/trim_with_all_exclusive_armor_patterns", template.itemName());
        }
    }

    private static void grant(EntityPlayerMP player, String path, String criterion) {
        Advancement a = player.server.getAdvancementManager().getAdvancement(Content.id(path));
        if (a != null) {
            player.getAdvancements().grantCriterion(a, criterion);
        }
    }
}
