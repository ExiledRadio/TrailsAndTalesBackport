package com.exiledradio.trailsandtalesbackport.camel;

import com.exiledradio.backportcore.gameevent.GameEvent;
import com.exiledradio.trailsandtalesbackport.common.Vibrations;
import net.minecraft.block.Block;
import net.minecraft.block.BlockConcretePowder;
import net.minecraft.block.BlockSand;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityLookHelper;
import net.minecraft.entity.ai.EntityMoveHelper;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.passive.AbstractHorse;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializer;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.pathfinding.PathNavigateGround;
import net.minecraft.stats.StatList;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

import javax.annotation.Nullable;

/**
 * 1.20's camel ({@code Camel}), on 1.12.2's horse.
 *
 * <h2>What it is built on</h2>
 * 1.12.2's {@link AbstractHorse}, as 1.20's camel is built on 1.20's: the saddle inventory and its screen,
 * the jump bar (1.12.2's {@code IJumpingMount}, here the dash), riding, eating grass, fall damage and the
 * saddle dropped on death all come from it. What the camel changes is overridden here, number for number:
 * <ul>
 *   <li>32 health, speed 0.09, jump strength 0.42, step height 1.5; 1.7 wide and 2.375 tall (0.945 sitting),
 *       eye height 2.275 (0.845 sitting); a baby is 0.45 of that.</li>
 *   <li>Always tame; never rears; carries two riders; bred and healed with cactus.</li>
 *   <li><b>Sitting</b>: the synced "last pose change tick" is the game time of the last sit or stand, negated
 *       while sitting, as in 1.20. Sitting down takes 40 ticks, standing up 52; while sitting or in either
 *       move it refuses to move, and a rider pressing forward stands it up.</li>
 *   <li><b>Dash</b> (the jump key): a push of 22.2222 x speed along the camel's facing and up by 1.4285 x jump
 *       strength, scaled by the jump bar; then a 55-tick cooldown, the dash-ready sound when it ends.</li>
 *   <li>The AI is 1.20's brain ({@link CamelBrain}), the body and head turn as 1.20's do (30 degrees), and the
 *       walk animation follows 1.20's camel (6x the distance, eased by 0.2, none while sitting or dashing).</li>
 * </ul>
 */
public class EntityCamel extends AbstractHorse {

    public static final float BABY_SCALE = 0.45F;
    public static final int DASH_COOLDOWN_TICKS = 55;
    public static final int MAX_HEAD_Y_ROT = 30;
    static final float RUNNING_SPEED_BONUS = 0.1F;
    static final float DASH_VERTICAL_MOMENTUM = 1.4285F;
    static final float DASH_HORIZONTAL_MOMENTUM = 22.2222F;
    static final int SITDOWN_DURATION_TICKS = 40;
    static final int STANDUP_DURATION_TICKS = 52;
    static final float SITTING_HEIGHT_DIFFERENCE = 1.43F;
    static final float WIDTH = 1.7F;
    static final float HEIGHT = 2.375F;
    static final float EYE_HEIGHT = 2.275F;
    static final float SITTING_EYE_HEIGHT = 0.845F;

    /** 1.12.2 has no long serializer: this one is registered with Forge (see {@code CamelContent}). */
    public static final DataSerializer<Long> LONG = new DataSerializer<Long>() {
        @Override
        public void write(PacketBuffer buf, Long value) {
            buf.writeVarLong(value);
        }

        @Override
        public Long read(PacketBuffer buf) {
            return buf.readVarLong();
        }

        @Override
        public DataParameter<Long> createKey(int id) {
            return new DataParameter<Long>(id, this);
        }

        @Override
        public Long copyValue(Long value) {
            return value;
        }
    };

