package com.exiledradio.trailsandtalesbackport.client;

import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import com.exiledradio.trailsandtalesbackport.common.Content;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.StateMapperBase;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Map;

/**
 * Client-only setup. A dedicated server never loads this class: the mod class reaches it through a
 * no-argument call, which names no client type.
 *
 * <ul>
 *   <li>A model for every item model request in {@link Content#itemModels()} - by exact name, never by
 *       walking the model registry's keys, which some performance mods bake lazily.</li>
 *   <li>One model name per multipart block ({@link Content#multipart}).</li>
 * </ul>
 *
 * Streams with client work of their own (renderers, colors, particles) add a class in their own
 * {@code <stream>.client} package and one line in {@link #init}.
 */
public final class ClientSetup {

    private ClientSetup() {
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.register(new ClientSetup());
        net.minecraftforge.fml.client.registry.ClientRegistry.bindTileEntitySpecialRenderer(
                com.exiledradio.trailsandtalesbackport.common.wood.WoodSign.Tile.class,
                new com.exiledradio.trailsandtalesbackport.common.wood.client.WoodSignRenderer());
        // Streams add their client init calls below, one line each.
        com.exiledradio.trailsandtalesbackport.cherry.client.CherryClient.init();
        net.minecraftforge.fml.client.registry.ClientRegistry.bindTileEntitySpecialRenderer(
                com.exiledradio.trailsandtalesbackport.hangingsign.TileHangingSign.class,
                new com.exiledradio.trailsandtalesbackport.hangingsign.client.HangingSignRenderer());
        com.exiledradio.trailsandtalesbackport.boat.client.RenderModBoat.register();
        com.exiledradio.trailsandtalesbackport.archaeology.client.ArchaeologyClient.init();
        com.exiledradio.trailsandtalesbackport.sniffer.client.SnifferClient.init();
        com.exiledradio.trailsandtalesbackport.armadillo.client.ArmadilloClient.init();
        com.exiledradio.trailsandtalesbackport.camel.client.CamelClient.init();
        com.exiledradio.trailsandtalesbackport.lookfeel.client.LookAndFeelClient.init();
    }

    @SubscribeEvent
    public void registerModels(ModelRegistryEvent event) {
        for (Map.Entry<Item, Map<Integer, String>> entry : Content.itemModels().entrySet()) {
            for (Map.Entry<Integer, String> meta : entry.getValue().entrySet()) {
                ModelLoader.setCustomModelResourceLocation(entry.getKey(), meta.getKey(),
                        new ModelResourceLocation(TrailsAndTalesBackport.MOD_ID + ":" + meta.getValue(), "inventory"));
            }
        }
        for (final Block block : Content.multipartBlocks()) {
            final ModelResourceLocation one = new ModelResourceLocation(block.getRegistryName(), "multipart");
            ModelLoader.setCustomStateMapper(block, new StateMapperBase() {
                @Override
                protected ModelResourceLocation getModelResourceLocation(IBlockState state) {
                    return one;
                }
            });
        }
    }
}
