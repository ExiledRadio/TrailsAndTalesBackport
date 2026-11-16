package com.exiledradio.trailsandtalesbackport.smithing;

import com.exiledradio.backportcore.smithing.ArmorTrims;
import com.exiledradio.backportcore.smithing.SmithingTemplates;
import com.exiledradio.backportcore.smithing.SmithingUpgrades;
import com.exiledradio.trailsandtalesbackport.ModConfig;
import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import com.exiledradio.trailsandtalesbackport.common.Content;
import com.exiledradio.trailsandtalesbackport.common.Owners;

import net.minecraft.block.Block;
import net.minecraft.init.Items;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.registries.IForgeRegistry;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Stream Sm: 1.20's smithing templates and armor trims.
 *
 * <ul>
 * <li>The seventeen templates ({@link Templates}), each duplicating with 7 diamonds and its block, as 1.20.6's
 * recipes; a block another family mod owns (cobbled deepslate, blackstone) is used when that mod is installed, and
 * the recipe is left out without it.</li>
 * <li>Backport Core does the smithing ({@code smithing.SmithingTemplates}) and the trims ({@code smithing.ArmorTrims}):
 * this registers the netherite upgrade template, the sixteen patterns and 1.20's ten materials.</li>
 * <li>The table: this mod's 1.20 container ({@link ContainerSmithing}) on whichever smithing table is in the game -
 * Village &amp; Pillage's, Nether Update's copy, or this mod's own copy ({@link BlockSmithingTable}) when neither
 * is installed ({@link SmithingTables}).</li>
 * <li>Where templates are found: {@link SmithingLoot}. The copper and amethyst it needs: {@code smithing.copy}.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = TrailsAndTalesBackport.MOD_ID)
public final class SmithingContent {

    @Nullable
    public static BlockSmithingTable table;
    @Nullable
    static SoundEvent use;

    private SmithingContent() {
    }

    public static boolean enabled() {
        return ModConfig.smithing.enabled;
    }

    /** Our copy of the smithing table: only without Village &amp; Pillage (its owner) and Nether Update (which copies it). */
    public static boolean registersTable() {
        return enabled() && !Loader.isModLoaded(Owners.NETHER_UPDATE) && Owners.shouldRegisterCopy("smithing_table");
    }

    /** From the mod's pre-init. */
    public static void preInit() {
        if (!enabled()) {
            return;
        }
        // Our container is the 1.20 table, on every smithing table (SmithingTables).
        SmithingTemplates.declareTableSupport(TrailsAndTalesBackport.MOD_ID);
        registerMaterials();
        SmithingTables.preInit();
        com.exiledradio.trailsandtalesbackport.smithing.copy.CopperCopies.preInit();
        com.exiledradio.trailsandtalesbackport.smithing.copy.AmethystCopies.preInit();
    }

    /** From the mod's init. */
    public static void init() {
        if (enabled()) {
            NetworkRegistry.INSTANCE.registerGuiHandler(TrailsAndTalesBackport.instance, new SmithingGui());
            SmithingLoot.register();
        }
        com.exiledradio.trailsandtalesbackport.smithing.copy.CopperCopies.init();
        com.exiledradio.trailsandtalesbackport.smithing.copy.AmethystCopies.init();
    }

    /** From the mod's post-init: every netherite mod's ingot gets the ore name the netherite material reads. */
    public static void postInit() {
        if (enabled()) {
            SmithingUpgrades.registerIngots();
        }
    }

