package com.exiledradio.trailsandtalesbackport.sniffer;

import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.pathfinding.Path;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.stats.StatList;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.LootTable;

import javax.annotation.Nullable;

/**
 * 1.20's sniffer.
 *
 * <p>14 health, speed 0.1, 1.9 x 1.75 (0.4 lower while digging); eyes at 0.6 of its height. Floats, keeps out
 * of water unless burning or already in it. Bred and tempted with torchflower seeds; two sniffers in love lay
 * a sniffer egg (an item) instead of a baby, with the parents' usual cooldown, hearts and experience. A baby
 * (snifflet) takes 48000 ticks to grow up; feeding it seeds speeds that up as for any animal.
 *
 * <p>What it does is its {@link SnifferBrain}: it scents and sniffs, picks a spot of grass, dirt, podzol,
 * coarse dirt, moss, mud or rooted dirt it has not dug before within 10-18 blocks, walks there searching,
 * digs for 160-180 ticks dropping a torchflower seed or a pitcher pod (the {@code gameplay/sniffer_digging}
 * loot table) 120 ticks in, then stands up happy and does not dig again for 9600 ticks. Its state is sent
 * to the client, which plays the matching animation and sounds.
 */
public class EntitySniffer extends EntityAnimal {

    public static final float WIDTH = 1.9F;
    public static final float HEIGHT = 1.75F;
    public static final float DIGGING_BB_HEIGHT_OFFSET = 0.4F;
    public static final int BABY_AGE_TICKS = 48000;
    public static final int DIGGING_PARTICLES_DELAY_MS = 1700;
    public static final int DIGGING_PARTICLES_DURATION_MS = 6000;
    public static final int DIGGING_PARTICLES_AMOUNT = 30;
    public static final int DIGGING_DROP_SEED_OFFSET_TICKS = 120;
    /** Entity event: digging started (1.20's 63), which starts the digging sound on clients. */
    public static final byte EVENT_DIGGING = 63;

    public static final ResourceLocation DIGGING_LOOT = new ResourceLocation(
            com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport.MOD_ID, "gameplay/sniffer_digging");

    private static final DataParameter<Byte> STATE = EntityDataManager.createKey(EntitySniffer.class, DataSerializers.BYTE);
    private static final DataParameter<Integer> DROP_SEED_AT_TICK = EntityDataManager.createKey(EntitySniffer.class, DataSerializers.VARINT);

    public final AnimationClock feelingHappyAnimation = new AnimationClock();
    public final AnimationClock scentingAnimation = new AnimationClock();
    public final AnimationClock sniffingAnimation = new AnimationClock();
    public final AnimationClock diggingAnimation = new AnimationClock();
    public final AnimationClock risingAnimation = new AnimationClock();

    final SnifferBrain brain;

