package com.exiledradio.trailsandtalesbackport.smithing.client;

import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import com.exiledradio.trailsandtalesbackport.smithing.ContainerSmithing;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** The smithing stream's client parts: the 1.20 screen and its empty-slot icons in the item atlas. */
@SideOnly(Side.CLIENT)
public final class SmithingClient {

    /** 1.20's {@code item/empty_slot_*} icons that 1.12.2 lacks (its armor-slot icons it has). */
    static final String[] ICONS = {"smithing_template_armor_trim", "smithing_template_netherite_upgrade", "sword",
            "pickaxe", "axe", "hoe", "shovel", "ingot", "redstone_dust", "lapis_lazuli", "quartz", "diamond", "emerald",
            "amethyst_shard"};

    private SmithingClient() {
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.register(new SmithingClient());
    }

    /** For the GUI handler; returns {@code Object} so the common class names no client type. */
    public static Object screen(ContainerSmithing container) {
        return new GuiSmithing(Minecraft.getMinecraft().player.inventory, container);
    }

    static ResourceLocation icon(String name) {
        return new ResourceLocation(TrailsAndTalesBackport.MOD_ID, "items/empty_slot_" + name);
    }

    @SubscribeEvent
    public void stitch(TextureStitchEvent.Pre event) {
        if (event.getMap() == Minecraft.getMinecraft().getTextureMapBlocks()) {
            for (String name : ICONS) {
                event.getMap().registerSprite(icon(name));
            }
        }
    }
}
