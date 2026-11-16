package com.exiledradio.trailsandtalesbackport.sculk;

import com.exiledradio.backportcore.gameevent.GameEvent;
import com.exiledradio.backportcore.gameevent.GameEventContext;
import com.exiledradio.backportcore.gameevent.GameEvents;
import com.exiledradio.backportcore.gameevent.SensorActivation;
import com.exiledradio.backportcore.waterlog.Waterloggable;
import com.exiledradio.backportcore.waterlog.Waterlogging;

import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.Random;

/**
 * The sculk sensor as 1.20.6's {@code SculkSensorBlock}: this mod's copy when Wild Update Backport (its owner) is
 * absent, and the base of the calibrated sculk sensor. Same states and metadata as Wild Update Backport's sensor
 * ({@code sculk_sensor_phase}, metadata = the phase), so the owner can take a world's copies over.
 *
 * <ul>
 *   <li>{@code inactive} -> hears a vibration ({@link TileSculkSensor}) -> {@code active} for
 *       {@link #activeTicks()} (30; the calibrated sensor 10) with redstone power from 15 (at the sensor) down to
 *       1 (at the edge of its range), strongly powering the block below -> {@code cooldown} for 10 ticks ->
 *       {@code inactive}, with {@code sculk_clicking_stop}.</li>
 *   <li>A comparator reads the last vibration's frequency while active.</li>
 *   <li>Activating resonates amethyst beside it (Backport Core's {@link SensorActivation}; see
 *       {@link Resonance}), then posts {@code sculk_sensor_tendrils_clicking}.</li>
 *   <li>Stepping on it schedules a step vibration from the entity (1.20's {@code forceScheduleVibration}),
 *       sneaking or not; a warden stepping on it does not.</li>
 *   <li>It holds water (Backport Core's {@link Waterloggable}); a waterlogged sensor clicks silently.</li>
 * </ul>
 * The power lives in the tile entity: 1.20's {@code power} property is not part of any model.
 */
public class BlockSculkSensor extends Block implements Waterloggable {

    public enum Phase implements IStringSerializable {
        INACTIVE("inactive"), ACTIVE("active"), COOLDOWN("cooldown");

        private final String name;

        Phase(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return this.name;
        }
    }

    public static final PropertyEnum<Phase> PHASE = PropertyEnum.create("sculk_sensor_phase", Phase.class);
    public static final int ACTIVE_TICKS = 30;
    public static final int COOLDOWN_TICKS = 10;
    private static final AxisAlignedBB SHAPE = new AxisAlignedBB(0, 0, 0, 1, 0.5D, 1);
    private static final ResourceLocation WARDEN = new ResourceLocation("wildupdatebackport", "warden");

    public BlockSculkSensor() {
        super(SensorContent.MATERIAL, MapColor.CYAN);
        this.setHardness(1.5F);
        this.setResistance(1.5F * 5.0F / 3.0F);
        this.setLightLevel(1.0F / 15.0F);
        this.setSoundType(SensorContent.SENSOR_SOUND);
        this.setDefaultState(defaultState());
    }

    protected IBlockState defaultState() {
        return this.blockState.getBaseState().withProperty(PHASE, Phase.INACTIVE);
    }

    /** 1.20's {@code getActiveTicks}. */
    public int activeTicks() {
        return ACTIVE_TICKS;
    }

    public static Phase phase(IBlockState state) {
        return state.getValue(PHASE);
    }

    public static boolean canActivate(IBlockState state) {
        return state.getBlock() instanceof BlockSculkSensor && phase(state) == Phase.INACTIVE;
    }

