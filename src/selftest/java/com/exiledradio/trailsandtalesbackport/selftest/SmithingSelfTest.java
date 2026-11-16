package com.exiledradio.trailsandtalesbackport.selftest;

import com.exiledradio.backportcore.smithing.ArmorTrims;
import com.exiledradio.backportcore.smithing.SmithingTemplates;
import com.exiledradio.backportcore.smithing.SmithingUpgrades;
import com.exiledradio.trailsandtalesbackport.ModConfig;
import com.exiledradio.trailsandtalesbackport.common.Content;
import com.exiledradio.trailsandtalesbackport.common.Owners;
import com.exiledradio.trailsandtalesbackport.smithing.ContainerSmithing;
import com.exiledradio.trailsandtalesbackport.smithing.SmithingContent;
import com.exiledradio.trailsandtalesbackport.smithing.SmithingTables;
import com.exiledradio.trailsandtalesbackport.smithing.Templates;
import com.exiledradio.trailsandtalesbackport.smithing.copy.AmethystCopies;
import com.exiledradio.trailsandtalesbackport.smithing.copy.BlockAmethystCluster;
import com.exiledradio.trailsandtalesbackport.smithing.copy.CopperCopies;
import com.exiledradio.trailsandtalesbackport.smithing.copy.CopperOreGenerator;
import com.exiledradio.trailsandtalesbackport.smithing.copy.GeodeGenerator;

import net.minecraft.advancements.Advancement;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.inventory.ClickType;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.LootTable;
import net.minecraftforge.oredict.OreDictionary;

import java.util.List;
import java.util.Random;

/** Stream Sm: templates, trims, the 1.20 table, template loot, and the copper and amethyst copies. */
public final class SmithingSelfTest implements SelfTest {

    @Override
    public String name() {
        return "smithing";
    }

    @Override
    public void run(Check c) {
        templates(c);
        table(c);
        trims(c);
        upgrades(c);
        quickMove(c);
        loot(c);
        copper(c);
        amethyst(c);
    }

    private static ItemStack t(Templates t) {
        return new ItemStack(t.item());
    }

    private void templates(Check c) {
        for (Templates t : Templates.values()) {
            c.ok(t.item() != null && Content.id(t.itemName()).equals(t.item().getRegistryName()), "registered " + t.itemName());
        }
        c.eq(17, OreDictionary.getOres("armorTrimTemplate", false).size(), "17 templates are armorTrimTemplate");
        c.eq("item.trailsandtalesbackport.smithing_template", t(Templates.DUNE).getTranslationKey(), "one name for all");
        c.ok(SmithingTemplates.isTemplate(t(Templates.COAST)) && SmithingTemplates.isTemplate(t(Templates.NETHERITE_UPGRADE)), "templates go in the template slot");
        c.ok(SmithingTemplates.isTrimTemplate(t(Templates.WILD)) && !SmithingTemplates.isTrimTemplate(t(Templates.NETHERITE_UPGRADE)), "trim vs upgrade template");
        c.eq(16, ArmorTrims.patterns().size(), "16 patterns");
        c.eq(10, ArmorTrims.materials().size(), "10 materials");
        ItemStack d = new ItemStack(Items.DIAMOND);
        Crafting.expect(c, Crafting.grid(3, 3, d, t(Templates.DUNE), d, d, new ItemStack(Blocks.SANDSTONE), d, d, d, d),
                new ItemStack(Templates.DUNE.item(), 2), "dune duplicates with sandstone");
        Crafting.expect(c, Crafting.grid(3, 3, d, t(Templates.NETHERITE_UPGRADE), d, d, new ItemStack(Blocks.NETHERRACK), d, d, d, d),
                new ItemStack(Templates.NETHERITE_UPGRADE.item(), 2), "netherite upgrade duplicates with netherrack");
        Crafting.expect(c, Crafting.grid(3, 3, d, t(Templates.HOST), d, d, new ItemStack(Blocks.HARDENED_CLAY), d, d, d, d),
                new ItemStack(Templates.HOST.item(), 2), "host duplicates with terracotta");
        c.ok(net.minecraft.item.crafting.CraftingManager.findMatchingResult(
                Crafting.grid(3, 3, d, t(Templates.SPIRE), d, d, new ItemStack(Blocks.SANDSTONE), d, d, d, d), c.world).isEmpty(),
                "spire does not duplicate with sandstone");
        c.ok(c.world.getMinecraftServer().getAdvancementManager().getAdvancement(Content.id("adventure/trim_with_all_exclusive_armor_patterns")) != null,
                "Smithing with Style loaded");
    }

