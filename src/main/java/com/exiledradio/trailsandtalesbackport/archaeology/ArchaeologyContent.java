package com.exiledradio.trailsandtalesbackport.archaeology;

import com.exiledradio.backportcore.ownership.BlockOwnership;
import com.exiledradio.trailsandtalesbackport.ModConfig;
import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import com.exiledradio.trailsandtalesbackport.common.Content;
import com.exiledradio.trailsandtalesbackport.common.wood.WoodKind;

import net.minecraft.advancements.Advancement;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityEgg;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.entity.projectile.EntitySnowball;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Stream A: the brush, suspicious sand and gravel, the twenty pottery sherds, the decorated pot, the six
 * archaeology loot tables and the advancements <i>Respecting the Remnants</i> and <i>Careful
 * Restoration</i>.
 *
 * <h2>For structures</h2>
 * The suspicious blocks are {@code trailsandtalesbackport:suspicious_sand} and {@code suspicious_gravel};
 * a structure places one and writes {@code LootTable} (one of {@link ArchaeologyLoot#TABLES}, or any table)
 * and {@code LootTableSeed} into its tile entity's NBT, by name, with no link to this class.
 */
@Mod.EventBusSubscriber(modid = TrailsAndTalesBackport.MOD_ID)
public final class ArchaeologyContent {

    public static final String[] SHERDS = {"angler", "archer", "arms_up", "blade", "brewer", "burn", "danger",
            "explorer", "friend", "heart", "heartbreak", "howl", "miner", "mourner", "plenty", "prize", "sheaf",
            "shelter", "skull", "snort"};

    public static final ResourceLocation SALVAGE_SHERD = Content.id("adventure/salvage_sherd");
    public static final ResourceLocation CAREFUL_RESTORATION = Content.id("adventure/craft_decorated_pot_using_only_sherds");

    private static final List<SoundEvent> SOUNDS = new ArrayList<SoundEvent>();
    static final SoundType SUSPICIOUS_SAND = WoodKind.soundType(SOUNDS, "block.suspicious_sand", 1.0F, 1.0F);
    static final SoundType SUSPICIOUS_GRAVEL = WoodKind.soundType(SOUNDS, "block.suspicious_gravel", 1.0F, 1.0F);
    static final SoundType DECORATED_POT = WoodKind.soundType(SOUNDS, "block.decorated_pot", 1.0F, 1.0F);
    static final SoundEvent POT_SHATTER = event("block.decorated_pot.shatter");
    static final SoundType DECORATED_POT_CRACKED = new SoundType(1.0F, 1.0F, POT_SHATTER, DECORATED_POT.getStepSound(),
            DECORATED_POT.getPlaceSound(), DECORATED_POT.getHitSound(), DECORATED_POT.getFallSound());
    static final SoundEvent POT_INSERT = event("block.decorated_pot.insert");
    static final SoundEvent POT_INSERT_FAIL = event("block.decorated_pot.insert_fail");
    static final SoundEvent BRUSH_GENERIC = event("item.brush.brushing.generic");
    static final SoundEvent BRUSH_SAND = event("item.brush.brushing.sand");
    static final SoundEvent BRUSH_GRAVEL = event("item.brush.brushing.gravel");
    static final SoundEvent BRUSH_SAND_COMPLETED = event("item.brush.brushing.sand.complete");
    static final SoundEvent BRUSH_GRAVEL_COMPLETED = event("item.brush.brushing.gravel.complete");

    /** All null when the stream is off. */
    public static BlockSuspicious suspiciousSand;
    public static BlockSuspicious suspiciousGravel;
    public static BlockDecoratedPot decoratedPot;
    public static ItemBrush brush;
    public static Item potItem;
    /** Sherd name ("angler") to item. */
    public static final Map<String, Item> SHERD_ITEMS = new LinkedHashMap<String, Item>();

    private ArchaeologyContent() {
    }

    private static SoundEvent event(String name) {
        SoundEvent e = new SoundEvent(Content.id(name));
        e.setRegistryName(Content.id(name));
        SOUNDS.add(e);
        return e;
    }

    public static boolean enabled() {
        return ModConfig.archaeology.enabled;
    }

    public static void preInit() {
        if (!enabled()) {
            return;
        }
        List<String> names = new ArrayList<String>();
        names.add("brush");
        names.add("suspicious_sand");
        names.add("suspicious_gravel");
        names.add("decorated_pot");
        for (String s : SHERDS) {
            names.add(s + "_pottery_sherd");
        }
        BlockOwnership.declare(TrailsAndTalesBackport.MOD_ID, names.toArray(new String[0]));
        ArchaeologyLoot.register();
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        if (!enabled()) {
            return;
        }
        suspiciousSand = Content.block(event.getRegistry(), "suspicious_sand", new BlockSuspicious(
                Blocks.SAND.getDefaultState(), MapColor.SAND, SUSPICIOUS_SAND, BRUSH_SAND, BRUSH_SAND_COMPLETED), "shovel");
        suspiciousGravel = Content.block(event.getRegistry(), "suspicious_gravel", new BlockSuspicious(
                Blocks.GRAVEL.getDefaultState(), MapColor.STONE, SUSPICIOUS_GRAVEL, BRUSH_GRAVEL, BRUSH_GRAVEL_COMPLETED), "shovel");
        decoratedPot = Content.block(event.getRegistry(), "decorated_pot", new BlockDecoratedPot(
                DECORATED_POT, DECORATED_POT_CRACKED, POT_INSERT, POT_INSERT_FAIL), null);
        GameRegistry.registerTileEntity(TileSuspicious.class, Content.id("brushable_block"));
        GameRegistry.registerTileEntity(TileDecoratedPot.class, Content.id("decorated_pot"));
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        if (!enabled()) {
            return;
        }
        Content.registerBlockItem(event.getRegistry(), suspiciousSand);
        Content.registerBlockItem(event.getRegistry(), suspiciousGravel);
        potItem = Content.blockItem(event.getRegistry(), decoratedPot, new ItemDecoratedPot(decoratedPot));
        brush = Content.item(event.getRegistry(), "brush", new ItemBrush(BRUSH_GENERIC));
        for (String s : SHERDS) {
            Item sherd = Content.item(event.getRegistry(), s + "_pottery_sherd", new Item() {
                @Override
                public EnumRarity getRarity(ItemStack stack) {
                    return EnumRarity.UNCOMMON;
                }
            });
            SHERD_ITEMS.put(s, sherd);
            OreDictionary.registerOre(RecipeDecoratedPot.SHERD_ORE, sherd);
        }
    }

    @SubscribeEvent
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        if (!enabled()) {
            return;
        }
        // 1.20's brush: feather, copper ingot, stick. Copper from Caves & Cliffs (or any mod's ingotCopper).
        ResourceLocation id = Content.id("brush");
        event.getRegistry().register(new ShapedOreRecipe(id, new ItemStack(brush), "X", "#", "I",
                'X', Items.FEATHER, '#', "ingotCopper", 'I', Items.STICK).setRegistryName(id));
        id = Content.id("decorated_pot_simple");
        event.getRegistry().register(new ShapedOreRecipe(id, new ItemStack(potItem), " # ", "# #", " # ",
                '#', Items.BRICK).setRegistryName(id));
        event.getRegistry().register(new RecipeDecoratedPot(potItem).setRegistryName(Content.id("decorated_pot")));
    }

    @SubscribeEvent
    public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        if (enabled()) {
            for (SoundEvent e : SOUNDS) {
                event.getRegistry().register(e);
            }
        }
    }

    // --- loot and advancements -------------------------------------------------------------------

    @SubscribeEvent
    public static void onLootTableLoad(LootTableLoadEvent event) {
        if (enabled()) {
            ArchaeologyLoot.onLoad(event);
        }
    }

    /** A player rolled a suspicious block's loot table: the table's criterion of <i>Respecting the Remnants</i>. */
    static void lootGenerated(EntityPlayerMP player, ResourceLocation table) {
        if (TrailsAndTalesBackport.MOD_ID.equals(table.getNamespace()) && table.getPath().startsWith("archaeology/")) {
            grant(player, SALVAGE_SHERD, table.getPath().substring("archaeology/".length()));
        }
    }

    static void grant(EntityPlayerMP player, ResourceLocation id, String criterion) {
        Advancement advancement = player.getServer().getAdvancementManager().getAdvancement(id);
        if (advancement != null && advancement.getCriteria().containsKey(criterion)) {
            player.getAdvancements().grantCriterion(advancement, criterion);
        }
    }

    @SubscribeEvent
    public static void onCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (potItem != null && event.player instanceof EntityPlayerMP && event.crafting.getItem() == potItem
                && RecipeDecoratedPot.onlySherds(event.craftMatrix)) {
            grant((EntityPlayerMP) event.player, CAREFUL_RESTORATION, "pot_crafted_using_only_sherds");
        }
    }

    // --- brushing: the other hand waits -----------------------------------------------------------

    private static boolean brushing(EntityPlayer player) {
        return brush != null && player.isHandActive() && player.getActiveItemStack().getItem() == brush;
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() == EnumHand.OFF_HAND && brushing(event.getEntityPlayer())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (brushing(event.getEntityPlayer())) {
            event.setCanceled(true);
        }
    }

    // --- pots shattering -------------------------------------------------------------------------

    /**
     * Breaking a pot with a pot-breaking tool cracks it first (vanilla's {@code playerWillDestroy}), so its
     * break effect is the shatter and its drops are the sherds. Lowest priority, after anything that cancels.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBreak(BlockEvent.BreakEvent event) {
        IBlockState state = event.getState();
        if (decoratedPot == null || state.getBlock() != decoratedPot || state.getValue(BlockDecoratedPot.CRACKED)) {
            return;
        }
        if (BlockDecoratedPot.shatters(event.getPlayer().getHeldItemMainhand())) {
            event.getWorld().setBlockState(event.getPos(), state.withProperty(BlockDecoratedPot.CRACKED, true), 4);
        }
    }

    /** Arrows, snowballs, eggs, fireballs, wither skulls and tridents shatter pots (1.20's impact projectiles). */
    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        RayTraceResult ray = event.getRayTraceResult();
        Entity projectile = event.getEntity();
        if (decoratedPot == null || ray == null || ray.typeOfHit != RayTraceResult.Type.BLOCK || projectile.world.isRemote) {
            return;
        }
        World world = projectile.world;
        BlockPos pos = ray.getBlockPos();
        IBlockState state = world.getBlockState(pos);
        if (state.getBlock() != decoratedPot || !isImpactProjectile(projectile) || !mayInteract(projectile, world, pos)) {
            return;
        }
        decoratedPot.shatter(world, pos, state, projectile);
    }

    static boolean isImpactProjectile(Entity e) {
        if (e instanceof EntityArrow || e instanceof EntitySnowball || e instanceof EntityEgg || e instanceof EntityFireball) {
            return true;
        }
        EntityEntry entry = EntityRegistry.getEntry(e.getClass());
        return entry != null && entry.getRegistryName() != null && "trident".equals(entry.getRegistryName().getPath());
    }

    /** Vanilla's {@code Projectile.mayInteract}: a player's may edit there; a mob's only with mob griefing on. */
    static boolean mayInteract(Entity projectile, World world, BlockPos pos) {
        Entity owner = null;
        if (projectile instanceof EntityArrow) {
            owner = ((EntityArrow) projectile).shootingEntity;
        } else if (projectile instanceof EntityThrowable) {
            owner = ((EntityThrowable) projectile).getThrower();
        } else if (projectile instanceof EntityFireball) {
            owner = ((EntityFireball) projectile).shootingEntity;
        }
        if (owner instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) owner;
            return world.isBlockModifiable(player, pos) && player.isAllowEdit();
        }
        return owner == null || world.getGameRules().getBoolean("mobGriefing");
    }

    // --- placing them (for this mod's structure stream) -------------------------------------------

    /**
     * Places suspicious sand ({@code gravel} false) or gravel holding a loot table, with the given flags.
     * Other streams and mods can do the same by name: the block, then {@code LootTable}/{@code LootTableSeed}
     * in its tile entity's NBT.
     *
     * @return false when the stream is off or the block could not be set
     */
    public static boolean placeSuspicious(World world, BlockPos pos, boolean gravel, ResourceLocation table, long seed, int flags) {
        Block block = gravel ? suspiciousGravel : suspiciousSand;
        if (block == null || !world.setBlockState(pos, block.getDefaultState(), flags)) {
            return false;
        }
        net.minecraft.tileentity.TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileSuspicious) {
            ((TileSuspicious) te).setLootTable(table, seed);
            return true;
        }
        return false;
    }

    /** @return this mod's sherd item for a name such as "angler", or null */
    public static Item sherd(String name) {
        return SHERD_ITEMS.get(name);
    }
}