    private static final DataParameter<Boolean> DASH = EntityDataManager.createKey(EntityCamel.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Long> LAST_POSE_CHANGE_TICK = EntityDataManager.createKey(EntityCamel.class, LONG);

    // Client: 1.20's animation states.
    public final AnimationClock sitAnimation = new AnimationClock();
    public final AnimationClock sitPoseAnimation = new AnimationClock();
    public final AnimationClock sitUpAnimation = new AnimationClock();
    public final AnimationClock idleAnimation = new AnimationClock();
    public final AnimationClock dashAnimation = new AnimationClock();

    final CamelBrain brain;
    private int dashCooldown;
    private int idleAnimationTimeout;

    public EntityCamel(World world) {
        super(world);
        refreshDimensions();
        this.stepHeight = 1.5F;
        this.moveHelper = new MoveHelper();
        ObfuscationReflectionHelper.setPrivateValue(EntityLiving.class, this, new LookHelper(), "field_70749_g");
        ((PathNavigateGround) getNavigator()).setCanSwim(true);
        this.brain = new CamelBrain(this);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(DASH, false);
        this.dataManager.register(LAST_POSE_CHANGE_TICK, 0L);
    }

    @Override
    protected void initEntityAI() {
        // 1.20's camel has no goals: its brain (CamelBrain) runs from updateAITasks.
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(32.0D);
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.09F);
        getEntityAttribute(JUMP_STRENGTH).setBaseValue(0.42F);
    }

    @Override
    protected net.minecraft.entity.EntityBodyHelper createBodyHelper() {
        return new BodyHelper(this);
    }

    // ------------------------------------------------------------------ spawning and saving

    /**
     * 1.20 spawns camels grown up (a horse's one-in-five foal is 1.12.2's) and as if standing for good, so a
     * new camel can sit at once.
     */
    @Nullable
    @Override
    public IEntityLivingData onInitialSpawn(DifficultyInstance difficulty, @Nullable IEntityLivingData data) {
        int age = getGrowingAge();
        IEntityLivingData result = super.onInitialSpawn(difficulty, data);
        setGrowingAge(age);
        resetLastPoseChangeTickToFullStand(this.world.getTotalWorldTime());
        return result;
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        tag.setLong("LastPoseTick", this.dataManager.get(LAST_POSE_CHANGE_TICK));
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        resetLastPoseChangeTick(tag.getLong("LastPoseTick"));
    }

    // ------------------------------------------------------------------ size

    /** 1.20's dimensions: standing or sitting, times 0.45 for a baby. */
    private void refreshDimensions() {
        setSize(WIDTH, isCamelSitting() ? HEIGHT - SITTING_HEIGHT_DIFFERENCE : HEIGHT);
        setScale(getAgeScale());
    }

    @Override
    public void setScaleForAge(boolean child) {
        setScale(child ? BABY_SCALE : 1.0F);
    }

    public float getAgeScale() {
        return isChild() ? BABY_SCALE : 1.0F;
    }

    @Override
    public float getEyeHeight() {
        return (isCamelSitting() ? SITTING_EYE_HEIGHT : EYE_HEIGHT) * getAgeScale();
    }

    @Override
    public void notifyDataManagerChange(DataParameter<?> key) {
        if (!this.firstUpdate && DASH.equals(key)) {
            this.dashCooldown = this.dashCooldown == 0 ? DASH_COOLDOWN_TICKS : this.dashCooldown;
        }
        super.notifyDataManagerChange(key);
        if (LAST_POSE_CHANGE_TICK.equals(key)) {
            refreshDimensions();
        }
    }

    // ------------------------------------------------------------------ the tick

    @Override
    protected void updateAITasks() {
        this.world.profiler.startSection("camelBrain");
        this.brain.tick();
        this.world.profiler.endSection();
        super.updateAITasks();
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (isDashing() && this.dashCooldown < 50 && (this.onGround || isInWater() || isInLava() || isRiding())) {
            setDashing(false);
        }
        if (this.dashCooldown > 0) {
            this.dashCooldown--;
            if (this.dashCooldown == 0) {
                this.world.playSound(null, new BlockPos(this), CamelContent.DASH_READY, SoundCategory.NEUTRAL, 1.0F, 1.0F);
            }
        }
        if (this.world.isRemote) {
            setupAnimationStates();
        }
        if (refuseToMove()) {
            clampHeadRotationToBody();
        }
        if (isCamelSitting() && isInWater()) {
            standUpInstantly();
        }
    }

    private void setupAnimationStates() {
        if (this.idleAnimationTimeout <= 0) {
            this.idleAnimationTimeout = this.rand.nextInt(40) + 80;
            this.idleAnimation.start(this.ticksExisted);
        } else {
            this.idleAnimationTimeout--;
        }
        if (isCamelVisuallySitting()) {
            this.sitUpAnimation.stop();
            this.dashAnimation.stop();
            if (isVisuallySittingDown()) {
                this.sitAnimation.startIfStopped(this.ticksExisted);
                this.sitPoseAnimation.stop();
            } else {
                this.sitAnimation.stop();
                this.sitPoseAnimation.startIfStopped(this.ticksExisted);
            }
        } else {
            this.sitAnimation.stop();
            this.sitPoseAnimation.stop();
            this.dashAnimation.animateWhen(isDashing(), this.ticksExisted);
            this.sitUpAnimation.animateWhen(isInPoseTransition() && getPoseTime() >= 0L, this.ticksExisted);
        }
    }