    private void table(Check c) {
        boolean own = !Owners.present(Owners.VILLAGE_AND_PILLAGE) && !Owners.present(Owners.NETHER_UPDATE);
        c.eq(own, SmithingContent.table != null, "our smithing table copy only without Village & Pillage and Nether Update");
        c.ok(SmithingTemplates.tables().contains("trailsandtalesbackport"), "our table declared");
        c.ok(SmithingTemplates.templateSlot(), "template slot on");
        if (SmithingContent.table != null) {
            ItemStack i = new ItemStack(Items.IRON_INGOT), p = new ItemStack(Blocks.PLANKS, 1, 3);
            Crafting.expect(c, Crafting.grid(2, 3, i, i, p, p, p, p), new ItemStack(SmithingContent.table), "2 iron over 4 planks");
            c.ok(SmithingTables.isTable(SmithingContent.table), "our table opens the container");
        }
    }

    private ContainerSmithing container(Check c, EntityPlayerMP player) {
        BlockPos pos = c.area().add(4, 0, 4);
        if (SmithingContent.table != null) {
            c.world.setBlockState(pos, SmithingContent.table.getDefaultState());
        }
        player.setPosition(pos.getX() + 0.5, pos.getY(), pos.getZ() - 1.5);
        return new ContainerSmithing(player.inventory, c.world, pos);
    }

    private static void put(ContainerSmithing m, ItemStack template, ItemStack base, ItemStack addition) {
        m.getSlot(ContainerSmithing.SLOT_TEMPLATE).putStack(template);
        m.getSlot(ContainerSmithing.SLOT_BASE).putStack(base);
        m.getSlot(ContainerSmithing.SLOT_ADDITION).putStack(addition);
    }

