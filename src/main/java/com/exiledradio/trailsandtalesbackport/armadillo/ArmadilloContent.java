package com.exiledradio.trailsandtalesbackport.armadillo;

import com.exiledradio.trailsandtalesbackport.ModConfig;
import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import com.exiledradio.trailsandtalesbackport.common.Content;

import net.minecraft.block.BlockDispenser;
import net.minecraft.dispenser.IBehaviorDispenseItem;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.entity.ai.EntityAIAvoidEntity;
import net.minecraft.entity.monster.EntitySpider;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.storage.loot.LootTableList;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * Stream Ar: the armadillo and the armadillo scute.
 *
 * <ul>
 *   <li>Entity {@code armadillo}, id 15 of this stream's block 15-19, with its spawn egg (1.20.5's colors).</li>
 *   <li>Item {@code armadillo_scute}: shed by armadillos and brushed off them. Wolf armor (the wolves stream)
 *       is made from it; that stream finds it by name ({@code Owners.ours("armadillo_scute")}).</li>
 *   <li>Brushing: the brush is the archaeology stream's ({@code trailsandtalesbackport:brush}). This stream
 *       never links against it - it recognizes the item by its registry name ({@link #isBrush}), so brushing
 *       works whenever both streams are in the game, and a dispenser holding the brush brushes an armadillo in
 *       front of it, as 1.20.5's does.</li>
 *   <li>Spiders and cave spiders keep 6 blocks from an armadillo that is not rolled up (1.20.5's avoid goal).</li>
 *   <li>Spawns in savannas and badlands ({@link ArmadilloSpawns}).</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = TrailsAndTalesBackport.MOD_ID)
public final class ArmadilloContent {

    public static final int ARMADILLO_ID = 15;
    /** The archaeology stream's brush. */
    public static final ResourceLocation BRUSH_ID = Content.id("brush");

    private static final List<SoundEvent> SOUNDS = new ArrayList<SoundEvent>();
    public static final SoundEvent AMBIENT = sound("entity.armadillo.ambient");
    public static final SoundEvent BRUSH = sound("entity.armadillo.brush");
    public static final SoundEvent DEATH = sound("entity.armadillo.death");
    public static final SoundEvent EAT = sound("entity.armadillo.eat");
    public static final SoundEvent HURT = sound("entity.armadillo.hurt");
    public static final SoundEvent HURT_REDUCED = sound("entity.armadillo.hurt_reduced");
    public static final SoundEvent LAND = sound("entity.armadillo.land");
    public static final SoundEvent PEEK = sound("entity.armadillo.peek");
    public static final SoundEvent ROLL = sound("entity.armadillo.roll");
    public static final SoundEvent SCUTE_DROP = sound("entity.armadillo.scute_drop");
    public static final SoundEvent STEP = sound("entity.armadillo.step");
    public static final SoundEvent UNROLL_FINISH = sound("entity.armadillo.unroll_finish");
    public static final SoundEvent UNROLL_START = sound("entity.armadillo.unroll_start");

    /** Null when the stream is off. */
    public static Item scute;

    private ArmadilloContent() {
    }

    public static boolean enabled() {
        return ModConfig.armadillos.enabled;
    }

    private static SoundEvent sound(String name) {
        SoundEvent e = new SoundEvent(Content.id(name)).setRegistryName(Content.id(name));
        SOUNDS.add(e);
        return e;
    }

    /** @return true for the brush (the archaeology stream's item, recognized by name) */
    public static boolean isBrush(ItemStack stack) {
        return !stack.isEmpty() && BRUSH_ID.equals(stack.getItem().getRegistryName());
    }

    /** Pre-init: the loot tables. */
    public static void preInit() {
        if (enabled()) {
            LootTableList.register(EntityArmadillo.SHED_LOOT);
            LootTableList.register(EntityArmadillo.BRUSH_LOOT);
        }
    }

    /** Init: spawns and the dispenser's brush, which need every biome and item registered. */
    public static void init() {
        if (!enabled()) {
            return;
        }
        ArmadilloSpawns.addAll();
        registerBrushDispensing();
    }

    // ------------------------------------------------------------------ registry events

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        if (enabled()) {
            scute = Content.item(event.getRegistry(), "armadillo_scute", new Item());
        }
    }

    @SubscribeEvent
    public static void registerEntities(RegistryEvent.Register<EntityEntry> event) {
        if (!enabled()) {
            return;
        }
        event.getRegistry().register(EntityEntryBuilder.create().entity(EntityArmadillo.class)
                .id(Content.id("armadillo"), ARMADILLO_ID).name(TrailsAndTalesBackport.MOD_ID + ".armadillo")
                .tracker(80, 3, true).egg(11366765, 8538184).build());
    }

    @SubscribeEvent
    public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        if (!enabled()) {
            return;
        }
        for (SoundEvent e : SOUNDS) {
            event.getRegistry().register(e);
        }
    }

    // ------------------------------------------------------------------ world events

    /** 1.20.5: spiders (and so cave spiders) avoid an armadillo that is not rolled up, priority 2. */
    @SubscribeEvent
    public static void spiderJoins(EntityJoinWorldEvent event) {
        if (!enabled() || event.getWorld().isRemote || !(event.getEntity() instanceof EntitySpider)) {
            return;
        }
        EntitySpider spider = (EntitySpider) event.getEntity();
        spider.tasks.addTask(2, new EntityAIAvoidEntity<EntityArmadillo>(spider, EntityArmadillo.class,
                a -> a != null && !a.isScared(), 6.0F, 1.0D, 1.2D));
    }

    /**
     * 1.20.5's spawn rules hold for chunk-generation spawns too, which 1.12.2 places without asking the mob:
     * an armadillo that would not spawn there naturally is turned away.
     */
    @SubscribeEvent
    public static void checkSpawn(LivingSpawnEvent.CheckSpawn event) {
        if (event.getEntity() instanceof EntityArmadillo && event.getSpawner() == null && event.getResult() == Event.Result.DEFAULT
                && !((EntityArmadillo) event.getEntity()).getCanSpawnHere()) {
            event.setResult(Event.Result.DENY);
        }
    }

    // ------------------------------------------------------------------ the dispenser's brush

    /**
     * 1.20.5's brush in a dispenser: brushes the first adult armadillo in the block in front (16 durability, the
     * brush breaking when used up); with none it fails (click) and keeps the brush. When another behavior was
     * already registered for the brush, that one runs when there is no armadillo.
     */
    private static void registerBrushDispensing() {
        Item brush = ForgeRegistries.ITEMS.getValue(BRUSH_ID);
        if (brush == null || !BRUSH_ID.equals(brush.getRegistryName())) {
            return;
        }
        IBehaviorDispenseItem previous = BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.getObject(brush);
        boolean hasPrevious = previous != null && previous != IBehaviorDispenseItem.DEFAULT_BEHAVIOR
                && previous.getClass() != net.minecraft.dispenser.BehaviorDefaultDispenseItem.class;
        BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.putObject(brush, new BrushDispenseBehavior(hasPrevious ? previous : null));
    }

    /** Chooses between brushing an armadillo and the brush's earlier behavior (1.12.2's {@code dispense} is final). */
    static final class BrushDispenseBehavior implements IBehaviorDispenseItem {
        private final IBehaviorDispenseItem previous;
        private final Brushing brushing = new Brushing();

        BrushDispenseBehavior(IBehaviorDispenseItem previous) {
            this.previous = previous;
        }

        @Override
        public ItemStack dispense(IBlockSource source, ItemStack stack) {
            if (this.previous != null && armadillos(source).isEmpty()) {
                return this.previous.dispense(source, stack);
            }
            return this.brushing.dispense(source, stack);
        }
    }

    static List<EntityArmadillo> armadillos(IBlockSource source) {
        EnumFacing facing = source.getBlockState().getValue(BlockDispenser.FACING);
        BlockPos pos = source.getBlockPos().offset(facing);
        return source.getWorld().getEntitiesWithinAABB(EntityArmadillo.class, new AxisAlignedBB(pos),
                e -> e != null && e.isEntityAlive());
    }

    /** 1.20.5's optional dispense behavior: the click of a failed dispense when nothing was brushed. */
    static final class Brushing extends Bootstrap.BehaviorDispenseOptional {
        @Override
        protected ItemStack dispenseStack(IBlockSource source, ItemStack stack) {
            this.successful = false;
            for (EntityArmadillo armadillo : armadillos(source)) {
                if (armadillo.brushOffScute(null)) {
                    this.successful = true;
                    if (stack.attemptDamageItem(16, source.getWorld().rand, null)) {
                        stack.shrink(1);
                        stack.setItemDamage(0);
                    }
                    return stack;
                }
            }
            return stack;
        }
    }
}