    /** 1.20's {@code Mob.clampHeadRotationToBody}. */
    private void clampHeadRotationToBody() {
        float head = this.rotationYawHead;
        float diff = MathHelper.wrapDegrees(this.renderYawOffset - head);
        float clamped = MathHelper.clamp(diff, -MAX_HEAD_Y_ROT, MAX_HEAD_Y_ROT);
        setRotationYawHead(head + diff - clamped);
    }

    // ------------------------------------------------------------------ moving and riding

    @Override
    public void travel(float strafe, float vertical, float forward) {
        Entity controller = getControllingPassenger();
        if (isBeingRidden() && controller instanceof EntityPlayer && isHorseSaddled() && isEntityAlive()) {
            travelRidden((EntityPlayer) controller);
        } else {
            camelTravel(strafe, vertical, forward, false);
        }
    }

    /** 1.20's {@code LivingEntity.travelRidden} with the camel's (and horse's) ridden input, turn and dash. */
    private void travelRidden(EntityPlayer player) {
        float strafe = 0.0F;
        float forward = 0.0F;
        if (!refuseToMove()) {
            strafe = player.moveStrafing * 0.5F;
            forward = player.moveForward;
            if (forward <= 0.0F) {
                forward *= 0.25F;
            }
        }
        tickRidden(player, forward);
        if (canPassengerSteer()) {
            setAIMoveSpeed(getRiddenSpeed(player));
            camelTravel(strafe, 0.0F, forward, true);
        } else {
            this.motionX = 0.0D;
            this.motionY = 0.0D;
            this.motionZ = 0.0D;
            updateWalkAnimation();
        }
    }

    /** 1.20's {@code AbstractHorse.tickRidden} and the camel's: turn with the rider, dash, stand up to go. */
    private void tickRidden(EntityPlayer player, float forward) {
        float yaw = refuseToMove() ? this.rotationYaw : player.rotationYaw;
        float pitch = refuseToMove() ? this.rotationPitch : player.rotationPitch * 0.5F;
        setRotation(yaw, pitch);
        this.prevRotationYaw = this.renderYawOffset = this.rotationYawHead = this.rotationYaw;
        if (canPassengerSteer() && this.onGround) {
            setHorseJumping(false);
            if (this.jumpPower > 0.0F && !isHorseJumping()) {
                executeRidersJump(this.jumpPower);
            }
            this.jumpPower = 0.0F;
        }
        if (player.moveForward > 0.0F && isCamelSitting() && !isInPoseTransition()) {
            standUp();
        }
    }

    /**
     * 1.20's {@code Camel.travel}: on the ground while it refuses to move, it keeps only its fall. Movement
     * itself is 1.12.2's (through the horse's riderless branch, which is the plain living-entity move), with
     * 1.20's ridden air control (a tenth of the speed) and the camel's walk animation.
     */
    private void camelTravel(float strafe, float vertical, float forward, boolean ridden) {
        if (refuseToMove() && this.onGround) {
            this.motionX = 0.0D;
            this.motionZ = 0.0D;
            strafe = 0.0F;
            forward = 0.0F;
        }
        float swing = this.limbSwing;
        float swingAmount = this.limbSwingAmount;
        float prevSwingAmount = this.prevLimbSwingAmount;
        this.plainTravel = true;
        this.riddenTravel = ridden;
        try {
            super.travel(strafe, vertical, forward);
        } finally {
            this.plainTravel = false;
            this.riddenTravel = false;
        }
        // 1.12.2's own walk animation has just run; 1.20's camel replaces it.
        this.limbSwing = swing;
        this.limbSwingAmount = swingAmount;
        this.prevLimbSwingAmount = prevSwingAmount;
        updateWalkAnimation();
    }

    private boolean plainTravel;
    private boolean riddenTravel;