    private void trims(Check c) {
        EntityPlayerMP player = Players.real(c.world, "smith");
        ContainerSmithing m = container(c, player);
        c.ok(m.canInteractWith(player), "container usable at the table");
        ItemStack chest = new ItemStack(Items.IRON_CHESTPLATE);
        chest.addEnchantment(Enchantments.PROTECTION, 3);
        chest.setItemDamage(40);
        put(m, new ItemStack(Templates.COAST.item(), 2), chest, new ItemStack(Items.GOLD_INGOT, 5));
        ItemStack out = m.getSlot(ContainerSmithing.SLOT_RESULT).getStack().copy();
        ArmorTrims.Trim trim = ArmorTrims.get(out);
        if (c.ok(trim != null, "coast + iron chestplate + gold makes a trim")) {
            c.eq("trailsandtalesbackport:coast", trim.pattern.id.toString(), "pattern");
            c.eq("trailsandtalesbackport:gold", trim.material.id.toString(), "material");
            NBTTagCompound tag = out.getTagCompound().getCompoundTag("Trim");
            c.eq("trailsandtalesbackport:coast", tag.getString("pattern"), "NBT Trim.pattern");
            c.eq(3, net.minecraft.enchantment.EnchantmentHelper.getEnchantmentLevel(Enchantments.PROTECTION, out), "enchantment kept");
            c.eq(40, out.getItemDamage(), "damage kept");
            c.eq(Content.id("gold"), trim.material.paletteFor(out), "gold on iron: plain palette");
        }
        // Take it: one of each used up, advancement granted.
        m.slotClick(ContainerSmithing.SLOT_RESULT, 0, ClickType.QUICK_MOVE, player);
        c.eq(1, m.getSlot(ContainerSmithing.SLOT_TEMPLATE).getStack().getCount(), "template used up");
        c.ok(m.getSlot(ContainerSmithing.SLOT_BASE).getStack().isEmpty(), "base used up");
        c.eq(4, m.getSlot(ContainerSmithing.SLOT_ADDITION).getStack().getCount(), "one addition used");
        c.ok(contains(player.inventory.mainInventory, Items.IRON_CHESTPLATE), "trimmed chestplate in the inventory");
        Advancement any = c.world.getMinecraftServer().getAdvancementManager().getAdvancement(Content.id("adventure/trim_with_any_armor_pattern"));
        c.ok(any != null && player.getAdvancements().getProgress(any).isDone(), "Crafting a New Look");

        // The same trim again makes nothing (1.20), the cross shows.
        put(m, t(Templates.COAST), out.copy(), new ItemStack(Items.GOLD_INGOT));
        c.ok(m.getSlot(ContainerSmithing.SLOT_RESULT).getStack().isEmpty(), "same trim again: nothing");
        c.ok(m.hasRecipeError(), "recipe error shown");
        put(m, t(Templates.COAST), out.copy(), new ItemStack(Items.IRON_INGOT));
        ArmorTrims.Trim iron = ArmorTrims.get(m.getSlot(ContainerSmithing.SLOT_RESULT).getStack());
        c.ok(iron != null && "trailsandtalesbackport:iron".equals(iron.material.id.toString()), "a new material replaces the trim");
        if (iron != null) {
            c.eq(Content.id("iron_darker"), iron.material.paletteFor(out), "iron on iron armor: darker palette");
        }
        int lapis = EnumDyeColor.BLUE.getDyeDamage();
        put(m, t(Templates.SILENCE), new ItemStack(Items.LEATHER_BOOTS), new ItemStack(Items.DYE, 1, lapis));
        ArmorTrims.Trim l = ArmorTrims.get(m.getSlot(ContainerSmithing.SLOT_RESULT).getStack());
        c.ok(l != null && "trailsandtalesbackport:lapis".equals(l.material.id.toString()), "lapis lazuli on leather boots");
        put(m, t(Templates.WILD), new ItemStack(Items.DIAMOND_SWORD), new ItemStack(Items.DIAMOND));
        c.ok(m.getSlot(ContainerSmithing.SLOT_RESULT).getStack().isEmpty(), "a sword takes no trim");
        c.ok(!ContainerSmithing.accepts(ContainerSmithing.SLOT_BASE, new ItemStack(Blocks.DIRT)), "dirt goes nowhere");
        c.ok(!ContainerSmithing.accepts(ContainerSmithing.SLOT_ADDITION, new ItemStack(Items.COAL)), "coal is no material");

        // Exclusive pattern counts toward Smithing with Style.
        put(m, t(Templates.SILENCE), new ItemStack(Items.GOLDEN_HELMET), new ItemStack(Items.REDSTONE));
        m.slotClick(ContainerSmithing.SLOT_RESULT, 0, ClickType.QUICK_MOVE, player);
        Advancement style = c.world.getMinecraftServer().getAdvancementManager().getAdvancement(Content.id("adventure/trim_with_all_exclusive_armor_patterns"));
        c.ok(style != null && player.getAdvancements().getProgress(style).getCriterionProgress("silence_armor_trim_smithing_template") != null
                && player.getAdvancements().getProgress(style).getCriterionProgress("silence_armor_trim_smithing_template").isObtained(), "silence counted");
        c.ok(style != null && !player.getAdvancements().getProgress(style).isDone(), "not done with one of eight");

        // An unknown trim is kept and ignored.
        ItemStack odd = new ItemStack(Items.IRON_HELMET);
        NBTTagCompound tt = new NBTTagCompound();
        tt.setString("pattern", "someoldmod:zigzag");
        tt.setString("material", "trailsandtalesbackport:iron");
        odd.setTagInfo("Trim", tt);
        c.ok(ArmorTrims.get(odd) == null && odd.getTagCompound().hasKey("Trim"), "unknown pattern ignored, tag kept");
        m.onContainerClosed(player);
    }

