package com.exiledradio.trailsandtalesbackport.selftest;

import com.exiledradio.backportcore.gameevent.GameEvent;
import com.exiledradio.backportcore.gameevent.GameEventContext;
import com.exiledradio.backportcore.gameevent.GameEventListener;
import com.exiledradio.backportcore.gameevent.GameEventTags;
import com.exiledradio.backportcore.gameevent.GameEvents;
import com.exiledradio.backportcore.gameevent.PositionSource;
import com.exiledradio.trailsandtalesbackport.armadillo.EntityArmadillo;
import com.exiledradio.trailsandtalesbackport.archaeology.ArchaeologyContent;
import com.exiledradio.trailsandtalesbackport.archaeology.TileDecoratedPot;
import com.exiledradio.trailsandtalesbackport.bookshelf.BookshelfContent;
import com.exiledradio.trailsandtalesbackport.bookshelf.TileChiseledBookshelf;
import com.exiledradio.trailsandtalesbackport.camel.EntityCamel;
import com.exiledradio.trailsandtalesbackport.heads.NoteBlockHeads;
import com.exiledradio.trailsandtalesbackport.sculk.BlockCalibratedSculkSensor;
import com.exiledradio.trailsandtalesbackport.sculk.BlockSculkSensor;
import com.exiledradio.trailsandtalesbackport.sculk.SensorContent;
import com.exiledradio.trailsandtalesbackport.sculk.TileCalibratedSculkSensor;
import com.exiledradio.trailsandtalesbackport.sculk.TileSculkSensor;
import com.exiledradio.trailsandtalesbackport.sniffer.SnifferContent;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldServer;

import java.util.ArrayList;
import java.util.List;

/** Stream Sc: the calibrated sculk sensor, the sculk sensor copy, resonance, and this mod's game events. */
public final class SensorSelfTest implements SelfTest {

    @Override
    public String name() {
        return "sensor";
    }

    /** Hears every event at once and records it. */
    static final class Ears implements GameEventListener {
        final BlockPos pos;
        final List<GameEvent> heard = new ArrayList<GameEvent>();
        final List<Vec3d> where = new ArrayList<Vec3d>();
        final List<Entity> sources = new ArrayList<Entity>();

        Ears(BlockPos pos) {
            this.pos = pos;
        }

        @Override
        public PositionSource position() {
            return new PositionSource.Block(this.pos);
        }

        @Override
        public int radius() {
            return 32;
        }

        @Override
        public boolean immediate() {
            return true;
        }

        @Override
        public boolean handle(WorldServer world, GameEvent event, Vec3d origin, GameEventContext context) {
            this.heard.add(event);
            this.where.add(origin);
            this.sources.add(context.source);
            return true;
        }

        void clear() {
            this.heard.clear();
            this.where.clear();
            this.sources.clear();
        }
    }

    /** Lets a tick pass for the vibration selector, then runs the sensor's tick n times. */
    private static void ticks(WorldServer world, TileSculkSensor tile, int n) {
        for (int i = 0; i < n; i++) {
            world.getWorldInfo().setWorldTotalTime(world.getTotalWorldTime() + 1);
            tile.update();
        }
    }