    /** False while the camel moves itself, so the horse's travel takes its riderless branch. */
    @Override
    public boolean canBeSteered() {
        return !this.plainTravel && super.canBeSteered();
    }

    /** 1.20's flying speed for a mob a player rides: a tenth of its speed, in place of 1.12.2's 0.02. */
    @Override
    public void moveRelative(float strafe, float up, float forward, float friction) {
        if (this.riddenTravel && !this.onGround && !isInWater() && !isInLava()) {
            friction = getAIMoveSpeed() * 0.1F;
        }
        super.moveRelative(strafe, up, forward, friction);
    }

    /** 1.20's camel {@code updateWalkAnimation}: 6x the distance moved, eased by 0.2, none sitting or dashing. */
    private void updateWalkAnimation() {
        double dx = this.posX - this.prevPosX;
        double dz = this.posZ - this.prevPosZ;
        float distance = MathHelper.sqrt(dx * dx + dz * dz);
        float target = !isCamelSitting() && !this.dashAnimation.isStarted() ? Math.min(distance * 6.0F, 1.0F) : 0.0F;
        this.prevLimbSwingAmount = this.limbSwingAmount;
        this.limbSwingAmount += (target - this.limbSwingAmount) * 0.2F;
        this.limbSwing += this.limbSwingAmount;
    }

    /** 1.20: the speed attribute, plus 0.1 while the rider sprints and the dash is ready. */
    public float getRiddenSpeed(EntityPlayer player) {
        float bonus = player.isSprinting() && getJumpCooldown() == 0 ? RUNNING_SPEED_BONUS : 0.0F;
        return (float) getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getAttributeValue() + bonus;
    }

    /** 1.20's {@code executeRidersJump}: the dash. */
    public void executeRidersJump(float scale) {
        double jump = getJumpPower();
        double speed = getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getAttributeValue();
        float yaw = this.rotationYaw * (float) (Math.PI / 180.0D);
        double push = DASH_HORIZONTAL_MOMENTUM * scale * speed * getBlockSpeedFactor();
        this.motionX += -MathHelper.sin(yaw) * push;
        this.motionZ += MathHelper.cos(yaw) * push;
        this.motionY += DASH_VERTICAL_MOMENTUM * scale * jump;
        this.dashCooldown = DASH_COOLDOWN_TICKS;
        setDashing(true);
        this.isAirBorne = true;
    }

    /** 1.20's {@code LivingEntity.getJumpPower()}: jump strength, plus 0.1 a level of Jump Boost. */
    private double getJumpPower() {
        double jump = getHorseJumpStrength();
        if (isPotionActive(MobEffects.JUMP_BOOST)) {
            jump += (getActivePotionEffect(MobEffects.JUMP_BOOST).getAmplifier() + 1) * 0.1F;
        }
        return jump;
    }

    /** 1.20's block speed factor: soul sand (the one 1.12.2 block 1.20 slows) at the feet or just below. */
    private float getBlockSpeedFactor() {
        BlockPos at = new BlockPos(this);
        Block feet = this.world.getBlockState(at).getBlock();
        if (feet == Blocks.SOUL_SAND) {
            return 0.4F;
        }
        if (feet == Blocks.WATER || feet == Blocks.FLOWING_WATER) {
            return 1.0F;
        }
        BlockPos below = new BlockPos(this.posX, getEntityBoundingBox().minY - 0.5000001D, this.posZ);
        return this.world.getBlockState(below).getBlock() == Blocks.SOUL_SAND ? 0.4F : 1.0F;
    }

    public boolean refuseToMove() {
        return isCamelSitting() || isInPoseTransition();
    }

    /** 1.20: a player steers only a saddled camel (1.12.2's horse lets any first rider). */
    @Nullable
    @Override
    public Entity getControllingPassenger() {
        if (isHorseSaddled() && !getPassengers().isEmpty() && getPassengers().get(0) instanceof EntityPlayer) {
            return getPassengers().get(0);
        }
        return null;
    }

    @Override
    public void updatePassenger(Entity passenger) {
        if (!isPassenger(passenger)) {
            return;
        }
        Vec3d at = getPassengerAttachmentPoint(passenger, 1.0F);
        double sink = passenger instanceof EntityPlayer ? 0.6D : -passenger.getYOffset();
        passenger.setPosition(this.posX + at.x, this.posY + at.y - sink, this.posZ + at.z);
        if (passenger instanceof EntityLivingBase) {
            ((EntityLivingBase) passenger).renderYawOffset = this.renderYawOffset;
        }
    }