    private void upgrades(Check c) {
        EntityPlayerMP player = Players.real(c.world, "upgrader");
        ContainerSmithing m = container(c, player);
        // No netherite mod in the dev run: a stand-in upgrade, diamond sword + nether star = golden sword.
        SmithingUpgrades.add(Items.DIAMOND_SWORD, Ingredient.fromItem(Items.NETHER_STAR), new ItemStack(Items.GOLDEN_SWORD), "selftest");
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.addEnchantment(Enchantments.SHARPNESS, 5);
        sword.setStackDisplayName("Blade");
        c.ok(SmithingTemplates.upgradesNeedTemplate(), "upgrades need the template (default)");
        c.ok(SmithingUpgrades.find(sword, new ItemStack(Items.NETHER_STAR)) == null, "two-slot find answers nothing");
        put(m, ItemStack.EMPTY, sword.copy(), new ItemStack(Items.NETHER_STAR));
        c.ok(m.getSlot(ContainerSmithing.SLOT_RESULT).getStack().isEmpty(), "no template, no upgrade");
        put(m, t(Templates.NETHERITE_UPGRADE), sword.copy(), new ItemStack(Items.NETHER_STAR));
        ItemStack out = m.getSlot(ContainerSmithing.SLOT_RESULT).getStack();
        c.eq(Items.GOLDEN_SWORD, out.getItem(), "upgrade with the template");
        c.eq(5, net.minecraft.enchantment.EnchantmentHelper.getEnchantmentLevel(Enchantments.SHARPNESS, out), "enchantment carried over");
        c.eq("Blade", out.getDisplayName(), "name carried over");
        put(m, t(Templates.COAST), sword.copy(), new ItemStack(Items.NETHER_STAR));
        c.ok(m.getSlot(ContainerSmithing.SLOT_RESULT).getStack().isEmpty(), "a trim template does no upgrade");

        ModConfig.smithing.netheriteNeedsTemplate = false;
        try {
            c.ok(!SmithingTemplates.upgradesNeedTemplate(), "setting off");
            SmithingTemplates.Result r = SmithingTemplates.find(ItemStack.EMPTY, sword, new ItemStack(Items.NETHER_STAR));
            c.ok(r != null && !r.consumesTemplate(), "setting off: upgrade without the template");
            c.ok(SmithingUpgrades.find(sword, new ItemStack(Items.NETHER_STAR)) != null, "setting off: two-slot find works again");
        } finally {
            ModConfig.smithing.netheriteNeedsTemplate = true;
        }
        m.onContainerClosed(player);
    }

    private void quickMove(Check c) {
        EntityPlayerMP player = Players.real(c.world, "sorter");
        ContainerSmithing m = container(c, player);
        player.inventory.mainInventory.set(9, t(Templates.EYE));
        player.inventory.mainInventory.set(10, new ItemStack(Items.DIAMOND_LEGGINGS));
        player.inventory.mainInventory.set(11, new ItemStack(Items.EMERALD, 3));
        // Container slot index of inventory slot 9 is 4 (after the four smithing slots).
        m.transferStackInSlot(player, 4);
        m.transferStackInSlot(player, 5);
        m.transferStackInSlot(player, 6);
        c.eq(Templates.EYE.item(), m.getSlot(ContainerSmithing.SLOT_TEMPLATE).getStack().getItem(), "shift-click: template to the template slot");
        c.eq(Items.DIAMOND_LEGGINGS, m.getSlot(ContainerSmithing.SLOT_BASE).getStack().getItem(), "shift-click: armor to the base slot");
        c.eq(Items.EMERALD, m.getSlot(ContainerSmithing.SLOT_ADDITION).getStack().getItem(), "shift-click: emerald to the addition slot");
        c.ok(ArmorTrims.get(m.getSlot(ContainerSmithing.SLOT_RESULT).getStack()) != null, "and they make a trim");
        m.onContainerClosed(player);
        c.ok(contains(player.inventory.mainInventory, Items.DIAMOND_LEGGINGS), "closing returns the inputs");
    }