    static boolean isWarden(Entity entity) {
        return WARDEN.equals(EntityList.getKey(entity));
    }

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
        if (phase(state) != Phase.ACTIVE) {
            if (phase(state) == Phase.COOLDOWN) {
                world.setBlockState(pos, state.withProperty(PHASE, Phase.INACTIVE), 3);
                if (!Waterlogging.hasWater(world, pos)) {
                    world.playSound(null, pos, SensorContent.CLICKING_STOP, SoundCategory.BLOCKS, 1.0F, world.rand.nextFloat() * 0.2F + 0.8F);
                }
            }
        } else {
            deactivate(world, pos, state);
        }
    }

    /** 1.20's {@code stepOn}. Also reached for sneaking players, whom 1.12.2's walking skips (see SensorContent). */
    @Override
    public void onEntityWalk(World world, BlockPos pos, Entity entity) {
        stepOn(world, pos, entity);
    }

    static void stepOn(World world, BlockPos pos, Entity entity) {
        IBlockState state = world.getBlockState(pos);
        if (world.isRemote || !canActivate(state) || isWarden(entity) || !(world instanceof WorldServer)) {
            return;
        }
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileSculkSensor) {
            TileSculkSensor sensor = (TileSculkSensor) te;
            WorldServer server = (WorldServer) world;
            if (sensor.user().canReceiveVibration(server, pos, GameEvent.STEP, GameEventContext.of(state))) {
                sensor.listener().forceSchedule(server, GameEvent.STEP, GameEventContext.of(entity),
                        new Vec3d(entity.posX, entity.posY, entity.posZ));
            }
        }
    }

    /** 1.20's {@code onPlace}: a sensor placed with power left over (a structure, a command) loses it. */
    @Override
    public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
        if (!world.isRemote) {
            TileEntity te = world.getTileEntity(pos);
            if (te instanceof TileSculkSensor && ((TileSculkSensor) te).power() > 0 && !world.isUpdateScheduled(pos, this)) {
                ((TileSculkSensor) te).setPower(0);
            }
        }
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        if (phase(state) == Phase.ACTIVE) {
            updateNeighbors(world, pos, state.getBlock());
        }
        super.breakBlock(world, pos, state);
    }

    private static void updateNeighbors(World world, BlockPos pos, Block block) {
        world.notifyNeighborsOfStateChange(pos, block, false);
        world.notifyNeighborsOfStateChange(pos.down(), block, false);
    }

    public static void deactivate(World world, BlockPos pos, IBlockState state) {
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileSculkSensor) {
            ((TileSculkSensor) te).setPower(0);
        }
        world.setBlockState(pos, state.withProperty(PHASE, Phase.COOLDOWN), 3);
        world.scheduleUpdate(pos, state.getBlock(), COOLDOWN_TICKS);
        updateNeighbors(world, pos, state.getBlock());
    }

    /** 1.20's {@code activate}. */
    public void activate(@Nullable Entity source, World world, BlockPos pos, IBlockState state, int power, int frequency) {
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileSculkSensor) {
            ((TileSculkSensor) te).setPower(power);
        }
        world.setBlockState(pos, state.withProperty(PHASE, Phase.ACTIVE), 3);
        world.scheduleUpdate(pos, state.getBlock(), activeTicks());
        updateNeighbors(world, pos, state.getBlock());
        SensorActivation.activated(source, world, pos, frequency);
        GameEvents.emit(world, GameEvent.SCULK_SENSOR_TENDRILS_CLICKING, pos, GameEventContext.of(source));
        if (!Waterlogging.hasWater(world, pos)) {
            world.playSound(null, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, SensorContent.CLICKING, SoundCategory.BLOCKS,
                    1.0F, world.rand.nextFloat() * 0.2F + 0.8F);
        }
    }

    /** 1.20's sculk-to-redstone dust while active; 1.12.2's dust takes one color, so it is sculk's cyan. */
    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(IBlockState state, World world, BlockPos pos, Random rand) {
        if (phase(state) != Phase.ACTIVE) {
            return;
        }
        EnumFacing dir = EnumFacing.random(rand);
        if (dir == EnumFacing.UP || dir == EnumFacing.DOWN) {
            return;
        }
        double x = pos.getX() + 0.5D + (dir.getXOffset() == 0 ? 0.5D - rand.nextDouble() : dir.getXOffset() * 0.6D);
        double y = pos.getY() + 0.25D;
        double z = pos.getZ() + 0.5D + (dir.getZOffset() == 0 ? 0.5D - rand.nextDouble() : dir.getZOffset() * 0.6D);
        // 1.12.2's redstone dust reads its speed as a color: 0x39D6E0, sculk's particle color.
        world.spawnParticle(EnumParticleTypes.REDSTONE, x, y, z, 0.2235D, 0.8392D, 0.8784D);
    }

    // --- redstone ----------------------------------------------------------------------------

    @Override
    @SuppressWarnings("deprecation")
    public boolean canProvidePower(IBlockState state) {
        return true;
    }

    @Override
    @SuppressWarnings("deprecation")
    public int getWeakPower(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) {
        TileEntity te = world.getTileEntity(pos);
        return te instanceof TileSculkSensor ? ((TileSculkSensor) te).power() : 0;
    }

    /** 1.20's {@code getDirectSignal}: the block below, through {@code Direction.UP}. */
    @Override
    @SuppressWarnings("deprecation")
    public int getStrongPower(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) {
        return side == EnumFacing.UP ? state.getWeakPower(world, pos, side) : 0;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean hasComparatorInputOverride(IBlockState state) {
        return true;
    }

    @Override
    @SuppressWarnings("deprecation")
    public int getComparatorInputOverride(IBlockState state, World world, BlockPos pos) {
        TileEntity te = world.getTileEntity(pos);
        return te instanceof TileSculkSensor && phase(state) == Phase.ACTIVE ? ((TileSculkSensor) te).lastVibrationFrequency() : 0;
    }

    // --- shape -------------------------------------------------------------------------------

    @Override
    @SuppressWarnings("deprecation")
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return SHAPE;
    }

    /** Cutout, so the open parts of the tendrils' textures are see-through. */
    @Override
    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face) {
        return face == EnumFacing.DOWN ? BlockFaceShape.SOLID : BlockFaceShape.UNDEFINED;
    }

    @Override
    public PathNodeType getAiPathNodeType(IBlockState state, IBlockAccess world, BlockPos pos, @Nullable EntityLiving entity) {
        return PathNodeType.BLOCKED;
    }

    // --- state, tile entity, drops -------------------------------------------------------------

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, PHASE);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return phase(state).ordinal();
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getStateFromMeta(int meta) {
        Phase[] all = Phase.values();
        return this.getDefaultState().withProperty(PHASE, all[Math.min(meta & 3, all.length - 1)]);
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileSculkSensor();
    }

    /** 1.20's loot table: only with Silk Touch. */
    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return Items.AIR;
    }

    @Override
    public boolean canSilkHarvest(World world, BlockPos pos, IBlockState state, EntityPlayer player) {
        return true;
    }

    /** 1.20's {@code spawnAfterBreak}: 5 experience, none with Silk Touch (Forge asks only without it). */
    @Override
    public int getExpDrop(IBlockState state, IBlockAccess world, BlockPos pos, int fortune) {
        return 5;
    }
}
