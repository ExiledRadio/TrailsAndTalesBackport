package com.exiledradio.trailsandtalesbackport.sculk;

import com.exiledradio.backportcore.gameevent.GameEventEmitters;
import com.exiledradio.backportcore.gameevent.GameEventTags;
import com.exiledradio.backportcore.gameevent.GameEvents;
import com.exiledradio.backportcore.gameevent.SensorActivation;
import com.exiledradio.trailsandtalesbackport.ModConfig;
import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import com.exiledradio.trailsandtalesbackport.common.Content;
import com.exiledradio.trailsandtalesbackport.common.Owners;
import com.exiledradio.trailsandtalesbackport.common.wood.WoodKind;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemHoe;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;

import java.util.ArrayList;
import java.util.List;

/**
 * Stream Sc: the calibrated sculk sensor, this mod's copy of the sculk sensor when Wild Update Backport (its
 * owner) is absent, and 1.20's vibration rules for the whole game (Backport Core's game events, required as
 * "1.20" while this stream is on).
 *
 * <ul>
 *   <li>Recipe (1.20): three amethyst shards ({@code gemAmethyst}: Caves &amp; Cliffs Backport's, or this mod's
 *       copy when it has one) over and beside a sculk sensor (Wild Update Backport's, or ours).</li>
 *   <li>Amethyst resonance ({@link Resonance}) for every backport's sensors.</li>
 *   <li>Hoes mine both sensors faster (1.20's {@code #mineable/hoe}); sneaking players still step on them.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = TrailsAndTalesBackport.MOD_ID)
public final class SensorContent {

    public static final Material MATERIAL = new Material(MapColor.CYAN);

    private static final List<SoundEvent> SOUNDS = new ArrayList<SoundEvent>();
    static final SoundType SENSOR_SOUND = WoodKind.soundType(SOUNDS, "block.sculk_sensor", 1.0F, 1.0F);
    static final SoundEvent CLICKING = event("block.sculk_sensor.clicking");
    static final SoundEvent CLICKING_STOP = event("block.sculk_sensor.clicking_stop");
    static final SoundEvent RESONATE = event("block.amethyst_block.resonate");

    /** Null when the stream is off. */
    public static BlockCalibratedSculkSensor calibrated;
    /** Our sculk sensor: null when Wild Update Backport registers its own (or the stream is off). */
    public static BlockSculkSensor sensor;

    private SensorContent() {
    }

    private static SoundEvent event(String name) {
        SoundEvent e = new SoundEvent(Content.id(name));
        e.setRegistryName(Content.id(name));
        SOUNDS.add(e);
        return e;
    }

    public static boolean enabled() {
        return ModConfig.calibratedSculkSensor.enabled;
    }

    public static void preInit() {
        // This mod's boats and rafts place an entity (heard with any backport's game events on).
        GameEventEmitters.addEntityPlacer(stack -> stack.getItem().getClass().getName()
                .startsWith("com.exiledradio.trailsandtalesbackport.boat."));
        if (!enabled()) {
            return;
        }
        // 1.20's game events and vibration rules, for every backport's listeners (Backport Core's gameevent).
        GameEvents.require("1.20", SensorContent::enabled);
        SensorActivation.addHandler(Resonance::resonate);
    }

    /** Every mod's blocks exist: the amethyst blocks that resonate. */
    public static void init() {
        if (!enabled()) {
            return;
        }
        for (String name : new String[] {Owners.CAVES_AND_CLIFFS + ":amethyst_block", TrailsAndTalesBackport.MOD_ID + ":amethyst_block"}) {
            ResourceLocation id = new ResourceLocation(name);
            Block block = ForgeRegistries.BLOCKS.getValue(id);
            if (block != null && id.equals(block.getRegistryName())) {
                GameEventTags.addResonator(block);
            }
        }
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        if (!enabled()) {
            return;
        }
        if (Owners.shouldRegisterCopy("sculk_sensor")) {
            sensor = Content.block(event.getRegistry(), "sculk_sensor", new BlockSculkSensor(), null);
            GameRegistry.registerTileEntity(TileSculkSensor.class, Content.id("sculk_sensor"));
        }
        calibrated = Content.block(event.getRegistry(), "calibrated_sculk_sensor", new BlockCalibratedSculkSensor(), null);
        GameRegistry.registerTileEntity(TileCalibratedSculkSensor.class, Content.id("calibrated_sculk_sensor"));
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        if (!enabled()) {
            return;
        }
        if (sensor != null) {
            Content.registerBlockItem(event.getRegistry(), sensor);
        }
        Content.registerBlockItem(event.getRegistry(), calibrated);
    }

    /** @return the sculk sensor item the recipe takes: Wild Update Backport's, or ours */
    public static Item sensorItem() {
        Item theirs = Owners.theirs(Owners.WILD_UPDATE, "sculk_sensor");
        if (theirs != null) {
            return theirs;
        }
        return sensor == null ? null : Item.getItemFromBlock(sensor);
    }

    /** Low priority: other mods' sensors and amethyst are registered by then. */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        if (!enabled()) {
            return;
        }
        Item sensors = sensorItem();
        if (sensors == null) {
            TrailsAndTalesBackport.LOGGER.warn("No sculk sensor in this game: the calibrated sculk sensor has no recipe.");
            return;
        }
        Object shard = amethystShard();
        if (shard == null) {
            TrailsAndTalesBackport.LOGGER.warn("No amethyst shard in this game: the calibrated sculk sensor has no recipe.");
            return;
        }
        ResourceLocation id = Content.id("calibrated_sculk_sensor");
        event.getRegistry().register(new ShapedOreRecipe(id, new ItemStack(calibrated), " # ", "#X#", '#', shard, 'X', sensors)
                .setRegistryName(id));
    }

    /** Caves &amp; Cliffs Backport's shard (or this mod's copy) by name, else any mod's {@code gemAmethyst}. */
    static Object amethystShard() {
        Item shard = Owners.item(Owners.CAVES_AND_CLIFFS, "amethyst_shard");
        if (shard != null) {
            return shard;
        }
        return OreDictionary.doesOreNameExist("gemAmethyst") && !OreDictionary.getOres("gemAmethyst").isEmpty() ? "gemAmethyst" : null;
    }

    @SubscribeEvent
    public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        if (enabled()) {
            for (SoundEvent e : SOUNDS) {
                event.getRegistry().register(e);
            }
        }
    }

    // ---------------------------------------------------------------------------------------------

    static boolean ours(Block block) {
        return block != null && (block == sensor || block == calibrated);
    }

    /** 1.20's {@code stepOn} runs for sneaking players too; 1.12.2's {@code onEntityWalk} does not. */
    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (calibrated == null || event.phase != TickEvent.Phase.END || event.player.world.isRemote) {
            return;
        }
        EntityPlayer player = event.player;
        if (player.onGround && player.isSneaking() && !player.isRiding()) {
            World world = player.world;
            BlockPos below = new BlockPos(MathHelper.floor(player.posX), MathHelper.floor(player.posY - 0.2D), MathHelper.floor(player.posZ));
            if (ours(world.getBlockState(below).getBlock())) {
                BlockSculkSensor.stepOn(world, below, player);
            }
        }
    }

    /** Hoes mine the sensors at their material's speed (1.20's {@code #mineable/hoe}), Efficiency included. */
    @SubscribeEvent
    public static void breakSpeed(PlayerEvent.BreakSpeed event) {
        if (!ours(event.getState().getBlock())) {
            return;
        }
        ItemStack held = event.getEntityPlayer().getHeldItemMainhand();
        if (!(held.getItem() instanceof ItemHoe)) {
            return;
        }
        float speed;
        try {
            speed = Item.ToolMaterial.valueOf(((ItemHoe) held.getItem()).getMaterialName()).getEfficiency();
        } catch (IllegalArgumentException e) {
            return;
        }
        int efficiency = EnchantmentHelper.getEfficiencyModifier(event.getEntityPlayer());
        if (efficiency > 0) {
            speed += efficiency * efficiency + 1;
        }
        event.setNewSpeed(event.getNewSpeed() * speed);
    }
}
