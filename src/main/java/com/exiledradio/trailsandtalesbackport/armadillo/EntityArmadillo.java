package com.exiledradio.trailsandtalesbackport.armadillo;

import com.exiledradio.backportcore.gameevent.GameEvent;
import com.exiledradio.trailsandtalesbackport.common.Vibrations;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityLookHelper;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.pathfinding.PathNavigateGround;
import net.minecraft.stats.StatList;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.LootTable;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 1.20.5's armadillo (current Java where they differ).
 *
 * <p>12 health, speed 0.14, 0.7 x 0.65, eyes at 0.26 (a baby is 0.6 the size, eyes at 0.21875). Floats. Bred
 * and tempted with spider eyes. What it does is its {@link ArmadilloBrain}: it strolls, looks around, follows a
 * player holding spider eyes, and when something scares it - an undead mob, a sprinting or riding player, or
 * whoever hurt it, within 7 blocks across and 2 up or down - it rolls up into its shell, peeks out now and
 * then, and unrolls about 3 seconds after the danger is gone. Rolled up it takes {@code (damage - 1) / 2}, does
 * not turn, cannot be fed, and spiders stop avoiding it.
 *
 * <p>An adult sheds a scute every 5-10 minutes ({@code gameplay/armadillo_shed}); brushing an adult gives one
 * at once ({@code brush/armadillo}) and costs the brush 16 durability.
 */
public class EntityArmadillo extends EntityAnimal {

    public static final float WIDTH = 0.7F;
    public static final float HEIGHT = 0.65F;
    public static final float EYE_HEIGHT = 0.26F;
    /** Current Java's baby eye height (1.20.5 scaled the adult's). */
    public static final float BABY_EYE_HEIGHT = 0.21875F;
    public static final float BABY_SCALE = 0.6F;
    public static final float MAX_HEAD_ROTATION_EXTENT = 32.5F;
    /** Entity event: peek out of the shell (1.20.5's 64), which plays the peek animation and sound on clients. */
    public static final byte EVENT_PEEK = 64;

    public static final ResourceLocation SHED_LOOT = new ResourceLocation(
            com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport.MOD_ID, "gameplay/armadillo_shed");
    public static final ResourceLocation BRUSH_LOOT = new ResourceLocation(
            com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport.MOD_ID, "brush/armadillo");

    private static final DataParameter<Byte> STATE = EntityDataManager.createKey(EntityArmadillo.class, DataSerializers.BYTE);

    public final AnimationClock rollOutAnimation = new AnimationClock();
    public final AnimationClock rollUpAnimation = new AnimationClock();
    public final AnimationClock peekAnimation = new AnimationClock();

    final ArmadilloBrain brain;
    private long inStateTicks;
    private int scuteTime;
    private boolean peekReceivedClient;