    public EntitySniffer(World world) {
        super(world);
        setSize(WIDTH, HEIGHT);
        this.brain = new SnifferBrain(this);
        ((net.minecraft.pathfinding.PathNavigateGround) getNavigator()).setCanSwim(true);
        setPathPriority(PathNodeType.WATER, -1.0F);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(STATE, (byte) SnifferState.IDLING.ordinal());
        this.dataManager.register(DROP_SEED_AT_TICK, 0);
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.1F);
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(14.0D);
    }

    @Override
    public float getEyeHeight() {
        return this.height * 0.6F;
    }

    // ------------------------------------------------------------------ state

    public SnifferState getState() {
        return SnifferState.of(this.dataManager.get(STATE));
    }

    private EntitySniffer setState(SnifferState state) {
        this.dataManager.set(STATE, (byte) state.ordinal());
        return this;
    }

    public boolean isSearching() {
        return getState() == SnifferState.SEARCHING;
    }

    public boolean isPanicking() {
        return this.brain.panicking;
    }

    public boolean isTempted() {
        return this.brain.tempted;
    }

    public boolean canSniff() {
        return !isTempted() && !isPanicking() && !isInWater() && !isInLove() && this.onGround && !isRiding();
    }

    /** @return ticks left before this sniffer sniffs for a new spot (0 when it may) */
    public int sniffCooldown() {
        return this.brain.sniffCooldown;
    }

    /** @return how many dug spots it remembers (up to 21, as 1.20) */
    public int exploredCount() {
        return this.brain.explored.size();
    }

    public boolean isExplored(BlockPos pos) {
        return this.brain.isExplored(pos);
    }

    public boolean canPlayDiggingSound() {
        SnifferState s = getState();
        return s == SnifferState.DIGGING || s == SnifferState.SEARCHING;
    }

    @Override
    public void notifyDataManagerChange(DataParameter<?> key) {
        if (STATE.equals(key)) {
            resetAnimations();
            switch (getState()) {
                case SCENTING:
                    this.scentingAnimation.startIfStopped(this.ticksExisted);
                    break;
                case SNIFFING:
                    this.sniffingAnimation.startIfStopped(this.ticksExisted);
                    break;
                case DIGGING:
                    this.diggingAnimation.startIfStopped(this.ticksExisted);
                    break;
                case RISING:
                    this.risingAnimation.startIfStopped(this.ticksExisted);
                    break;
                case FEELING_HAPPY:
                    this.feelingHappyAnimation.startIfStopped(this.ticksExisted);
                    break;
                default:
                    break;
            }
            refreshSize();
        }
        super.notifyDataManagerChange(key);
    }

    /** 1.20's digging dimensions: 0.4 lower while digging (1.12.2 then scales a baby by half). */
    private void refreshSize() {
        setSize(WIDTH, getState() == SnifferState.DIGGING ? HEIGHT - DIGGING_BB_HEIGHT_OFFSET : HEIGHT);
    }

    private void resetAnimations() {
        this.diggingAnimation.stop();
        this.sniffingAnimation.stop();
        this.risingAnimation.stop();
        this.feelingHappyAnimation.stop();
        this.scentingAnimation.stop();
    }

    /** 1.20's {@code transitionTo}: a new state, with its sound. */
    public EntitySniffer transitionTo(SnifferState state) {
        switch (state) {
            case SCENTING:
                setState(SnifferState.SCENTING);
                playSound(SnifferContent.SCENTING, 1.0F, isChild() ? 1.3F : 1.0F);
                break;
            case SNIFFING:
                playSound(SnifferContent.SNIFFING, 1.0F, 1.0F);
                setState(SnifferState.SNIFFING);
                break;
            case DIGGING:
                setState(SnifferState.DIGGING);
                this.dataManager.set(DROP_SEED_AT_TICK, this.ticksExisted + DIGGING_DROP_SEED_OFFSET_TICKS);
                this.world.setEntityState(this, EVENT_DIGGING);
                break;
            case RISING:
                playSound(SnifferContent.DIGGING_STOP, 1.0F, 1.0F);
                setState(SnifferState.RISING);
                break;
            case FEELING_HAPPY:
                playSound(SnifferContent.HAPPY, 1.0F, 1.0F);
                setState(SnifferState.FEELING_HAPPY);
                break;
            default:
                setState(state);
                break;
        }
        return this;
    }

    void onDiggingComplete(boolean success) {
        if (success) {
            this.brain.storeExplored(onPos());
        }
    }

    /** 1.20's {@code getOnPos}: the block the sniffer stands on. */
    private BlockPos onPos() {
        return new BlockPos(this.posX, this.posY - 0.2D, this.posZ);
    }

    // ------------------------------------------------------------------ digging

    /** 1.20's head position: 2.25 blocks ahead of the sniffer, on its look direction. */
    private Vec3d headPosition() {
        return getPositionVector().add(getLookVec().scale(2.25D));
    }

    public BlockPos headBlock() {
        Vec3d head = headPosition();
        return new BlockPos(head.x, this.posY + 0.2D, head.z);
    }

    /** 1.20's {@code calculateDigPosition}: five tries 10 to 18 blocks out for a block it may dig. */
    @Nullable
    BlockPos calculateDigPosition() {
        for (int i = 0; i < 5; i++) {
            Vec3d v = this.brain.landPos(10 + 2 * i, 3);
            if (v == null) {
                continue;
            }
            BlockPos p = new BlockPos(v);
            if (!this.world.getWorldBorder().contains(p)) {
                continue;
            }
            p = p.down();
            if (canDig(p)) {
                return p;
            }
        }
        return null;
    }

    public boolean canDig() {
        return !isPanicking() && !isTempted() && !isChild() && !isInWater() && this.onGround && !isRiding()
                && canDig(headBlock().down());
    }

    private boolean canDig(BlockPos pos) {
        if (!SnifferContent.isDiggable(this.world.getBlockState(pos)) || this.brain.isExplored(pos)) {
            return false;
        }
        Path path = getNavigator().getPathToPos(pos);
        if (path == null) {
            return false;
        }
        // 1.20's Path.canReach with accuracy 1: the path ends within one block (Manhattan) of the target.
        PathPoint end = path.getFinalPathPoint();
        return end != null && Math.abs(end.x - pos.getX()) + Math.abs(end.y - pos.getY()) + Math.abs(end.z - pos.getZ()) <= 1;
    }

    private void dropSeed() {
        if (this.world.isRemote || this.dataManager.get(DROP_SEED_AT_TICK) != this.ticksExisted) {
            return;
        }
        WorldServer server = (WorldServer) this.world;
        LootTable table = server.getLootTableManager().getLootTableFromLocation(DIGGING_LOOT);
        LootContext context = new LootContext.Builder(server).withLootedEntity(this).build();
        BlockPos head = headBlock();
        for (ItemStack stack : table.generateLootForPools(this.rand, context)) {
            EntityItem item = new EntityItem(server, head.getX(), head.getY(), head.getZ(), stack);
            item.setDefaultPickupDelay();
            server.spawnEntity(item);
        }
        playSound(SnifferContent.DROP_SEED, 1.0F, 1.0F);
    }

    /** 1.20's {@code emitDiggingParticles}: dirt bits and hit sounds from 1.7 s to 6 s into the dig. */
    private void emitDiggingParticles() {
        long time = this.diggingAnimation.isStarted() ? this.diggingAnimation.timeInMillis(this.ticksExisted) : 0L;
        if (this.world.isRemote && time > DIGGING_PARTICLES_DELAY_MS && time < DIGGING_PARTICLES_DURATION_MS) {
            BlockPos head = headBlock();
            IBlockState below = this.world.getBlockState(head.down());
            if (below.getRenderType() != EnumBlockRenderType.INVISIBLE) {
                for (int i = 0; i < DIGGING_PARTICLES_AMOUNT; i++) {
                    this.world.spawnParticle(EnumParticleTypes.BLOCK_CRACK, head.getX() + 0.5D, head.getY() + 0.5D - 0.65D,
                            head.getZ() + 0.5D, 0.0D, 0.0D, 0.0D, net.minecraft.block.Block.getStateId(below));
                }
                if (this.ticksExisted % 10 == 0) {
                    SoundType sound = below.getBlock().getSoundType(below, this.world, head.down(), this);
                    this.world.playSound(this.posX, this.posY, this.posZ, sound.getHitSound(), getSoundCategory(), 0.5F, 0.5F, false);
                }
            }
        }
    }

    @Override
    public void onUpdate() {
        switch (getState()) {
            case DIGGING:
                emitDiggingParticles();
                dropSeed();
                break;
            case SEARCHING:
                if (this.world.isRemote && this.ticksExisted % 20 == 0) {
                    this.world.playSound(this.posX, this.posY, this.posZ, SnifferContent.SEARCHING, getSoundCategory(), 1.0F, 1.0F, false);
                }
                break;
            default:
                break;
        }
        super.onUpdate();
    }

    @Override
    public void onLivingUpdate() {
        if (!this.world.isRemote) {
            // 1.20's onPathfindingStart/Done: water is avoided unless the sniffer is burning or in it.
            setPathPriority(PathNodeType.WATER, isBurning() || isInWater() ? 0.0F : -1.0F);
        }
        super.onLivingUpdate();
    }

    @Override
    protected void updateAITasks() {
        this.world.profiler.startSection("snifferBrain");
        this.brain.tick();
        this.world.profiler.endSection();
        super.updateAITasks();
    }

    @Override
    protected void jump() {
        super.jump();
        // 1.20: a jump from a standstill moves it forward a little, so it does not hop on the spot.
        if (getMoveHelper().getSpeed() > 0.0D && this.motionX * this.motionX + this.motionZ * this.motionZ < 0.01D) {
            moveRelative(0.0F, 0.0F, 1.0F, 0.1F);
        }
    }

    @Override
    public void handleStatusUpdate(byte id) {
        if (id == EVENT_DIGGING) {
            SnifferContent.proxy().diggingSound(this);
        } else {
            super.handleStatusUpdate(id);
        }
    }

    // ------------------------------------------------------------------ breeding and interaction

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == SnifferContent.torchflowerSeeds;
    }

    @Override
    public boolean processInteract(EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        boolean food = isBreedingItem(stack);
        boolean baby = isChild();
        boolean result = super.processInteract(player, hand);
        if (result && food) {
            this.world.playSound(null, this.posX, this.posY, this.posZ, SnifferContent.EAT, SoundCategory.NEUTRAL, 1.0F,
                    MathHelper.nextFloat(this.world.rand, 0.8F, 1.2F));
            if (baby && player instanceof EntityPlayerMP) {
                SnifferContent.grantLittleSniffs((EntityPlayerMP) player);
            }
        }
        return result;
    }

    @Override
    public boolean canMateWith(EntityAnimal other) {
        if (!(other instanceof EntitySniffer)) {
            return false;
        }
        return mateState(getState()) && mateState(((EntitySniffer) other).getState()) && super.canMateWith(other);
    }

    private static boolean mateState(SnifferState s) {
        return s == SnifferState.IDLING || s == SnifferState.SCENTING || s == SnifferState.FEELING_HAPPY;
    }

    /** 1.20's {@code spawnChildFromBreeding}: a sniffer egg item where the sniffer stands. */
    void spawnEggFromBreeding(EntitySniffer partner) {
        Item egg = SnifferContent.eggItem;
        EntityItem item = new EntityItem(this.world, this.posX, this.posY, this.posZ, new ItemStack(egg));
        item.setDefaultPickupDelay();
        finalizeBreeding(partner);
        playSound(SnifferContent.EGG_PLOP, 1.0F, (this.rand.nextFloat() - this.rand.nextFloat()) * 0.2F + 0.5F);
        this.world.spawnEntity(item);
    }

    /** 1.20's {@code finalizeSpawnChildFromBreeding} with no child entity. */
    private void finalizeBreeding(EntitySniffer partner) {
        EntityPlayerMP cause = getLoveCause();
        if (cause == null) {
            cause = partner.getLoveCause();
        }
        if (cause != null) {
            cause.addStat(StatList.ANIMALS_BRED);
            CriteriaTriggers.BRED_ANIMALS.trigger(cause, this, partner, null);
        }
        setGrowingAge(6000);
        partner.setGrowingAge(6000);
        resetInLove();
        partner.resetInLove();
        this.world.setEntityState(this, (byte) 18);
        if (this.world.getGameRules().getBoolean("doMobLoot")) {
            this.world.spawnEntity(new EntityXPOrb(this.world, this.posX, this.posY, this.posZ, this.rand.nextInt(7) + 1));
        }
    }

    @Override
    @Nullable
    public EntityAgeable createChild(EntityAgeable ageable) {
        return new EntitySniffer(this.world);
    }

    @Override
    public void onDeath(DamageSource cause) {
        transitionTo(SnifferState.IDLING);
        super.onDeath(cause);
    }

    // ------------------------------------------------------------------ sounds and the rest

    @Override
    @Nullable
    protected SoundEvent getAmbientSound() {
        SnifferState s = getState();
        return s == SnifferState.DIGGING || s == SnifferState.SEARCHING ? null : SnifferContent.IDLE;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SnifferContent.HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SnifferContent.DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, net.minecraft.block.Block block) {
        playSound(SnifferContent.STEP, 0.15F, 1.0F);
    }

    @Override
    public double getMountedYOffset() {
        return 1.8D;
    }

    @Override
    public int getVerticalFaceSpeed() {
        return 40;
    }

    @Override
    public int getHorizontalFaceSpeed() {
        return 10;
    }

    @Override
    @Nullable
    protected ResourceLocation getLootTable() {
        return null;
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return super.getRenderBoundingBox().grow(0.6D);
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        this.brain.write(tag);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        this.brain.read(tag);
    }
}