    /**
     * 1.20's {@code getPassengerAttachmentPoint}: the first rider 0.5 forward, the second 0.7 back (0.2 further
     * forward for an animal), at the body's height as the sitting animations move it.
     */
    public Vec3d getPassengerAttachmentPoint(Entity passenger, float partialTicks) {
        int index = Math.max(getPassengers().indexOf(passenger), 0);
        boolean first = index == 0;
        float scale = getAgeScale();
        float z = 0.5F;
        float y = (float) (this.isDead ? 0.01F : getBodyAnchorAnimationYOffset(first, 0.0F, this.height, scale));
        if (getPassengers().size() > 1) {
            if (!first) {
                z = -0.7F;
            }
            if (passenger instanceof EntityAnimal) {
                z += 0.2F;
            }
        }
        z *= scale;
        float yaw = this.rotationYaw * (float) (Math.PI / 180.0D);
        return new Vec3d(-MathHelper.sin(yaw) * z, y, MathHelper.cos(yaw) * z);
    }

    @Override
    public double getMountedYOffset() {
        return getBodyAnchorAnimationYOffset(true, 0.0F, this.height, getAgeScale());
    }

    /** 1.20's {@code getBodyAnchorAnimationYOffset}, following the sit and stand animations. */
    double getBodyAnchorAnimationYOffset(boolean first, float partialTicks, float height, float scale) {
        double y = height - 0.375F * scale;
        float sittingDiff = scale * SITTING_HEIGHT_DIFFERENCE;
        float lower = sittingDiff - scale * 0.2F;
        float raise = sittingDiff - lower;
        boolean transition = isInPoseTransition();
        boolean sitting = isCamelSitting();
        if (transition) {
            int duration = sitting ? SITDOWN_DURATION_TICKS : STANDUP_DURATION_TICKS;
            int split;
            float factor;
            if (sitting) {
                split = 28;
                factor = first ? 0.5F : 0.1F;
            } else {
                split = first ? 24 : 32;
                factor = first ? 0.6F : 0.35F;
            }
            float t = MathHelper.clamp(getPoseTime() + partialTicks, 0.0F, duration);
            boolean firstPart = t < split;
            float alpha = firstPart ? t / split : (t - split) / (duration - split);
            float mid = sittingDiff - factor * lower;
            y += sitting
                    ? lerp(alpha, firstPart ? sittingDiff : mid, firstPart ? mid : raise)
                    : lerp(alpha, firstPart ? raise - sittingDiff : raise - mid, firstPart ? raise - mid : 0.0F);
        }
        if (sitting && !transition) {
            y += raise;
        }
        return y;
    }

    private static float lerp(float alpha, float a, float b) {
        return a + alpha * (b - a);
    }

    /** 1.20: two riders (it checks {@code <= 2}, as 1.20 does; right-clicking seats only up to two). */
    @Override
    protected boolean canFitPassenger(Entity passenger) {
        return getPassengers().size() <= 2;
    }

    // ------------------------------------------------------------------ the dash (jump bar)

    /** 1.20's {@code onPlayerJump}: only saddled, on the ground, with the dash ready. Client side. */
    @Override
    public void setJumpPower(int power) {
        if (isHorseSaddled() && this.dashCooldown <= 0 && this.onGround) {
            if (power < 0) {
                power = 0;
            }
            this.jumpPower = power >= 90 ? 1.0F : 0.4F + 0.4F * power / 90.0F;
        }
    }

    /**
     * 1.20: the jump bar shows (and so the dash works) while saddled and not sitting. On a client only for
     * the rider who steers, as 1.20 shows it only to the driver, not to the second rider.
     */
    @Override
    public boolean canJump() {
        if (refuseToMove() || !isHorseSaddled()) {
            return false;
        }
        if (this.world.isRemote) {
            Entity controller = getControllingPassenger();
            return controller instanceof EntityPlayer && ((EntityPlayer) controller).isUser();
        }
        return true;
    }

    /** Server: the rider dashed. 1.20's client never sends it during the cooldown; 1.12.2's may, so it is checked. */
    @Override
    public void handleStartJump(int power) {
        if (this.dashCooldown > 0) {
            return;
        }
        playSound(CamelContent.DASH, getSoundVolume(), getSoundPitch());
        Vibrations.entity(this, GameEvent.ENTITY_ACTION);
        setDashing(true);
    }