    private void loot(Check c) {
        expectRate(c, "minecraft:chests/desert_pyramid", Templates.DUNE, 1.0 / 7, 2);
        expectRate(c, "minecraft:chests/stronghold_library", Templates.EYE, 1.0, 1);
        expectRate(c, "minecraft:chests/woodland_mansion", Templates.VEX, 0.5, 1);
        // Owners absent in the dev run: fallback loot at the owners' odds.
        if (!Owners.present(Owners.VILLAGE_AND_PILLAGE)) {
            expectRate(c, "minecraft:chests/woodland_mansion", Templates.SENTRY, 0.25, 2);
        }
        if (!Owners.present(Owners.NETHER_UPDATE)) {
            expectRate(c, "minecraft:chests/nether_bridge", Templates.NETHERITE_UPGRADE, 0.1, 1);
            expectRate(c, "minecraft:chests/nether_bridge", Templates.SNOUT, 1.0 / 12, 1);
        }
        expectRate(c, "minecraft:chests/nether_bridge", Templates.RIB, 1.0 / 15, 1);
        if (!Owners.present(Owners.WILD_UPDATE)) {
            expectRate(c, "minecraft:chests/abandoned_mineshaft", Templates.WARD, 4.0 / 80, 1);
        }
        if (!Owners.present(Owners.UPDATE_AQUATIC)) {
            expectRate(c, "minecraft:chests/simple_dungeon", Templates.COAST, 1.0 / 6, 2);
        }
    }

    private void expectRate(Check c, String table, Templates t, double chance, int count) {
        LootTable lt = c.world.getLootTableManager().getLootTableFromLocation(new ResourceLocation(table));
        Random random = new Random(1);
        int hits = 0, n = 2000, items = 0;
        for (int i = 0; i < n; i++) {
            LootContext ctx = new LootContext.Builder(c.world).build();
            boolean hit = false;
            for (ItemStack s : lt.generateLootForPools(random, ctx)) {
                if (s.getItem() == t.item()) {
                    hit = true;
                    items += s.getCount();
                }
            }
            if (hit) {
                hits++;
            }
        }
        double rate = hits / (double) n;
        c.ok(Math.abs(rate - chance) < Math.max(0.03, chance * 0.2), String.format("%s in %s: %.3f (want %.3f)", t.pattern, table, rate, chance));
        c.ok(hits == 0 || items == hits * count, t.pattern + " count " + count);
    }

    private void copper(Check c) {
        boolean copies = !Owners.present(Owners.CAVES_AND_CLIFFS) && !net.minecraftforge.fml.common.Loader.isModLoaded("trickytrialsbackport");
        c.eq(copies, CopperCopies.ore != null, "copper copy only without Caves & Cliffs and Tricky Trials");
        if (CopperCopies.ore == null) {
            return;
        }
        c.eq(1, CopperCopies.ore.getHarvestLevel(CopperCopies.ore.getDefaultState()), "stone pickaxe");
        c.eq(CopperCopies.raw, CopperCopies.ore.getItemDropped(CopperCopies.ore.getDefaultState(), new Random(), 0), "drops raw copper");
        c.ok(ItemStack.areItemsEqual(new ItemStack(CopperCopies.ingot), FurnaceRecipes.instance().getSmeltingResult(new ItemStack(CopperCopies.raw))), "raw copper smelts to an ingot");
        c.ok(ArmorTrims.material(new ItemStack(CopperCopies.ingot)) != null, "our copper ingot is the copper material");
        Item brush = Owners.ours("brush");
        if (brush != null) {
            Crafting.expect(c, Crafting.grid(1, 3, new ItemStack(Items.FEATHER), new ItemStack(CopperCopies.ingot), new ItemStack(Items.STICK)),
                    new ItemStack(brush), "the brush takes our copper");
        }
        BlockPos at = c.area();
        c.fill(at, at.add(15, 15, 15), Blocks.STONE.getDefaultState());
        int placed = CopperOreGenerator.vein(c.world, new Random(3), at.add(8, 8, 8), 10);
        c.ok(placed > 0, "a vein places copper ore in stone (" + placed + ")");
    }

