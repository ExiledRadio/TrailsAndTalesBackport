package com.exiledradio.trailsandtalesbackport.heads;

import com.exiledradio.backportcore.ownership.BlockOwnership;
import com.exiledradio.trailsandtalesbackport.ModConfig;
import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import com.exiledradio.trailsandtalesbackport.common.Content;
import com.exiledradio.trailsandtalesbackport.common.Owners;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDispenser;
import net.minecraft.dispenser.BehaviorDefaultDispenseItem;
import net.minecraft.dispenser.IBehaviorDispenseItem;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Stream Hd: the piglin head and mob heads on note blocks.
 *
 * <ul>
 *   <li>{@link BlockPiglinHead} / {@link ItemPiglinHead}: standing and wall piglin head, worn on the head;
 *       its ears flap while it is powered, or worn by a walking mob (client).</li>
 *   <li>A piglin - Nether Update's, found by its registry name - killed by a charged creeper's explosion
 *       drops one ({@link #onDrops}), once per creeper, as every 1.12.2 head drop is.</li>
 *   <li>{@link NoteBlockHeads}: a head on a note block plays its mob's sound.</li>
 *   <li>A dispenser puts the piglin head on the mob or armor stand in front of it, or fails (1.20).</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = TrailsAndTalesBackport.MOD_ID)
public final class HeadsContent {

    /** Nether Update's piglin; only an adult one drops the head. */
    public static final ResourceLocation PIGLIN = new ResourceLocation(Owners.NETHER_UPDATE, "piglin");

    private static final List<SoundEvent> SOUNDS = new ArrayList<SoundEvent>();
    public static final SoundEvent IMITATE_SKELETON = event("block.note_block.imitate.skeleton");
    public static final SoundEvent IMITATE_WITHER_SKELETON = event("block.note_block.imitate.wither_skeleton");
    public static final SoundEvent IMITATE_ZOMBIE = event("block.note_block.imitate.zombie");
    public static final SoundEvent IMITATE_CREEPER = event("block.note_block.imitate.creeper");
    public static final SoundEvent IMITATE_ENDER_DRAGON = event("block.note_block.imitate.ender_dragon");
    public static final SoundEvent IMITATE_PIGLIN = event("block.note_block.imitate.piglin");

    /** Null when the stream is off. */
    public static BlockPiglinHead head;
    /** Null when the stream is off. */
    public static ItemPiglinHead headItem;

    private HeadsContent() {
    }

    private static SoundEvent event(String name) {
        SoundEvent e = new SoundEvent(Content.id(name));
        e.setRegistryName(Content.id(name));
        SOUNDS.add(e);
        return e;
    }

    public static boolean enabled() {
        return ModConfig.heads.enabled;
    }

    public static void preInit() {
        if (enabled()) {
            BlockOwnership.declare(TrailsAndTalesBackport.MOD_ID, "piglin_head");
            MinecraftForge.EVENT_BUS.register(NoteBlockHeads.class);
        }
    }

    public static void init() {
        if (headItem != null) {
            BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.putObject(headItem, EQUIP);
        }
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        if (!enabled()) {
            return;
        }
        head = Content.block(event.getRegistry(), "piglin_head", new BlockPiglinHead(), null);
        GameRegistry.registerTileEntity(TilePiglinHead.class, Content.id("piglin_head"));
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        if (enabled()) {
            headItem = Content.blockItem(event.getRegistry(), head, new ItemPiglinHead(head));
        }
    }

    @SubscribeEvent
    public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        if (enabled()) {
            for (SoundEvent e : SOUNDS) {
                event.getRegistry().register(e);
            }
        }
    }

    // --- the drop --------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        EntityLivingBase dead = event.getEntityLiving();
        if (dead.world.isRemote) {
            return;
        }
        EntityItem item = headDrop(dead, EntityList.getKey(dead), event.getSource());
        if (item != null) {
            event.getDrops().add(item);
        }
    }

    /**
     * 1.20's {@code Piglin.dropCustomDeathLoot}: a piglin (an adult: 1.20 drops no loot for babies) killed by
     * a charged creeper that has dropped no head yet drops a piglin head, and the creeper counts it.
     *
     * @param key the dead entity's registry name, null for an unregistered one
     * @return the head to drop, or null
     */
    @Nullable
    public static EntityItem headDrop(EntityLivingBase dead, @Nullable ResourceLocation key, DamageSource source) {
        if (headItem == null || key == null || !PIGLIN.equals(key) || dead.isChild()) {
            return null;
        }
        Entity killer = source.getTrueSource();
        if (!(killer instanceof EntityCreeper) || killer == dead) {
            return null;
        }
        EntityCreeper creeper = (EntityCreeper) killer;
        // canDropMobsSkull: charged, and no head dropped by this creeper yet (1.12.2's check adds doMobLoot).
        if (!creeper.getPowered() || !creeper.ableToCauseSkullDrop()) {
            return null;
        }
        creeper.incrementDroppedSkulls();
        EntityItem item = new EntityItem(dead.world, dead.posX, dead.posY, dead.posZ, new ItemStack(headItem));
        item.setDefaultPickupDelay();
        return item;
    }

    // --- the dispenser ---------------------------------------------------------------------------

    /**
     * 1.20's head behavior: put it on the first mob or armor stand in front that can wear it, or fail
     * (the failure click, the head stays in the dispenser).
     */
    static final IBehaviorDispenseItem EQUIP = new BehaviorDefaultDispenseItem() {
        private boolean success;

        @Override
        protected ItemStack dispenseStack(IBlockSource source, ItemStack stack) {
            int before = stack.getCount();
            ItemArmor.dispenseArmor(source, stack);
            this.success = stack.getCount() < before;
            return stack;
        }

        @Override
        protected void playDispenseSound(IBlockSource source) {
            source.getWorld().playEvent(this.success ? 1000 : 1001, source.getBlockPos(), 0);
        }
    };
}