    @Override
    public void handleStopJump() {
    }

    public int getJumpCooldown() {
        return this.dashCooldown;
    }

    public boolean isDashing() {
        return this.dataManager.get(DASH);
    }

    public void setDashing(boolean dashing) {
        this.dataManager.set(DASH, dashing);
    }

    // ------------------------------------------------------------------ sitting

    public boolean isCamelSitting() {
        return this.dataManager.get(LAST_POSE_CHANGE_TICK) < 0L;
    }

    public boolean isCamelVisuallySitting() {
        return getPoseTime() < 0L != isCamelSitting();
    }

    public boolean isInPoseTransition() {
        return getPoseTime() < (isCamelSitting() ? SITDOWN_DURATION_TICKS : STANDUP_DURATION_TICKS);
    }

    private boolean isVisuallySittingDown() {
        return isCamelSitting() && getPoseTime() < SITDOWN_DURATION_TICKS && getPoseTime() >= 0L;
    }

    public void sitDown() {
        if (!isCamelSitting()) {
            playSound(CamelContent.SIT, getSoundVolume(), getSoundPitch());
            Vibrations.entity(this, GameEvent.ENTITY_ACTION);
            resetLastPoseChangeTick(-this.world.getTotalWorldTime());
        }
    }

    public void standUp() {
        if (isCamelSitting()) {
            playSound(CamelContent.STAND, getSoundVolume(), getSoundPitch());
            Vibrations.entity(this, GameEvent.ENTITY_ACTION);
            resetLastPoseChangeTick(this.world.getTotalWorldTime());
        }
    }

    public void standUpInstantly() {
        Vibrations.entity(this, GameEvent.ENTITY_ACTION);
        resetLastPoseChangeTickToFullStand(this.world.getTotalWorldTime());
    }

    public void resetLastPoseChangeTick(long tick) {
        this.dataManager.set(LAST_POSE_CHANGE_TICK, tick);
    }

    private void resetLastPoseChangeTickToFullStand(long time) {
        resetLastPoseChangeTick(Math.max(0L, time - STANDUP_DURATION_TICKS - 1L));
    }

    public long getPoseTime() {
        return this.world.getTotalWorldTime() - Math.abs(this.dataManager.get(LAST_POSE_CHANGE_TICK));
    }

    /** 1.20's {@code wouldNotSuffocateAtTargetPose}: room for the other pose's box. */
    public boolean canCamelChangePose() {
        float scale = getAgeScale();
        float h = (isCamelSitting() ? HEIGHT : HEIGHT - SITTING_HEIGHT_DIFFERENCE) * scale;
        double half = WIDTH * scale / 2.0D;
        AxisAlignedBB box = new AxisAlignedBB(this.posX - half, this.posY, this.posZ - half,
                this.posX + half, this.posY + h, this.posZ + half).shrink(1.0E-7D);
        return !this.world.collidesWithAnyBlock(box);
    }

    @Override
    protected void onLeashDistance(float distance) {
        if (distance > 6.0F && isCamelSitting() && !isInPoseTransition() && canCamelChangePose()) {
            standUp();
        }
    }

    @Override
    protected void damageEntity(DamageSource source, float amount) {
        standUpInstantly();
        super.damageEntity(source, amount);
    }

    public boolean isPanicking() {
        return this.brain.panicking;
    }

    // ------------------------------------------------------------------ interaction

    /**
     * 1.20's {@code mobInteract}: sneaking opens the inventory; a saddle is put on; cactus feeds; otherwise a
     * player gets on, while there are fewer than two riders.
     */
    @Override
    public boolean processInteract(EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (player.isSneaking() && !isChild()) {
            openGUI(player);
            return true;
        }
        if (stack.getItem() == Items.SADDLE) {
            if (isEntityAlive() && !isHorseSaddled() && !isChild()) {
                if (!this.world.isRemote) {
                    this.horseChest.setInventorySlotContents(0, new ItemStack(Items.SADDLE));
                    stack.shrink(1);
                }
                return true;
            }
        } else if (stack.interactWithEntity(player, this, hand)) {
            return true;
        }
        if (isBreedingItem(stack)) {
            if (handleEating(player, stack) && !player.capabilities.isCreativeMode) {
                stack.shrink(1);
            }
            return true;
        }
        if (getPassengers().size() < 2 && !isChild()) {
            mountTo(player);
        }
        return true;
    }