    public EntityArmadillo(World world) {
        super(world);
        setSize(WIDTH, HEIGHT);
        this.brain = new ArmadilloBrain(this);
        ((PathNavigateGround) getNavigator()).setCanSwim(true);
        this.scuteTime = pickNextScuteDropTime();
        // 1.20's LookControl keeps the head within the mob's own turn limit (32 degrees, none rolled up); 1.12.2's
        // has 75 fixed and is only reachable through its field.
        try {
            ObfuscationReflectionHelper.setPrivateValue(EntityLiving.class, this, new ArmadilloLook(this), "field_70749_g");
        } catch (RuntimeException e) {
            com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport.LOGGER.debug("Armadillo look helper not replaced", e);
        }
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(STATE, (byte) ArmadilloState.IDLE.ordinal());
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(12.0D);
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.14D);
    }

    @Override
    public float getEyeHeight() {
        return isChild() ? BABY_EYE_HEIGHT : EYE_HEIGHT;
    }

    /** 1.20.5's {@code getAgeScale}: a baby is 0.6 the size (1.12.2's default is half). */
    @Override
    public void setScaleForAge(boolean child) {
        setScale(child ? BABY_SCALE : 1.0F);
    }

    @Override
    protected net.minecraft.entity.EntityBodyHelper createBodyHelper() {
        return new ArmadilloBodyHelper(this);
    }

    // ------------------------------------------------------------------ state

    public ArmadilloState getState() {
        return ArmadilloState.byId(this.dataManager.get(STATE));
    }

    public void switchToState(ArmadilloState state) {
        this.dataManager.set(STATE, (byte) state.ordinal());
    }

    public boolean isScared() {
        return getState() != ArmadilloState.IDLE;
    }

    public boolean shouldHideInShell() {
        return getState().shouldHideInShell(this.inStateTicks);
    }

    public boolean shouldSwitchToScaredState() {
        return getState() == ArmadilloState.ROLLING && this.inStateTicks > ArmadilloState.ROLLING.animationDuration();
    }

    public long inStateTicks() {
        return this.inStateTicks;
    }

    /** @return ticks until this armadillo next sheds a scute (counted down by its server tick) */
    public int scuteTime() {
        return this.scuteTime;
    }

    public void setScuteTime(int ticks) {
        this.scuteTime = ticks;
    }

    /** @return true while 1.20's panic behavior runs (environmental damage) */
    public boolean isPanicking() {
        return this.brain.panicking;
    }

    /** @return ticks left of the remembered danger, 0 when none */
    public int dangerTicks() {
        return this.brain.danger;
    }

    public boolean isTempted() {
        return this.brain.tempted;
    }

    @Override
    public void notifyDataManagerChange(DataParameter<?> key) {
        if (STATE.equals(key)) {
            this.inStateTicks = 0L;
        }
        super.notifyDataManagerChange(key);
    }

    /** 1.20.5's {@code rollUp}: stops, forgets love, the roll sound, rolling. */
    public void rollUp() {
        if (!isScared()) {
            stopInPlace();
            resetInLove();
            Vibrations.entity(this, GameEvent.ENTITY_ACTION);
            playSound(ArmadilloContent.ROLL, getSoundVolume(), getSoundPitch());
            switchToState(ArmadilloState.ROLLING);
        }
    }

    /** 1.20.5's {@code rollOut}: the unroll-finish sound, idle. */
    public void rollOut() {
        if (isScared()) {
            Vibrations.entity(this, GameEvent.ENTITY_ACTION);
            playSound(ArmadilloContent.UNROLL_FINISH, getSoundVolume(), getSoundPitch());
            switchToState(ArmadilloState.IDLE);
        }
    }

    /** 1.20's {@code Mob.stopInPlace}. */
    private void stopInPlace() {
        getNavigator().clearPath();
        getMoveHelper().setMoveTo(this.posX, this.posY, this.posZ, 0.0D);
        this.moveStrafing = 0.0F;
        this.moveVertical = 0.0F;
        this.moveForward = 0.0F;
        setAIMoveSpeed(0.0F);
        this.motionX = 0.0D;
        this.motionY = 0.0D;
        this.motionZ = 0.0D;
    }

    /** 1.20.5's {@code canStayRolledUp}: not panicking, in a liquid, leashed, riding or ridden. */
    public boolean canStayRolledUp() {
        return !isPanicking() && !isInWater() && !isInLava() && !getLeashed() && !isRiding() && !isBeingRidden();
    }

    /**
     * 1.20.5's {@code isScaredBy}: within 7 blocks across and 2 up or down of its box, an undead mob, whoever
     * hurt it last, or a player (not a spectator) who is sprinting or riding.
     */
    public boolean isScaredBy(EntityLivingBase e) {
        if (!getEntityBoundingBox().grow(7.0D, 2.0D, 7.0D).intersects(e.getEntityBoundingBox())) {
            return false;
        }
        if (e.getCreatureAttribute() == EnumCreatureAttribute.UNDEAD) {
            return true;
        }
        if (getRevengeTarget() == e) {
            return true;
        }
        if (e instanceof EntityPlayer) {
            EntityPlayer p = (EntityPlayer) e;
            return !p.isSpectator() && (p.isSprinting() || p.isRiding());
        }
        return false;
    }

    // ------------------------------------------------------------------ ticking

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (this.world.isRemote) {
            setupAnimationStates();
        }
        if (isScared()) {
            clampHeadRotationToBody();
        }
        this.inStateTicks++;
    }

    /** 1.20's {@code Mob.clampHeadRotationToBody} with the armadillo's limit (none while rolled up). */
    private void clampHeadRotationToBody() {
        float max = getMaxHeadYRot();
        float head = this.rotationYawHead;
        float diff = MathHelper.wrapDegrees(this.renderYawOffset - head);
        float clamped = MathHelper.clamp(MathHelper.wrapDegrees(this.renderYawOffset - head), -max, max);
        this.rotationYawHead = head + diff - clamped;
    }

    /** 1.20.5's {@code getMaxHeadYRot}: 32 degrees, none while rolled up. */
    public int getMaxHeadYRot() {
        return isScared() ? 0 : 32;
    }

    private void setupAnimationStates() {
        switch (getState()) {
            case IDLE:
                this.rollOutAnimation.stop();
                this.rollUpAnimation.stop();
                this.peekAnimation.stop();
                break;
            case ROLLING:
                this.rollOutAnimation.stop();
                this.rollUpAnimation.startIfStopped(this.ticksExisted);
                this.peekAnimation.stop();
                break;
            case SCARED:
                this.rollOutAnimation.stop();
                this.rollUpAnimation.stop();
                if (this.peekReceivedClient) {
                    this.peekAnimation.stop();
                    this.peekReceivedClient = false;
                }
                if (this.inStateTicks == 0L) {
                    this.peekAnimation.start(this.ticksExisted);
                    this.peekAnimation.fastForward(ArmadilloState.SCARED.animationDuration(), 1.0F);
                } else {
                    this.peekAnimation.startIfStopped(this.ticksExisted);
                }
                break;
            case UNROLLING:
                this.rollOutAnimation.startIfStopped(this.ticksExisted);
                this.rollUpAnimation.stop();
                this.peekAnimation.stop();
                break;
            default:
                break;
        }
    }

    @Override
    protected void updateAITasks() {
        this.world.profiler.startSection("armadilloBrain");
        this.brain.tick();
        this.world.profiler.endSection();
        this.world.profiler.startSection("armadilloActivityUpdate");
        this.brain.updateActivity();
        this.world.profiler.endSection();
        // Current Java: the scute timer always counts down, and sheds only for an adult with mob loot on.
        if (isEntityAlive() && --this.scuteTime <= 0 && !isChild() && this.world.getGameRules().getBoolean("doMobLoot")) {
            if (dropLoot(SHED_LOOT, null)) {
                playSound(ArmadilloContent.SCUTE_DROP, 1.0F, (this.rand.nextFloat() - this.rand.nextFloat()) * 0.2F + 1.0F);
                Vibrations.entity(this, GameEvent.ENTITY_PLACE);
            }
            this.scuteTime = pickNextScuteDropTime();
        }
        super.updateAITasks();
    }

    /** 1.20.5: 5 to 10 minutes. */
    private int pickNextScuteDropTime() {
        return this.rand.nextInt(6000) + 6000;
    }

    /** @return true if the table gave anything, each stack dropped where the armadillo stands */
    private boolean dropLoot(ResourceLocation table, @Nullable EntityPlayer player) {
        if (!(this.world instanceof WorldServer)) {
            return false;
        }
        WorldServer server = (WorldServer) this.world;
        LootTable loot = server.getLootTableManager().getLootTableFromLocation(table);
        LootContext.Builder context = new LootContext.Builder(server).withLootedEntity(this);
        if (player != null) {
            context.withPlayer(player).withLuck(player.getLuck());
        }
        List<ItemStack> stacks = loot.generateLootForPools(this.rand, context.build());
        boolean any = false;
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty()) {
                entityDropItem(stack, 0.0F);
                any = true;
            }
        }
        return any;
    }

    @Override
    public void handleStatusUpdate(byte id) {
        if (id == EVENT_PEEK && this.world.isRemote) {
            this.peekReceivedClient = true;
            this.world.playSound(this.posX, this.posY, this.posZ, ArmadilloContent.PEEK, getSoundCategory(), 1.0F, 1.0F, false);
        } else {
            super.handleStatusUpdate(id);
        }
    }

    // ------------------------------------------------------------------ damage

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (isScared()) {
            amount = (amount - 1.0F) / 2.0F;
        }
        return super.attackEntityFrom(source, amount);
    }

    /** 1.20's {@code actuallyHurt}: hurt by a living thing it rolls up; by fire, lava and the like it unrolls. */
    @Override
    protected void damageEntity(DamageSource source, float amount) {
        super.damageEntity(source, amount);
        if (!isAIDisabled() && getHealth() > 0.0F && !this.world.isRemote) {
            if (source.getTrueSource() instanceof EntityLivingBase) {
                this.brain.detectDanger();
                if (canStayRolledUp()) {
                    rollUp();
                }
            } else if (ArmadilloBrain.PANIC_ENVIRONMENTAL_CAUSES.contains(source.getDamageType())) {
                rollOut();
            }
        }
    }

    // ------------------------------------------------------------------ breeding and interaction

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == Items.SPIDER_EYE;
    }

    /**
     * Current Java's {@code mobInteract}: a brush sheds a scute from an adult (16 durability); rolled up, nothing
     * else; otherwise feeding and breeding as any animal, with the armadillo's eating sound.
     */
    @Override
    public boolean processInteract(EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (ArmadilloContent.isBrush(stack) && brushOffScute(player)) {
            stack.damageItem(16, player);
            if (player instanceof EntityPlayerMP) {
                ArmadilloAdvancements.grantBrushArmadillo((EntityPlayerMP) player);
            }
            return true;
        }
        if (isScared()) {
            return false;
        }
        boolean food = isBreedingItem(stack);
        boolean result = super.processInteract(player, hand);
        if (result && food && !this.world.isRemote) {
            playSound(ArmadilloContent.EAT, getSoundVolume(), getSoundPitch());
        }
        return result;
    }

    /**
     * 1.20.5's {@code brushOffScute}: an adult drops the {@code brush/armadillo} loot (a scute) and plays the
     * brush sound. Also called by the dispenser's brush.
     *
     * @return false for a baby
     */
    public boolean brushOffScute(@Nullable EntityPlayer player) {
        if (isChild()) {
            return false;
        }
        if (!this.world.isRemote) {
            dropLoot(BRUSH_LOOT, player);
            Vibrations.entity(this, GameEvent.ENTITY_INTERACT);
            playSound(ArmadilloContent.BRUSH, 1.0F, 1.0F);
        }
        return true;
    }

    @Override
    public boolean canMateWith(EntityAnimal other) {
        return other instanceof EntityArmadillo && !isScared() && !((EntityArmadillo) other).isScared() && super.canMateWith(other);
    }

    /** 1.20's {@code Animal.spawnChildFromBreeding}, as 1.12.2's {@code EntityAIMate.spawnBaby} does it. */
    void spawnChildFromBreeding(EntityArmadillo partner) {
        EntityAgeable child = createChild(partner);
        BabyEntitySpawnEvent event = new BabyEntitySpawnEvent(this, partner, child);
        boolean cancelled = MinecraftForge.EVENT_BUS.post(event);
        child = event.getChild();
        if (cancelled) {
            setGrowingAge(6000);
            partner.setGrowingAge(6000);
            resetInLove();
            partner.resetInLove();
            return;
        }
        if (child == null) {
            return;
        }
        EntityPlayerMP cause = getLoveCause();
        if (cause == null) {
            cause = partner.getLoveCause();
        }
        if (cause != null) {
            cause.addStat(StatList.ANIMALS_BRED);
            CriteriaTriggers.BRED_ANIMALS.trigger(cause, this, partner, child);
        }
        setGrowingAge(6000);
        partner.setGrowingAge(6000);
        resetInLove();
        partner.resetInLove();
        child.setGrowingAge(-24000);
        child.setLocationAndAngles(this.posX, this.posY, this.posZ, 0.0F, 0.0F);
        this.world.spawnEntity(child);
        this.world.setEntityState(this, (byte) 18);
        if (this.world.getGameRules().getBoolean("doMobLoot")) {
            this.world.spawnEntity(new EntityXPOrb(this.world, this.posX, this.posY, this.posZ, this.rand.nextInt(7) + 1));
        }
    }

    @Override
    @Nullable
    public EntityAgeable createChild(EntityAgeable ageable) {
        return new EntityArmadillo(this.world);
    }

    // ------------------------------------------------------------------ spawning

    /**
     * 1.20.5's {@code checkArmadilloSpawnRules}: on {@code armadillo_spawnable_on} (grass, the badlands
     * terracottas, red sand, coarse dirt) with light above 8.
     */
    @Override
    public boolean getCanSpawnHere() {
        BlockPos pos = new BlockPos(MathHelper.floor(this.posX), MathHelper.floor(getEntityBoundingBox().minY), MathHelper.floor(this.posZ));
        return isSpawnableOn(this.world.getBlockState(pos.down())) && this.world.getLight(pos) > 8
                && getBlockPathWeight(pos) >= 0.0F && this.world.getBlockState(pos.down()).canEntitySpawn(this);
    }

    /** 1.20.5's {@code armadillo_spawnable_on} tag. */
    public static boolean isSpawnableOn(IBlockState state) {
        Block b = state.getBlock();
        if (b == Blocks.GRASS || b == Blocks.HARDENED_CLAY) {
            return true;
        }
        if (b == Blocks.SAND) {
            return state.getValue(net.minecraft.block.BlockSand.VARIANT) == net.minecraft.block.BlockSand.EnumType.RED_SAND;
        }
        if (b == Blocks.DIRT) {
            return state.getValue(net.minecraft.block.BlockDirt.VARIANT) == net.minecraft.block.BlockDirt.DirtType.COARSE_DIRT;
        }
        if (b == Blocks.STAINED_HARDENED_CLAY) {
            EnumDyeColor c = state.getValue(net.minecraft.block.BlockColored.COLOR);
            return c == EnumDyeColor.WHITE || c == EnumDyeColor.YELLOW || c == EnumDyeColor.ORANGE || c == EnumDyeColor.RED
                    || c == EnumDyeColor.BROWN || c == EnumDyeColor.SILVER;
        }
        return false;
    }

    // ------------------------------------------------------------------ sounds and the rest

    @Override
    @Nullable
    protected SoundEvent getAmbientSound() {
        return isScared() ? null : ArmadilloContent.AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return isScared() ? ArmadilloContent.HURT_REDUCED : ArmadilloContent.HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ArmadilloContent.DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, Block block) {
        playSound(ArmadilloContent.STEP, 0.15F, 1.0F);
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
    public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        tag.setString("state", getState().serializedName());
        tag.setInteger("scute_time", this.scuteTime);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        switchToState(ArmadilloState.fromName(tag.getString("state")));
        if (tag.hasKey("scute_time")) {
            this.scuteTime = tag.getInteger("scute_time");
        }
    }

    // ------------------------------------------------------------------ head and body

    /** 1.20's {@code Mth.rotateIfNecessary}: {@code current} turned just enough to be within {@code max} of target. */
    static float rotateIfNecessary(float current, float target, float max) {
        float diff = MathHelper.wrapDegrees(target - current);
        float clamped = MathHelper.clamp(diff, -max, max);
        return target - clamped;
    }

    /** 1.20's {@code Mth.rotateTowards}. */
    static float rotateTowards(float from, float to, float maxDelta) {
        float diff = MathHelper.wrapDegrees(to - from);
        return from + MathHelper.clamp(diff, -maxDelta, maxDelta);
    }

    /** 1.20's {@code LookControl}, with the armadillo's head limit. */
    static final class ArmadilloLook extends EntityLookHelper {
        private final EntityArmadillo mob;
        private float yMaxRotSpeed;
        private float xMaxRotAngle;
        private int lookAtCooldown;
        private double wantedX;
        private double wantedY;
        private double wantedZ;

        ArmadilloLook(EntityArmadillo mob) {
            super(mob);
            this.mob = mob;
        }

        @Override
        public void setLookPositionWithEntity(Entity e, float yaw, float pitch) {
            setLookPosition(e.posX, e instanceof EntityLivingBase ? e.posY + e.getEyeHeight()
                    : (e.getEntityBoundingBox().minY + e.getEntityBoundingBox().maxY) / 2.0D, e.posZ, yaw, pitch);
        }

        @Override
        public void setLookPosition(double x, double y, double z, float yaw, float pitch) {
            this.wantedX = x;
            this.wantedY = y;
            this.wantedZ = z;
            this.yMaxRotSpeed = yaw;
            this.xMaxRotAngle = pitch;
            this.lookAtCooldown = 2;
        }

        @Override
        public void onUpdateLook() {
            EntityArmadillo m = this.mob;
            m.rotationPitch = 0.0F;
            if (this.lookAtCooldown > 0) {
                this.lookAtCooldown--;
                double xd = this.wantedX - m.posX;
                double yd = this.wantedY - (m.posY + m.getEyeHeight());
                double zd = this.wantedZ - m.posZ;
                if (Math.abs(zd) > 1.0E-5F || Math.abs(xd) > 1.0E-5F) {
                    float yRotD = (float) (MathHelper.atan2(zd, xd) * (180.0D / Math.PI)) - 90.0F;
                    m.rotationYawHead = rotateTowards(m.rotationYawHead, yRotD, this.yMaxRotSpeed);
                }
                double sd = Math.sqrt(xd * xd + zd * zd);
                if (Math.abs(yd) > 1.0E-5F || Math.abs(sd) > 1.0E-5F) {
                    float xRotD = (float) -(MathHelper.atan2(yd, sd) * (180.0D / Math.PI));
                    m.rotationPitch = rotateTowards(m.rotationPitch, xRotD, this.xMaxRotAngle);
                }
            } else {
                m.rotationYawHead = rotateTowards(m.rotationYawHead, m.renderYawOffset, 10.0F);
            }
            if (!m.getNavigator().noPath()) {
                m.rotationYawHead = rotateIfNecessary(m.rotationYawHead, m.renderYawOffset, m.getMaxHeadYRot());
            }
        }

        @Override
        public boolean getIsLooking() {
            return this.lookAtCooldown > 0;
        }

        @Override
        public double getLookPosX() {
            return this.wantedX;
        }

        @Override
        public double getLookPosY() {
            return this.wantedY;
        }

        @Override
        public double getLookPosZ() {
            return this.wantedZ;
        }
    }

    /** 1.20's {@code BodyRotationControl} with the armadillo's head limit, idle while it is rolled up. */
    static final class ArmadilloBodyHelper extends net.minecraft.entity.EntityBodyHelper {
        private final EntityArmadillo mob;
        private int headStableTime;
        private float lastStableYHeadRot;

        ArmadilloBodyHelper(EntityArmadillo mob) {
            super(mob);
            this.mob = mob;
        }

        @Override
        public void updateRenderAngles() {
            EntityArmadillo m = this.mob;
            if (m.isScared()) {
                return;
            }
            double xd = m.posX - m.prevPosX;
            double zd = m.posZ - m.prevPosZ;
            float max = m.getMaxHeadYRot();
            if (xd * xd + zd * zd > 2.5000003E-7F) {
                m.renderYawOffset = m.rotationYaw;
                m.rotationYawHead = rotateIfNecessary(m.rotationYawHead, m.renderYawOffset, max);
                this.lastStableYHeadRot = m.rotationYawHead;
                this.headStableTime = 0;
            } else if (m.getPassengers().isEmpty() || !(m.getPassengers().get(0) instanceof EntityLiving)) {
                if (Math.abs(m.rotationYawHead - this.lastStableYHeadRot) > 15.0F) {
                    this.headStableTime = 0;
                    this.lastStableYHeadRot = m.rotationYawHead;
                    m.renderYawOffset = rotateIfNecessary(m.renderYawOffset, m.rotationYawHead, max);
                } else {
                    this.headStableTime++;
                    if (this.headStableTime > 10) {
                        float fraction = MathHelper.clamp((this.headStableTime - 10) / 10.0F, 0.0F, 1.0F);
                        m.renderYawOffset = rotateIfNecessary(m.renderYawOffset, m.rotationYawHead, max * (1.0F - fraction));
                    }
                }
            }
        }
    }
}