    @SubscribeEvent
    public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        if (!enabled()) {
            return;
        }
        ResourceLocation id = Content.id("block.smithing_table.use");
        use = new SoundEvent(id).setRegistryName(id);
        event.getRegistry().register(use);
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        if (registersTable()) {
            table = Content.block(event.getRegistry(), "smithing_table", new BlockSmithingTable(), "axe");
        }
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        if (!enabled()) {
            return;
        }
        IForgeRegistry<Item> r = event.getRegistry();
        if (table != null) {
            Content.registerBlockItem(r, table);
        }
        for (Templates t : Templates.values()) {
            t.item = Content.item(r, t.itemName(), new ItemSmithingTemplate(t));
            // Tricky Trials' bolt and flow templates carry the same name (its decor stream).
            OreDictionary.registerOre("armorTrimTemplate", t.item);
        }
        final Item upgrade = Templates.NETHERITE_UPGRADE.item;
        SmithingTemplates.requireForUpgrades(TrailsAndTalesBackport.MOD_ID, Ingredient.fromItem(upgrade),
                () -> ModConfig.smithing.netheriteNeedsTemplate);
        for (final Templates t : Templates.values()) {
            if (t.isTrim()) {
                ArmorTrims.registerPattern(Content.id(t.pattern), () -> t.item, Content.id(t.pattern), false);
            }
        }
    }

    @SubscribeEvent
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        if (!enabled()) {
            return;
        }
        IForgeRegistry<IRecipe> r = event.getRegistry();
        int left = 0;
        for (Templates t : Templates.values()) {
            ItemStack block = t.duplicationBlock();
            if (block.isEmpty()) {
                left++;
                continue;
            }
            shaped(r, t.itemName(), new ItemStack(t.item, 2), "#S#", "#C#", "###",
                    '#', Items.DIAMOND, 'S', t.item, 'C', block);
        }
        if (left > 0) {
            TrailsAndTalesBackport.LOGGER.info("Smithing: {} template duplication recipes wait for the mod that owns their block.", left);
        }
        if (table != null) {
            // 1.14's smithing table: two iron ingots over four planks.
            shaped(r, "smithing_table", new ItemStack(table), "@@", "##", "##", '@', "ingotIron", '#', "plankWood");
        }
    }

    private static void shaped(IForgeRegistry<IRecipe> r, String name, ItemStack out, Object... recipe) {
        ResourceLocation id = Content.id(name);
        r.register(new ShapedOreRecipe(id, out, recipe).setRegistryName(id));
    }

    // ---------------------------------------------------------------------------------------------
    // Materials
    // ---------------------------------------------------------------------------------------------

    /**
     * 1.20's {@code TrimMaterials}, in its order, with its colors: the item in the addition slot and the palette.
     * Iron, gold, diamond and netherite take their {@code _darker} palette on armor of their own kind. Copper and
     * amethyst are any mod's by ore name (Caves &amp; Cliffs' first, our copies without it); netherite is any netherite
     * mod's ingot, as Backport Core finds them.
     */
    static void registerMaterials() {
        material("quartz", item(Items.QUARTZ, 0), 0xE3D4C4, stacks(Items.QUARTZ, 0));
        material("iron", item(Items.IRON_INGOT, 0), 0xECECEC, stacks(Items.IRON_INGOT, 0))
                .darkerOn(armorOf(ItemArmor.ArmorMaterial.IRON), Content.id("iron_darker"));
        material("netherite", SmithingUpgrades::isIngot, 0x625859, SmithingUpgrades::ingots)
                .darkerOn(s -> s.getItem() instanceof ItemArmor && s.getItem().getRegistryName().getPath().contains("netherite"),
                        Content.id("netherite_darker"));
        material("redstone", item(Items.REDSTONE, 0), 0x971607, stacks(Items.REDSTONE, 0));
        material("copper", ore("ingotCopper"), 0xB4684D, ores("ingotCopper"));
        material("gold", item(Items.GOLD_INGOT, 0), 0xDEB12D, stacks(Items.GOLD_INGOT, 0))
                .darkerOn(armorOf(ItemArmor.ArmorMaterial.GOLD), Content.id("gold_darker"));
        material("emerald", item(Items.EMERALD, 0), 0x11A036, stacks(Items.EMERALD, 0));
        material("diamond", item(Items.DIAMOND, 0), 0x6EECD2, stacks(Items.DIAMOND, 0))
                .darkerOn(armorOf(ItemArmor.ArmorMaterial.DIAMOND), Content.id("diamond_darker"));
        int lapis = EnumDyeColor.BLUE.getDyeDamage();
        material("lapis", item(Items.DYE, lapis), 0x416E97, stacks(Items.DYE, lapis));
        material("amethyst", ore("gemAmethyst"), 0x9A5CC6, ores("gemAmethyst"));
        // Update Aquatic's turtle shell is drawn in vanilla's shape, so its icon takes the overlay (1.20 has its models).
        ArmorTrims.drawOnIcon(i -> i.getRegistryName() != null && Owners.UPDATE_AQUATIC.equals(i.getRegistryName().getNamespace())
                && "turtle_helmet".equals(i.getRegistryName().getPath()));
    }

    private static ArmorTrims.Material material(String name, Predicate<ItemStack> item, int color, Supplier<List<ItemStack>> examples) {
        return ArmorTrims.registerMaterial(Content.id(name), item, examples, Content.id(name), color);
    }

    private static Predicate<ItemStack> item(final Item item, final int meta) {
        return s -> s.getItem() == item && s.getMetadata() == meta;
    }

    private static Supplier<List<ItemStack>> stacks(final Item item, final int meta) {
        return () -> Collections.singletonList(new ItemStack(item, 1, meta));
    }

    private static Predicate<ItemStack> ore(final String ore) {
        return s -> {
            if (s.isEmpty()) {
                return false;
            }
            int wanted = OreDictionary.getOreID(ore);
            for (int id : OreDictionary.getOreIDs(s)) {
                if (id == wanted) {
                    return true;
                }
            }
            return false;
        };
    }

    private static Supplier<List<ItemStack>> ores(final String ore) {
        return () -> new ArrayList<ItemStack>(OreDictionary.getOres(ore, false));
    }

    private static Predicate<ItemStack> armorOf(final ItemArmor.ArmorMaterial material) {
        return s -> s.getItem() instanceof ItemArmor && ((ItemArmor) s.getItem()).getArmorMaterial() == material;
    }
}