    /** A sensor receives only with the 3x3 chunks around it loaded (1.20's requiresAdjacentChunksToBeTicking). */
    private static void loadAround(Check c, BlockPos pos) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                c.world.getChunk((pos.getX() >> 4) + x, (pos.getZ() >> 4) + z);
            }
        }
    }

    private static TileSculkSensor sensorAt(Check c, BlockPos pos) {
        TileEntity te = c.world.getTileEntity(pos);
        return te instanceof TileSculkSensor ? (TileSculkSensor) te : null;
    }

    @Override
    public void run(Check c) {
        BlockCalibratedSculkSensor cal = SensorContent.calibrated;
        BlockSculkSensor copy = SensorContent.sensor;
        if (!c.ok(cal != null, "calibrated sculk sensor registered")) {
            return;
        }
        boolean wild = net.minecraftforge.fml.common.Loader.isModLoaded("wildupdatebackport");
        if (wild) {
            c.ok(copy == null, "with Wild Update Backport, no sculk sensor of ours");
            c.eq(new net.minecraft.util.ResourceLocation("wildupdatebackport", "sculk_sensor"), SensorContent.sensorItem().getRegistryName(),
                    "the recipe takes Wild Update Backport's sculk sensor");
        } else {
            c.ok(copy != null, "our sculk sensor is registered without Wild Update Backport");
        }
        c.ok(GameEvents.isAtLeast("1.20"), "1.20's game event rules are on");
        c.eq(GameEventTags.VIBRATIONS_1_20, GameEventTags.vibrations(), "vibrations are 1.20's");

        // --- states -------------------------------------------------------------------------------
        if (copy != null) {
            c.eq(3, copy.getBlockState().getValidStates().size(), "sculk sensor: 3 phases (as Wild Update Backport's)");
            for (BlockSculkSensor.Phase p : BlockSculkSensor.Phase.values()) {
                IBlockState s = copy.getDefaultState().withProperty(BlockSculkSensor.PHASE, p);
                c.eq(p.ordinal(), copy.getMetaFromState(s), "sculk sensor metadata is the phase: " + p);
            }
            c.eq(30, copy.activeTicks(), "sculk sensor: active 30 ticks (1.20)");
        }
        c.eq(12, cal.getBlockState().getValidStates().size(), "calibrated: 3 phases x 4 facings");
        for (IBlockState s : cal.getBlockState().getValidStates()) {
            c.eq(s, cal.getStateFromMeta(cal.getMetaFromState(s)), "calibrated metadata round trip " + s);
        }
        c.eq(10, cal.activeTicks(), "calibrated: active 10 ticks");

        // --- recipe --------------------------------------------------------------------------------
        // Caves & Cliffs Backport's shard, else this mod's copy (smithing.copy.AmethystCopies).
        Item shard = com.exiledradio.trailsandtalesbackport.common.Owners.item(com.exiledradio.trailsandtalesbackport.common.Owners.CAVES_AND_CLIFFS, "amethyst_shard");
        if (c.ok(shard != null, "an amethyst shard for the recipe")) {
            ItemStack a = new ItemStack(shard);
            ItemStack s = new ItemStack(SensorContent.sensorItem());
            Crafting.expect(c, Crafting.grid(3, 2, ItemStack.EMPTY, a, ItemStack.EMPTY, a, s, a), new ItemStack(cal),
                    "3 amethyst shards over and beside a sculk sensor");
        }

        // --- placement and redstone ---------------------------------------------------------------
        BlockPos area = c.area();
        BlockPos at = area.add(8, 0, 8);
        loadAround(c, at);
        EntityPlayer placer = c.player("sensor", at.south(3));
        placer.rotationYaw = 0.0F; // looking south
        IBlockState placed = cal.getStateForPlacement(c.world, at, EnumFacing.UP, 0.5F, 1.0F, 0.5F, 0, placer, EnumHand.MAIN_HAND);
        c.eq(EnumFacing.SOUTH, placed.getValue(BlockCalibratedSculkSensor.FACING), "facing: where the placer looks");
        c.world.setBlockState(at, placed.withProperty(BlockCalibratedSculkSensor.FACING, EnumFacing.EAST), 3);
        TileSculkSensor tile = sensorAt(c, at);
        if (!c.ok(tile instanceof TileCalibratedSculkSensor, "calibrated tile entity")) {
            return;
        }
        tile.update();
        GameEvents.emit(c.world, GameEvent.EAT, at.north(5), GameEventContext.NONE);
        ticks(c.world, tile, 1);
        c.ok(tile.data().current() != null, "a vibration 5 blocks off is on its way");
        ticks(c.world, tile, 6);
        IBlockState now = c.world.getBlockState(at);
        c.eq(BlockSculkSensor.Phase.ACTIVE, BlockSculkSensor.phase(now), "active once it arrives");
        c.eq(11, tile.power(), "power for 5 of 16 blocks: 15 - floor(15/16 x 5)");
        c.eq(8, now.getComparatorInputOverride(c.world, at), "the comparator reads eat's frequency, 8");
        c.eq(11, now.getWeakPower(c.world, at, EnumFacing.NORTH), "power out of the front");
        c.eq(0, now.getWeakPower(c.world, at, EnumFacing.EAST), "no power out of the back (toward the input)");
        c.eq(11, now.getStrongPower(c.world, at, EnumFacing.UP), "strong power into the block below");
        c.eq(0, now.getStrongPower(c.world, at, EnumFacing.NORTH), "no strong power sideways");
        cal.updateTick(c.world, at, now, c.world.rand);
        c.eq(BlockSculkSensor.Phase.COOLDOWN, BlockSculkSensor.phase(c.world.getBlockState(at)), "then cooldown");
        c.eq(0, tile.power(), "no power in cooldown");
        cal.updateTick(c.world, at, c.world.getBlockState(at), c.world.rand);
        c.eq(BlockSculkSensor.Phase.INACTIVE, BlockSculkSensor.phase(c.world.getBlockState(at)), "then inactive");

        // --- the frequency filter ------------------------------------------------------------------
        // The back of an east-facing sensor is its west side.
        c.world.setBlockState(at.west(), Blocks.REDSTONE_BLOCK.getDefaultState(), 3);
        c.eq(15, BlockCalibratedSculkSensor.backSignal(c.world, at, c.world.getBlockState(at)), "a redstone block behind: 15");
        GameEvents.emit(c.world, GameEvent.EAT, at.north(3), GameEventContext.NONE);
        ticks(c.world, tile, 6);
        c.eq(BlockSculkSensor.Phase.INACTIVE, BlockSculkSensor.phase(c.world.getBlockState(at)), "frequency 15 asked: eat (8) is not heard");
        GameEvents.emit(c.world, GameEvent.EXPLODE, at.north(3), GameEventContext.NONE);
        ticks(c.world, tile, 6);
        c.eq(BlockSculkSensor.Phase.ACTIVE, BlockSculkSensor.phase(c.world.getBlockState(at)), "explode (15) is heard");
        c.eq(15, tile.lastVibrationFrequency(), "last frequency 15");
        c.world.setBlockToAir(at.west());
        NBTTagCompound tag = tile.writeToNBT(new NBTTagCompound());
        c.eq(15, tag.getInteger("last_vibration_frequency"), "last_vibration_frequency saved");
        c.ok(tag.hasKey("listener", 10), "listener saved");
        c.world.setBlockToAir(at);

        // --- the sculk sensor copy: range, own block, stepping, resonance -----------------------------
        BlockPos sp = area.add(20, 0, 20);
        loadAround(c, sp);
        EntityPig pig = new EntityPig(c.world);
        pig.setPosition(sp.getX() + 0.5D, sp.getY() + 0.5D, sp.getZ() + 0.5D);
        Ears ears = new Ears(sp.west(3));
        GameEvents.add(c.world, ears);
        net.minecraft.block.Block amethyst = com.exiledradio.trailsandtalesbackport.common.Owners.block(
                com.exiledradio.trailsandtalesbackport.common.Owners.CAVES_AND_CLIFFS, "amethyst_block");
        c.ok(amethyst != null && GameEventTags.resonates(amethyst.getDefaultState()), "the amethyst block resonates");
        if (copy == null) {
            // Wild Update Backport's sensor (1.19.2's stepOn: active at once) resonates through Core's SensorActivation.
            net.minecraft.block.Block theirs = net.minecraftforge.fml.common.registry.ForgeRegistries.BLOCKS.getValue(
                    new net.minecraft.util.ResourceLocation("wildupdatebackport", "sculk_sensor"));
            c.world.setBlockState(sp, theirs.getDefaultState(), 3);
            c.world.setBlockState(sp.east(), amethyst.getDefaultState(), 3);
            c.world.spawnEntity(pig);
            ears.clear();
            theirs.onEntityWalk(c.world, sp, pig);
            int w = ears.heard.indexOf(GameEvent.resonance(1));
            c.ok(w >= 0 && new BlockPos(ears.where.get(w)).equals(sp.east()), "Wild Update Backport's sensor resonates the amethyst beside it");
            c.world.setBlockToAir(sp.east());
            c.world.setBlockToAir(sp);
        } else {
            c.world.setBlockState(sp, copy.getDefaultState(), 3);
            TileSculkSensor sensor = sensorAt(c, sp);
            sensor.update();
            GameEvents.emit(c.world, GameEvent.EAT, sp.north(9), GameEventContext.NONE);
            ticks(c.world, sensor, 12);
            c.eq(BlockSculkSensor.Phase.INACTIVE, BlockSculkSensor.phase(c.world.getBlockState(sp)), "the sculk sensor does not hear 9 blocks off");
            GameEvents.emit(c.world, GameEvent.BLOCK_PLACE, sp, GameEventContext.NONE);
            ticks(c.world, sensor, 3);
            c.eq(BlockSculkSensor.Phase.INACTIVE, BlockSculkSensor.phase(c.world.getBlockState(sp)), "nor its own block being placed");

            c.world.setBlockState(sp.east(), amethyst.getDefaultState(), 3);
            c.world.spawnEntity(pig);
            ears.clear();
            copy.onEntityWalk(c.world, sp, pig);
            ticks(c.world, sensor, 1);
            IBlockState stepped = c.world.getBlockState(sp);
            c.eq(BlockSculkSensor.Phase.ACTIVE, BlockSculkSensor.phase(stepped), "stepping on it activates it the next tick");
            c.eq(15, sensor.power(), "at full power");
            c.eq(1, sensor.lastVibrationFrequency(), "with step's frequency");
            int r = ears.heard.indexOf(GameEvent.resonance(1));
            c.ok(r >= 0 && new BlockPos(ears.where.get(r)).equals(sp.east()), "the amethyst beside it resonates at frequency 1, from itself");
            c.ok(ears.heard.contains(GameEvent.SCULK_SENSOR_TENDRILS_CLICKING), "and the tendrils click");
            c.ok(r >= 0 && r < ears.heard.indexOf(GameEvent.SCULK_SENSOR_TENDRILS_CLICKING), "resonance before the clicking");
            pig.setDead();
            copy.updateTick(c.world, sp, stepped, c.world.rand);
            copy.updateTick(c.world, sp, c.world.getBlockState(sp), c.world.rand);
            c.world.setBlockToAir(sp.east());
            c.world.setBlockToAir(sp);
        }

        // --- this mod's game events ----------------------------------------------------------------
        BlockPos ev = area.add(4, 0, 4);
        GameEvents.remove(c.world, ears);
        ears = new Ears(ev.east(4).south(4));
        GameEvents.add(c.world, ears);
        ears.clear();
        EntityArmadillo armadillo = new EntityArmadillo(c.world);
        armadillo.setPosition(ev.getX() + 0.5D, ev.getY(), ev.getZ() + 0.5D);
        c.world.spawnEntity(armadillo);
        ears.clear();
        armadillo.rollUp();
        c.ok(ears.heard.contains(GameEvent.ENTITY_ACTION) && ears.sources.get(ears.heard.indexOf(GameEvent.ENTITY_ACTION)) == armadillo,
                "armadillo rolling up: entity_action from it");
        ears.clear();
        armadillo.rollOut();
        c.ok(ears.heard.contains(GameEvent.ENTITY_ACTION), "armadillo unrolling: entity_action");
        ears.clear();
        armadillo.brushOffScute(null);
        c.ok(ears.heard.contains(GameEvent.ENTITY_INTERACT), "armadillo brushed: entity_interact");
        armadillo.setDead();

        EntityCamel camel = new EntityCamel(c.world);
        camel.setPosition(ev.getX() + 2.5D, ev.getY(), ev.getZ() + 0.5D);
        c.world.spawnEntity(camel);
        ears.clear();
        camel.sitDown();
        c.ok(ears.heard.contains(GameEvent.ENTITY_ACTION), "camel sitting down: entity_action");
        ears.clear();
        camel.standUp();
        c.ok(ears.heard.contains(GameEvent.ENTITY_ACTION), "camel standing up: entity_action");
        ears.clear();
        camel.handleStartJump(100);
        c.ok(ears.heard.contains(GameEvent.ENTITY_ACTION), "camel dashing: entity_action");
        camel.setDead();

        if (SnifferContent.snifferEgg != null) {
            ears.clear();
            c.world.setBlockState(ev.east(6), SnifferContent.snifferEgg.getDefaultState(), 3);
            c.ok(ears.heard.contains(GameEvent.BLOCK_PLACE), "a sniffer egg placed: block_place");
            c.world.setBlockToAir(ev.east(6));
        }

        if (ArchaeologyContent.decoratedPot != null) {
            BlockPos pot = ev.south(4);
            c.world.setBlockState(pot, ArchaeologyContent.decoratedPot.getDefaultState(), 3);
            EntityPlayer potter = c.player("potter", pot.north());
            potter.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.DIAMOND, 2));
            ears.clear();
            ArchaeologyContent.decoratedPot.onBlockActivated(c.world, pot, c.world.getBlockState(pot), potter, EnumHand.MAIN_HAND,
                    EnumFacing.UP, 0.5F, 1.0F, 0.5F);
            c.ok(ears.heard.contains(GameEvent.BLOCK_CHANGE) && ears.sources.get(ears.heard.indexOf(GameEvent.BLOCK_CHANGE)) == potter,
                    "an item into a pot: block_change from the player");
            c.ok(((TileDecoratedPot) c.world.getTileEntity(pot)).getTheItem().getItem() == Items.DIAMOND, "the pot took it");
            potter.setHeldItem(EnumHand.MAIN_HAND, ItemStack.EMPTY);
            ears.clear();
            ArchaeologyContent.decoratedPot.onBlockActivated(c.world, pot, c.world.getBlockState(pot), potter, EnumHand.MAIN_HAND,
                    EnumFacing.UP, 0.5F, 1.0F, 0.5F);
            c.ok(ears.heard.contains(GameEvent.BLOCK_CHANGE), "an empty hand on a pot (it wobbles): block_change");
            ears.clear();
            ArchaeologyContent.decoratedPot.shatter(c.world, pot, c.world.getBlockState(pot), pig);
            c.ok(ears.heard.contains(GameEvent.BLOCK_DESTROY) && ears.sources.get(ears.heard.indexOf(GameEvent.BLOCK_DESTROY)) == pig,
                    "a projectile shattering a pot: block_destroy from the projectile");
            for (net.minecraft.entity.item.EntityItem item : c.world.getEntitiesWithinAABB(net.minecraft.entity.item.EntityItem.class,
                    new net.minecraft.util.math.AxisAlignedBB(pot).grow(2))) {
                item.setDead();
            }
        }

        if (BookshelfContent.shelf != null) {
            BlockPos shelfPos = ev.south(8);
            c.world.setBlockState(shelfPos, BookshelfContent.shelf.getDefaultState(), 3);
            TileChiseledBookshelf shelf = (TileChiseledBookshelf) c.world.getTileEntity(shelfPos);
            ears.clear();
            shelf.put(0, new ItemStack(Items.BOOK));
            c.ok(ears.heard.contains(GameEvent.BLOCK_CHANGE), "a book into a chiseled bookshelf: block_change");
            ears.clear();
            shelf.take(0);
            c.ok(ears.heard.contains(GameEvent.BLOCK_CHANGE), "a book out: block_change");
            c.world.setBlockToAir(shelfPos);
        }

        BlockPos note = ev.east(10);
        c.world.setBlockState(note, Blocks.NOTEBLOCK.getDefaultState(), 3);
        c.world.setBlockState(note.up(), Blocks.SKULL.getDefaultState().withProperty(net.minecraft.block.BlockSkull.FACING, EnumFacing.UP), 3);
        ears.clear();
        NoteBlockHeads.play(c.world, note, placer);
        c.ok(ears.heard.contains(GameEvent.NOTE_BLOCK_PLAY) && ears.sources.get(ears.heard.indexOf(GameEvent.NOTE_BLOCK_PLAY)) == placer,
                "a head on a note block played: note_block_play from the player");
        c.world.setBlockToAir(note.up());
        c.world.setBlockToAir(note);

        pig.setDead();
        GameEvents.remove(c.world, ears);
    }
}
