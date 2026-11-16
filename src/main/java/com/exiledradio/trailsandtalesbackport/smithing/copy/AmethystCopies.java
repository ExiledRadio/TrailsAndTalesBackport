package com.exiledradio.trailsandtalesbackport.smithing.copy;

import com.exiledradio.trailsandtalesbackport.ModConfig;
import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import com.exiledradio.trailsandtalesbackport.common.Content;
import com.exiledradio.trailsandtalesbackport.common.Owners;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * Our copy of Caves &amp; Cliffs' amethyst (Rule 1), registered only without it: the amethyst shard for the amethyst
 * trim and the calibrated sculk sensor, with what makes it obtainable in vanilla - amethyst geodes, whose shell is
 * calcite and smooth basalt, budding amethyst growing the three buds and the cluster, and the block of amethyst.
 * The blocks and geodes are the owner's own classes ({@code tools/smithing/copy_from_owner.py}), with its states,
 * numbers and sounds; tinted glass and the spyglass are not copied (nothing of ours is made from them).
 *
 * <p>Registered when the smithing stream or the calibrated sculk sensor is on. When Caves &amp; Cliffs is added to
 * a world later, Backport Core renames these to its blocks (they belong to it in Core's ownership table).
 */
@Mod.EventBusSubscriber(modid = TrailsAndTalesBackport.MOD_ID)
public final class AmethystCopies {

    public static BlockAmethyst amethyst;
    public static BlockAmethyst.Budding budding;
    /** Small, medium, large bud, cluster: the order budding amethyst grows them in. */
    public static BlockAmethystCluster[] stages;
    public static Block calcite;
    public static Block smoothBasalt;
    public static Item shard;
    public static SoundEvent chime;

    private static final List<Block> ALL = new ArrayList<Block>();

    private AmethystCopies() {
    }

    public static boolean registering() {
        return (ModConfig.smithing.enabled || ModConfig.calibratedSculkSensor.enabled)
                && !Loader.isModLoaded(Owners.CAVES_AND_CLIFFS) && Owners.shouldRegisterCopy("amethyst_block");
    }

    public static void preInit() {
    }

    public static void init() {
        if (amethyst != null) {
            GameRegistry.registerWorldGenerator(new GeodeGenerator(), 0);
        }
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        if (!registering()) {
            return;
        }
        IForgeRegistry<Block> r = event.getRegistry();
        SoundType block = CopySounds.type("block.amethyst_block");
        SoundType cluster = CopySounds.type("block.amethyst_cluster");
        chime = CopySounds.event("block.amethyst_block.chime");
        amethyst = add(r, "amethyst_block", new BlockAmethyst(block), "pickaxe");
        budding = add(r, "budding_amethyst", new BlockAmethyst.Budding(block), "pickaxe");
        stages = new BlockAmethystCluster[] {
                add(r, "small_amethyst_bud", new BlockAmethystCluster(bud("small", cluster), 3, 4, 1, false), null),
                add(r, "medium_amethyst_bud", new BlockAmethystCluster(bud("medium", cluster), 4, 3, 2, false), null),
                add(r, "large_amethyst_bud", new BlockAmethystCluster(bud("large", cluster), 5, 3, 4, false), null),
                add(r, "amethyst_cluster", new BlockAmethystCluster(cluster, 7, 3, 5, true), null),
        };
        for (int i = 0; i + 1 < stages.length; i++) {
            stages[i].next = stages[i + 1];
        }
        // Caves & Cliffs' stream D blocks that geodes are made of: calcite 0.75/0.75, smooth basalt 1.25/4.2.
        calcite = add(r, "calcite", new BlockBasic(Material.ROCK, MapColor.WHITE_STAINED_HARDENED_CLAY,
                CopySounds.type("block.calcite"), 0.75F, 0.75F), "pickaxe");
        smoothBasalt = add(r, "smooth_basalt", new BlockBasic(Material.ROCK, MapColor.BLACK,
                CopySounds.type("block.basalt"), 1.25F, 4.2F), "pickaxe");
        TrailsAndTalesBackport.LOGGER.info("Smithing: our copy of Caves & Cliffs' amethyst ({} blocks).", ALL.size());
    }

    /** 1.18's bud sound types: their own break and place, the cluster's step, hit and fall. */
    private static SoundType bud(String size, SoundType cluster) {
        return new SoundType(1.0F, 1.0F, CopySounds.event("block." + size + "_amethyst_bud.break"), cluster.getStepSound(),
                CopySounds.event("block." + size + "_amethyst_bud.place"), cluster.getHitSound(), cluster.getFallSound());
    }

    private static <B extends Block> B add(IForgeRegistry<Block> r, String name, B block, String tool) {
        Content.block(r, name, block, tool);
        ALL.add(block);
        return block;
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        if (amethyst == null) {
            return;
        }
        IForgeRegistry<Item> r = event.getRegistry();
        for (Block b : ALL) {
            Content.registerBlockItem(r, b);
        }
        shard = Content.item(r, "amethyst_shard", new Item());
        OreDictionary.registerOre("gemAmethyst", shard);
        OreDictionary.registerOre("blockAmethyst", amethyst);
        OreDictionary.registerOre("stoneCalcite", calcite);
    }

    @SubscribeEvent
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        if (amethyst == null) {
            return;
        }
        ResourceLocation id = Content.id("amethyst_block");
        event.getRegistry().register(new ShapedOreRecipe(id, new ItemStack(amethyst), "XX", "XX", 'X', shard).setRegistryName(id));
    }
}