    private void amethyst(Check c) {
        c.eq(!Owners.present(Owners.CAVES_AND_CLIFFS), AmethystCopies.amethyst != null, "amethyst copy only without Caves & Cliffs");
        if (AmethystCopies.amethyst == null) {
            return;
        }
        c.ok(ArmorTrims.material(new ItemStack(AmethystCopies.shard)) != null, "our shard is the amethyst material");
        ItemStack s = new ItemStack(AmethystCopies.shard);
        Crafting.expect(c, Crafting.grid(2, 2, s, s, s, s), new ItemStack(AmethystCopies.amethyst), "4 shards make a block");
        NonNullList<ItemStack> drops = NonNullList.create();
        IBlockState cluster = AmethystCopies.stages[3].getDefaultState();
        AmethystCopies.stages[3].getDrops(drops, c.world, BlockPos.ORIGIN, cluster, 0);
        c.ok(!drops.isEmpty() && drops.get(0).getItem() == AmethystCopies.shard, "the cluster drops shards");

        BlockPos at = c.area();
        c.fill(at, at.add(31, 31, 31), Blocks.STONE.getDefaultState());
        boolean geode = GeodeGenerator.place(c.world, new Random(5), at.add(8, 8, 8), at.getX(), at.getZ());
        int amethyst = 0, calcite = 0, basalt = 0;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = 0; x < 32; x++) {
            for (int y = 0; y < 32; y++) {
                for (int z = 0; z < 32; z++) {
                    IBlockState st = c.world.getBlockState(p.setPos(at.getX() + x, at.getY() + y, at.getZ() + z));
                    if (st.getBlock() == AmethystCopies.amethyst || st.getBlock() == AmethystCopies.budding) {
                        amethyst++;
                    } else if (st.getBlock() == AmethystCopies.calcite) {
                        calcite++;
                    } else if (st.getBlock() == AmethystCopies.smoothBasalt) {
                        basalt++;
                    }
                }
            }
        }
        c.ok(geode && amethyst > 0 && calcite > 0 && basalt > 0,
                "a geode: " + amethyst + " amethyst, " + calcite + " calcite, " + basalt + " smooth basalt");

        // Budding amethyst grows a bud into the air beside it.
        BlockPos b = c.area();
        c.world.setBlockState(b, AmethystCopies.budding.getDefaultState());
        Random always = new Random() {
            @Override
            public int nextInt(int bound) {
                return bound == 6 ? 1 : 0; // the up face, and the one-in-five
            }
        };
        AmethystCopies.budding.randomTick(c.world, b, AmethystCopies.budding.getDefaultState(), always);
        IBlockState up = c.world.getBlockState(b.up());
        c.ok(up.getBlock() == AmethystCopies.stages[0] && up.getValue(BlockAmethystCluster.FACING) == net.minecraft.util.EnumFacing.UP,
                "budding amethyst grows a small bud");
    }

    private static boolean contains(List<ItemStack> list, Item item) {
        for (ItemStack s : list) {
            if (s.getItem() == item) {
                return true;
            }
        }
        return false;
    }
}
