package com.exiledradio.trailsandtalesbackport.bookshelf;

import com.exiledradio.backportcore.ownership.BlockOwnership;
import com.exiledradio.trailsandtalesbackport.ModConfig;
import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import com.exiledradio.trailsandtalesbackport.common.Content;
import com.exiledradio.trailsandtalesbackport.common.wood.WoodKind;

import net.minecraft.advancements.Advancement;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRedstoneComparator;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.ShapedOreRecipe;

import java.util.ArrayList;
import java.util.List;

/**
 * Stream K: the chiseled bookshelf and the advancement <i>The Power of Books</i>.
 *
 * <p>The advancement is granted when a player places a comparator that reads a chiseled bookshelf, or a
 * chiseled bookshelf a comparator already faces (1.20's condition: the shelf with a comparator beside it
 * facing away from it, which is how a comparator reads a block).
 */
@Mod.EventBusSubscriber(modid = TrailsAndTalesBackport.MOD_ID)
public final class BookshelfContent {

    public static final ResourceLocation ADVANCEMENT = Content.id("adventure/read_power_of_chiseled_bookshelf");

    private static final List<SoundEvent> SOUNDS = new ArrayList<SoundEvent>();
    static final SoundType SOUND = WoodKind.soundType(SOUNDS, "block.chiseled_bookshelf", 1.0F, 1.0F);
    static final SoundEvent INSERT = event("block.chiseled_bookshelf.insert");
    static final SoundEvent INSERT_ENCHANTED = event("block.chiseled_bookshelf.insert.enchanted");
    static final SoundEvent PICKUP = event("block.chiseled_bookshelf.pickup");
    static final SoundEvent PICKUP_ENCHANTED = event("block.chiseled_bookshelf.pickup.enchanted");

    /** Null when the stream is off. */
    public static BlockChiseledBookshelf shelf;

    private BookshelfContent() {
    }

    private static SoundEvent event(String name) {
        SoundEvent e = new SoundEvent(Content.id(name));
        e.setRegistryName(Content.id(name));
        SOUNDS.add(e);
        return e;
    }

    public static boolean enabled() {
        return ModConfig.chiseledBookshelf.enabled;
    }

    public static void preInit() {
        if (enabled()) {
            BlockOwnership.declare(TrailsAndTalesBackport.MOD_ID, "chiseled_bookshelf");
        }
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        if (!enabled()) {
            return;
        }
        shelf = Content.block(event.getRegistry(), "chiseled_bookshelf",
                new BlockChiseledBookshelf(SOUND, INSERT, INSERT_ENCHANTED, PICKUP, PICKUP_ENCHANTED), "axe");
        Content.multipart(shelf);
        GameRegistry.registerTileEntity(TileChiseledBookshelf.class, Content.id("chiseled_bookshelf"));
        Blocks.FIRE.setFireInfo(shelf, 30, 20);
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        if (enabled()) {
            Content.blockItem(event.getRegistry(), shelf, new ItemBlock(shelf) {
                @Override
                public int getItemBurnTime(ItemStack stack) {
                    return 300;
                }
            });
        }
    }

    @SubscribeEvent
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        if (enabled()) {
            ResourceLocation id = Content.id("chiseled_bookshelf");
            event.getRegistry().register(new ShapedOreRecipe(id, new ItemStack(shelf), "###", "XXX", "###",
                    '#', "plankWood", 'X', "slabWood").setRegistryName(id));
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

    // ---------------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onPlace(BlockEvent.PlaceEvent event) {
        if (shelf == null || !(event.getPlayer() instanceof EntityPlayerMP)) {
            return;
        }
        World world = event.getWorld();
        BlockPos pos = event.getPos();
        Block placed = event.getPlacedBlock().getBlock();
        if (placed == shelf) {
            for (EnumFacing side : EnumFacing.Plane.HORIZONTAL) {
                if (reads(world, pos.offset(side), pos)) {
                    grant(event.getPlayer());
                    return;
                }
            }
        } else if (placed instanceof BlockRedstoneComparator) {
            EnumFacing facing = event.getPlacedBlock().getValue(BlockRedstoneComparator.FACING);
            if (world.getBlockState(pos.offset(facing)).getBlock() == shelf) {
                grant(event.getPlayer());
            }
        }
    }

    /** @return true if a comparator at {@code comparator} takes its input from {@code from} */
    static boolean reads(World world, BlockPos comparator, BlockPos from) {
        IBlockState state = world.getBlockState(comparator);
        return state.getBlock() instanceof BlockRedstoneComparator
                && comparator.offset(state.getValue(BlockRedstoneComparator.FACING)).equals(from);
    }

    static void grant(EntityPlayer player) {
        EntityPlayerMP mp = (EntityPlayerMP) player;
        Advancement advancement = mp.getServer().getAdvancementManager().getAdvancement(ADVANCEMENT);
        if (advancement != null) {
            mp.getAdvancements().grantCriterion(advancement, "chiseled_bookshelf");
        }
    }
}