    /** 1.20 opens the camel's inventory to anyone, whoever rides it, while it is not a baby. */
    @Override
    public void openGUI(EntityPlayer player) {
        if (!this.world.isRemote) {
            this.horseChest.setCustomName(getName());
            player.openGuiHorseInventory(this, this.horseChest);
        }
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == Item.getItemFromBlock(Blocks.CACTUS);
    }

    /** 1.20's camel {@code handleEating}: heals 2, falls in love when grown, grows a baby by 10 seconds. */
    @Override
    protected boolean handleEating(EntityPlayer player, ItemStack stack) {
        if (!isBreedingItem(stack)) {
            return false;
        }
        boolean heal = getHealth() < getMaxHealth();
        if (heal) {
            heal(2.0F);
        }
        boolean love = isTame() && getGrowingAge() == 0 && !isInLove();
        if (love) {
            setInLove(player);
        }
        boolean baby = isChild();
        if (baby) {
            this.world.spawnParticle(EnumParticleTypes.VILLAGER_HAPPY, this.posX + (this.rand.nextDouble() * 2.0D - 1.0D) * this.width,
                    this.posY + this.rand.nextDouble() * this.height + 0.5D,
                    this.posZ + (this.rand.nextDouble() * 2.0D - 1.0D) * this.width, 0.0D, 0.0D, 0.0D);
            if (!this.world.isRemote) {
                addGrowth(10);
            }
        }
        if (!heal && !love && !baby) {
            return false;
        }
        if (!isSilent()) {
            this.world.playSound(null, this.posX, this.posY, this.posZ, CamelContent.EAT, getSoundCategory(), 1.0F,
                    1.0F + (this.rand.nextFloat() - this.rand.nextFloat()) * 0.2F);
        }
        Vibrations.entity(this, GameEvent.EAT);
        return true;
    }

    /** The saddle sound is the camel's own (1.20's {@code getSaddleSoundEvent}). */
    @Override
    public void onInventoryChanged(IInventory inventory) {
        boolean wasSaddled = isHorseSaddled();
        updateHorseSlots();
        if (this.ticksExisted > 20 && !wasSaddled && isHorseSaddled()) {
            playSound(CamelContent.SADDLE, 0.5F, 1.0F);
        }
    }

    @Override
    public boolean isTame() {
        return true;
    }

    // ------------------------------------------------------------------ breeding

    @Override
    public boolean canMateWith(EntityAnimal other) {
        return other != this && other instanceof EntityCamel && canMate() && ((EntityCamel) other).canMate();
    }

    @Override
    public EntityAgeable createChild(EntityAgeable other) {
        return new EntityCamel(this.world);
    }

    /** 1.20's {@code Animal.spawnChildFromBreeding}, with Forge's baby event as 1.12.2's breeding fires it. */
    void spawnChildFromBreeding(EntityCamel mate) {
        EntityAgeable child = createChild(mate);
        BabyEntitySpawnEvent event = new BabyEntitySpawnEvent(this, mate, child);
        boolean cancelled = MinecraftForge.EVENT_BUS.post(event);
        child = event.getChild();
        if (cancelled || child == null) {
            setGrowingAge(6000);
            mate.setGrowingAge(6000);
            resetInLove();
            mate.resetInLove();
            return;
        }
        EntityPlayerMP cause = getLoveCause();
        if (cause == null) {
            cause = mate.getLoveCause();
        }
        if (cause != null) {
            cause.addStat(StatList.ANIMALS_BRED);
            net.minecraft.advancements.CriteriaTriggers.BRED_ANIMALS.trigger(cause, this, mate, child);
        }
        setGrowingAge(6000);
        mate.setGrowingAge(6000);
        resetInLove();
        mate.resetInLove();
        child.setGrowingAge(-24000);
        child.setLocationAndAngles(this.posX, this.posY, this.posZ, 0.0F, 0.0F);
        this.world.spawnEntity(child);
        this.world.setEntityState(this, (byte) 18);
        if (this.world.getGameRules().getBoolean("doMobLoot")) {
            this.world.spawnEntity(new EntityXPOrb(this.world, this.posX, this.posY, this.posZ, this.rand.nextInt(7) + 1));
        }
    }

    // ------------------------------------------------------------------ sounds

    @Override
    protected SoundEvent getAmbientSound() {
        return CamelContent.AMBIENT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return CamelContent.DEATH;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return CamelContent.HURT;
    }

    @Nullable
    @Override
    protected SoundEvent getAngrySound() {
        return null;
    }

    /** 1.20: the sand step on {@code #camel_sand_step_sound_blocks} (sand, red sand, suspicious sand, concrete powder). */
    @Override
    protected void playStepSound(BlockPos pos, Block block) {
        playSound(isSandStep(block) ? CamelContent.STEP_SAND : CamelContent.STEP, 1.0F, 1.0F);
    }

    static boolean isSandStep(Block block) {
        if (block instanceof BlockSand || block instanceof BlockConcretePowder) {
            return true;
        }
        return block.getRegistryName() != null && "suspicious_sand".equals(block.getRegistryName().getPath());
    }

    // ------------------------------------------------------------------ controls

    /** 1.20's {@code CamelMoveControl}: a walk order stands a sitting camel up first. */
    final class MoveHelper extends EntityMoveHelper {
        MoveHelper() {
            super(EntityCamel.this);
        }

        @Override
        public void onUpdateMoveHelper() {
            EntityCamel camel = EntityCamel.this;
            if (this.action == Action.MOVE_TO && !camel.getLeashed() && camel.isCamelSitting()
                    && !camel.isInPoseTransition() && camel.canCamelChangePose()) {
                camel.standUp();
            }
            super.onUpdateMoveHelper();
        }
    }

    /** 1.20's {@code CamelLookControl}: still while ridden; otherwise the head stays within 30 degrees while walking. */
    final class LookHelper extends EntityLookHelper {
        LookHelper() {
            super(EntityCamel.this);
        }

        @Override
        public void onUpdateLook() {
            EntityCamel camel = EntityCamel.this;
            if (camel.getControllingPassenger() != null) {
                return;
            }
            super.onUpdateLook();
            if (!camel.getNavigator().noPath()) {
                camel.rotationYawHead = rotateIfNecessary(camel.rotationYawHead, camel.renderYawOffset, MAX_HEAD_Y_ROT);
            }
        }
    }

    /** 1.20's {@code Mth.rotateIfNecessary}. */
    static float rotateIfNecessary(float angle, float towards, float max) {
        float diff = MathHelper.wrapDegrees(towards - angle);
        float clamped = MathHelper.clamp(diff, -max, max);
        return towards - clamped;
    }

    /** 1.20's {@code BodyRotationControl} with the camel's 30 degrees, still while it refuses to move. */
    static final class BodyHelper extends net.minecraft.entity.EntityBodyHelper {
        private final EntityCamel camel;
        private int headStableTime;
        private float lastStableYHeadRot;

        BodyHelper(EntityCamel camel) {
            super(camel);
            this.camel = camel;
        }

        @Override
        public void updateRenderAngles() {
            EntityCamel c = this.camel;
            if (c.refuseToMove()) {
                return;
            }
            double dx = c.posX - c.prevPosX;
            double dz = c.posZ - c.prevPosZ;
            if (dx * dx + dz * dz > 2.5E-7D) {
                c.renderYawOffset = c.rotationYaw;
                c.rotationYawHead = rotateIfNecessary(c.rotationYawHead, c.renderYawOffset, MAX_HEAD_Y_ROT);
                this.lastStableYHeadRot = c.rotationYawHead;
                this.headStableTime = 0;
            } else if (c.getPassengers().isEmpty() || !(c.getPassengers().get(0) instanceof EntityLiving)) {
                if (Math.abs(c.rotationYawHead - this.lastStableYHeadRot) > 15.0F) {
                    this.headStableTime = 0;
                    this.lastStableYHeadRot = c.rotationYawHead;
                    c.renderYawOffset = rotateIfNecessary(c.renderYawOffset, c.rotationYawHead, MAX_HEAD_Y_ROT);
                } else {
                    this.headStableTime++;
                    if (this.headStableTime > 10) {
                        float f = MathHelper.clamp((this.headStableTime - 10) / 10.0F, 0.0F, 1.0F);
                        c.renderYawOffset = rotateIfNecessary(c.renderYawOffset, c.rotationYawHead, MAX_HEAD_Y_ROT * (1.0F - f));
                    }
                }
            }
        }
    }
}
