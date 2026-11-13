package com.exiledradio.trailsandtalesbackport.armadillo;

import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.pathfinding.Path;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * The armadillo's brain: 1.20.5's {@code ArmadilloAi} and the vanilla behaviors and sensors it is made of,
 * on 1.12.2 (current Java where they differ).
 *
 * <h2>How 1.20 runs it, and how this does</h2>
 * Each tick, as 1.20's {@code Brain.tick}: memories with an expiry count down (the danger memory), the
 * sensors whose timers ran out scan (nearest living entities, hurt-by, temptation and nearest adult every 20
 * ticks, the scare detector every 5), every stopped behavior of the core activity and of the active one
 * (panic while danger is remembered and the armadillo is not panicking, else idle) tries to start, in 1.20's
 * priority order, and every running behavior - of any activity - ticks until it times out or its own
 * condition fails. Behaviors run side by side; there is no mutex. The armadillo's own memories are fields.
 *
 * <p>Priorities, as 1.20 numbers them: core 0-6 (swim, panic, look at target, move to target, the two
 * cooldowns, rolling out), idle 0-4 (look at a player sometimes, make love, temptation or following an
 * adult, look around, stroll), panic 0 (ball up). Within one priority 1.20's order depends on a hash map;
 * here it is core, then idle, then panic.
 *
 * <p>It is ticked from the entity's {@code updateAITasks}, after 1.12.2's navigator and ahead of its move
 * and look helpers, where 1.20 runs it from {@code customServerAiStep}. 1.20 saves no armadillo memories.
 */
final class ArmadilloBrain {

    static final float SPEED_WHEN_PANICKING = 2.0F;
    static final float SPEED_WHEN_IDLING = 1.0F;
    static final float SPEED_WHEN_TEMPTED = 1.25F;
    static final float SPEED_WHEN_FOLLOWING_ADULT = 1.25F;
    static final float SPEED_WHEN_MAKING_LOVE = 1.0F;
    static final int TEMPTATION_COOLDOWN = 100;
    /** 1.20.5's scare sensor: how long a scare is remembered, and how often it scans. */
    static final int DANGER_MEMORY_TICKS = 80;
    static final int SCARE_SCAN_TICKS = 5;
    /** 1.20.5's {@code ArmadilloBallUp}: the longest it stays balled up, and the "danger still around" mark. */
    static final int BALL_UP_STAY_IN_STATE = 6000;
    static final int DANGER_THRESHOLD = 75;
    /** 26.3's {@code panic_environmental_causes} damage types, as 1.12.2 names them (and freezing). */
    static final Set<String> PANIC_ENVIRONMENTAL_CAUSES = new HashSet<String>(Arrays.asList(
            "cactus", "freeze", "hotFloor", "inFire", "lava", "lightningBolt", "onFire"));

    private final EntityArmadillo armadillo;
    private final Random rand;

    // ---------------------------------------------------------------- memories
    @Nullable WalkTarget walkTarget;
    @Nullable Look look;
    boolean panicking;
    @Nullable DamageSource hurtBy;
    /** The nearest living entities within the follow range, closest first; null until the first scan. */
    @Nullable List<EntityLivingBase> nearest;
    /** Those of them the armadillo can see; null until the first scan. */
    @Nullable List<EntityLivingBase> visible;
    @Nullable EntityPlayer temptingPlayer;
    boolean tempted;
    /** Ticks left, or -1 when absent. */
    int temptationCooldown = -1;
    int gazeCooldown = -1;
    @Nullable EntityArmadillo breedTarget;
    @Nullable EntityArmadillo nearestVisibleAdult;
    /** Ticks until the danger memory expires; 0 when absent. */
    int danger;

    // Sensors start at a random point of their scan, as 1.20's do.
    private int livingTimer;
    private int hurtByTimer;
    private int temptTimer;
    private int adultTimer;
    private int scareTimer;
    private Activity active = Activity.IDLE;

    // ---------------------------------------------------------------- behaviors
    private final List<Entry> behaviors = new ArrayList<Entry>();
    final BallUp ballUp = new BallUp();

    enum Activity { CORE, IDLE, PANIC }

    private static final class Entry {
        final int priority;
        final Activity activity;
        final Behavior behavior;

        Entry(int priority, Activity activity, Behavior behavior) {
            this.priority = priority;
            this.activity = activity;
            this.behavior = behavior;
        }
    }

    ArmadilloBrain(EntityArmadillo armadillo) {
        this.armadillo = armadillo;
        this.rand = armadillo.getRNG();
        this.livingTimer = this.rand.nextInt(20);
        this.hurtByTimer = this.rand.nextInt(20);
        this.temptTimer = this.rand.nextInt(20);
        this.adultTimer = this.rand.nextInt(20);
        this.scareTimer = this.rand.nextInt(SCARE_SCAN_TICKS);
        add(0, Activity.CORE, new Swim());
        add(1, Activity.CORE, new Panic());
        add(2, Activity.CORE, new LookAtTarget());
        add(3, Activity.CORE, new MoveToTarget());
        add(4, Activity.CORE, new CountDownTemptation());
        add(5, Activity.CORE, new CountDownGaze());
        add(6, Activity.CORE, new RollingOut());
        add(0, Activity.IDLE, new LookAtPlayerSometimes());
        add(1, Activity.IDLE, new MakeLove());
        RunOne follow = new RunOne(false);
        follow.add(new FollowTemptation(), 1);
        follow.add(new FollowAdult(), 1);
        add(2, Activity.IDLE, follow);
        add(3, Activity.IDLE, new RandomLookAround());
        RunOne stroll = new RunOne(true);
        stroll.add(new RandomStroll(), 1);
        stroll.add(new WalkToLookTarget(), 1);
        stroll.add(new DoNothing(), 1);
        add(4, Activity.IDLE, stroll);
        add(0, Activity.PANIC, this.ballUp);
        // A stable sort: within one priority, core before idle before panic.
        Collections.sort(this.behaviors, Comparator.comparingInt((Entry e) -> e.priority).thenComparing(e -> e.activity));
    }

    private void add(int priority, Activity activity, Behavior behavior) {
        this.behaviors.add(new Entry(priority, activity, behavior));
    }

    // ---------------------------------------------------------------- the tick

    void tick() {
        long now = this.armadillo.world.getTotalWorldTime();
        // 1.20's forgetOutdatedMemories: the danger memory counts down and goes at 0.
        if (this.danger > 0) {
            this.danger--;
        }
        sense();
        for (Entry e : this.behaviors) {
            if (!e.behavior.running && (e.activity == Activity.CORE || e.activity == this.active)) {
                e.behavior.tryStart(now);
            }
        }
        for (Entry e : this.behaviors) {
            if (e.behavior.running) {
                e.behavior.tickOrStop(now);
            }
        }
    }

    /** 1.20.5's {@code ArmadilloAi.updateActivity}: panic if its conditions hold, else idle. */
    void updateActivity() {
        this.active = this.danger > 0 && !this.panicking ? Activity.PANIC : Activity.IDLE;
    }

    Activity activity() {
        return this.active;
    }

    /** 1.20's {@code setMemoryWithExpiry(DANGER_DETECTED_RECENTLY, true, 80)}. */
    void detectDanger() {
        this.danger = DANGER_MEMORY_TICKS;
    }

    boolean isVisible(Entity e) {
        return this.visible != null && this.visible.contains(e);
    }

    void lookAt(Entity e, boolean eyes) {
        this.look = new Look(e, eyes, null);
    }

    // ---------------------------------------------------------------- sensors

    private void sense() {
        EntityArmadillo a = this.armadillo;
        if (--this.livingTimer <= 0) {
            this.livingTimer = 20;
            senseLiving();
        }
        if (--this.hurtByTimer <= 0) {
            this.hurtByTimer = 20;
            // 1.20's HurtBySensor: the last damage within 40 ticks (1.12.2 forgets it the same way).
            this.hurtBy = a.getLastDamageSource();
        }
        if (--this.temptTimer <= 0) {
            this.temptTimer = 20;
            senseTemptation();
        }
        if (--this.adultTimer <= 0) {
            this.adultTimer = 20;
            // 1.20's AdultSensor: the closest visible adult armadillo.
            this.nearestVisibleAdult = null;
            if (this.visible != null) {
                for (EntityLivingBase e : this.visible) {
                    if (e instanceof EntityArmadillo && !e.isChild()) {
                        this.nearestVisibleAdult = (EntityArmadillo) e;
                        break;
                    }
                }
            }
        }
        if (--this.scareTimer <= 0) {
            this.scareTimer = SCARE_SCAN_TICKS;
            // 1.20.5's scare sensor (a MobSensor): when the armadillo cannot stay rolled up the danger is
            // forgotten; otherwise any nearby entity that scares it refreshes it.
            if (!a.canStayRolledUp()) {
                this.danger = 0;
            } else if (this.nearest != null) {
                for (EntityLivingBase e : this.nearest) {
                    if (a.isScaredBy(e)) {
                        detectDanger();
                        break;
                    }
                }
            }
        }
    }

    /** 1.20's NearestLivingEntitySensor: living entities within the follow range (16), and the visible ones. */
    private void senseLiving() {
        EntityArmadillo a = this.armadillo;
        double range = a.getEntityAttribute(net.minecraft.entity.SharedMonsterAttributes.FOLLOW_RANGE).getAttributeValue();
        List<EntityLivingBase> near = a.world.getEntitiesWithinAABB(EntityLivingBase.class,
                a.getEntityBoundingBox().grow(range, range, range), e -> e != a && e != null && e.isEntityAlive());
        near.sort(Comparator.comparingDouble(a::getDistanceSq));
        List<EntityLivingBase> seen = new ArrayList<EntityLivingBase>();
        for (EntityLivingBase e : near) {
            if (targetable(e, 16.0D) && a.getEntitySenses().canSee(e)) {
                seen.add(e);
            }
        }
        this.nearest = near;
        this.visible = seen;
    }

    /**
     * 1.20's non-combat {@code TargetingConditions} with a range: not a spectator, and within the range scaled
     * by how visible the entity is (sneaking 0.8; invisible 0.7 times its armor cover, at least 0.1).
     */
    private boolean targetable(EntityLivingBase e, double range) {
        if (e instanceof EntityPlayer && ((EntityPlayer) e).isSpectator()) {
            return false;
        }
        double r = range;
        if (e.isSneaking()) {
            r *= 0.8D;
        }
        if (e.isInvisible()) {
            int worn = 0;
            for (net.minecraft.item.ItemStack armor : e.getArmorInventoryList()) {
                worn += armor.isEmpty() ? 0 : 1;
            }
            r *= 0.7D * Math.max(worn / 4.0F, 0.1F);
        }
        return this.armadillo.getDistanceSq(e) <= r * r;
    }

    /** 1.20's TemptingSensor: the closest player within 10 (current Java's tempt range) holding spider eyes. */
    private void senseTemptation() {
        EntityArmadillo a = this.armadillo;
        EntityPlayer best = null;
        double bestDist = Double.MAX_VALUE;
        for (EntityPlayer p : a.world.playerEntities) {
            if (p.isSpectator() || !p.isEntityAlive() || !targetable(p, 10.0D) || a.isPassenger(p)) {
                continue;
            }
            double d = a.getDistanceSq(p);
            if ((a.isBreedingItem(p.getHeldItemMainhand()) || a.isBreedingItem(p.getHeldItemOffhand())) && d < bestDist) {
                best = p;
                bestDist = d;
            }
        }
        this.temptingPlayer = best;
    }

    // ---------------------------------------------------------------- positions

    /** 1.20's {@code LandRandomPos.getPos}: a random standing spot on land, as 1.12.2 finds one. */
    @Nullable
    Vec3d landPos(int xz, int y) {
        return RandomPositionGenerator.getLandPos(this.armadillo, xz, y);
    }

    /** 1.20's {@code AnimalPanic.lookForWater}: the nearest water within 5 across and 1 up or down. */
    @Nullable
    private BlockPos lookForWater() {
        EntityArmadillo a = this.armadillo;
        BlockPos at = new BlockPos(a);
        if (a.world.getBlockState(at).getCollisionBoundingBox(a.world, at) != null) {
            return null;
        }
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.getAllInBox(at.add(-5, -1, -5), at.add(5, 1, 5))) {
            double d = p.distanceSq(at);
            if (a.world.getBlockState(p).getMaterial() == Material.WATER && d < bestDist) {
                best = p.toImmutable();
                bestDist = d;
            }
        }
        return best;
    }

    // ---------------------------------------------------------------- trackers

    /** 1.20's look target: an {@code EntityTracker} (at the eyes or the feet) or a {@code BlockPosTracker}. */
    static final class Look {
        @Nullable final Entity entity;
        final boolean eyes;
        @Nullable final Vec3d pos;

        Look(@Nullable Entity entity, boolean eyes, @Nullable Vec3d pos) {
            this.entity = entity;
            this.eyes = eyes;
            this.pos = pos;
        }

        Vec3d position() {
            if (this.entity != null) {
                return new Vec3d(this.entity.posX, this.entity.posY + (this.eyes ? this.entity.getEyeHeight() : 0.0D),
                        this.entity.posZ);
            }
            return this.pos;
        }

        BlockPos blockPos() {
            return this.entity != null ? new BlockPos(this.entity) : new BlockPos(this.pos);
        }
    }

    /** 1.20's {@code WalkTarget}: a block or an entity, a speed multiplier and how close is close enough. */
    static final class WalkTarget {
        @Nullable final BlockPos pos;
        @Nullable final Entity entity;
        final float speed;
        final int closeEnough;

        WalkTarget(BlockPos pos, float speed, int closeEnough) {
            this.pos = pos.toImmutable();
            this.entity = null;
            this.speed = speed;
            this.closeEnough = closeEnough;
        }

        WalkTarget(Entity entity, float speed, int closeEnough) {
            this.pos = null;
            this.entity = entity;
            this.speed = speed;
            this.closeEnough = closeEnough;
        }

        BlockPos blockPos() {
            return this.pos != null ? this.pos : new BlockPos(this.entity);
        }
    }

    // ---------------------------------------------------------------- behavior machinery

    /** 1.20's {@code Behavior}: a random run time between min and max, entry conditions, start/tick/stop. */
    abstract class Behavior {
        final int min;
        final int max;
        boolean running;
        long end;

        Behavior(int min, int max) {
            this.min = min;
            this.max = max;
        }

        /** The memory conditions of 1.20's entry map. */
        boolean memoriesOk() {
            return true;
        }

        boolean extra() {
            return true;
        }

        boolean tryStart(long now) {
            if (memoriesOk() && extra()) {
                this.running = true;
                this.end = now + this.min + ArmadilloBrain.this.rand.nextInt(this.max + 1 - this.min);
                start(now);
                return true;
            }
            return false;
        }

        void tickOrStop(long now) {
            if (!timedOut(now) && canStillUse(now)) {
                tick(now);
            } else {
                doStop(now);
            }
        }

        void doStop(long now) {
            this.running = false;
            stop(now);
        }

        boolean timedOut(long now) {
            return now > this.end;
        }

        boolean canStillUse(long now) {
            return false;
        }

        void start(long now) {
        }

        void tick(long now) {
        }

        void stop(long now) {
        }
    }

    /** 1.20's one-shot behaviors: run for the tick they start in. */
    abstract class OneShot extends Behavior {
        OneShot() {
            super(1, 1);
        }

        abstract boolean trigger();

        @Override
        boolean tryStart(long now) {
            if (trigger()) {
                this.running = true;
                return true;
            }
            return false;
        }

        @Override
        void tickOrStop(long now) {
            this.running = false;
        }
    }

    /** 1.20's {@code RunOne}: a shuffled weighted list; the first child that starts is the one that runs. */
    final class RunOne extends Behavior {
        private final boolean needsNoWalkTarget;
        private final List<Behavior> children = new ArrayList<Behavior>();
        private final List<Integer> weights = new ArrayList<Integer>();

        /** @param needsNoWalkTarget 1.20's {@code WALK_TARGET: VALUE_ABSENT} entry condition */
        RunOne(boolean needsNoWalkTarget) {
            super(1, 1);
            this.needsNoWalkTarget = needsNoWalkTarget;
        }

        void add(Behavior b, int weight) {
            this.children.add(b);
            this.weights.add(weight);
        }

        @Override
        boolean memoriesOk() {
            return !this.needsNoWalkTarget || ArmadilloBrain.this.walkTarget == null;
        }

        @Override
        boolean tryStart(long now) {
            if (!memoriesOk()) {
                return false;
            }
            this.running = true;
            // 1.20's ShufflingList: each entry draws -rand^(1/weight) and the list is sorted by it.
            final double[] keys = new double[this.children.size()];
            List<Integer> order = new ArrayList<Integer>();
            for (int i = 0; i < keys.length; i++) {
                keys[i] = -Math.pow(ArmadilloBrain.this.rand.nextFloat(), 1.0F / this.weights.get(i));
                order.add(i);
            }
            order.sort((a, b) -> Double.compare(keys[a], keys[b]));
            for (int i : order) {
                Behavior b = this.children.get(i);
                if (!b.running && b.tryStart(now)) {
                    break;
                }
            }
            return true;
        }

        @Override
        void tickOrStop(long now) {
            boolean any = false;
            for (Behavior b : this.children) {
                if (b.running) {
                    b.tickOrStop(now);
                    any |= b.running;
                }
            }
            if (!any) {
                doStop(now);
            }
        }

        @Override
        void stop(long now) {
            for (Behavior b : this.children) {
                if (b.running) {
                    b.doStop(now);
                }
            }
        }
    }

    // ---------------------------------------------------------------- core

    /** 1.20's {@code Swim(0.8)}: jump in water or lava, 80% of ticks (the armadillo's eyes are below 0.4). */
    final class Swim extends Behavior {
        Swim() {
            super(60, 60);
        }

        @Override
        boolean extra() {
            return ArmadilloBrain.this.armadillo.isInWater() || ArmadilloBrain.this.armadillo.isInLava();
        }

        @Override
        boolean canStillUse(long now) {
            return extra();
        }

        @Override
        void tick(long now) {
            if (ArmadilloBrain.this.rand.nextFloat() < 0.8F) {
                ArmadilloBrain.this.armadillo.getJumpHelper().setJumping();
            }
        }
    }

    /**
     * 1.20.5's {@code ArmadilloPanic(2.0)}: current Java's {@code AnimalPanic} for environmental damage (fire,
     * lava, cactus, magma, lightning, freezing) remembered by the hurt-by sensor; it unrolls first.
     */
    final class Panic extends Behavior {
        Panic() {
            super(100, 120);
        }

        @Override
        boolean extra() {
            DamageSource s = ArmadilloBrain.this.hurtBy;
            return s != null && PANIC_ENVIRONMENTAL_CAUSES.contains(s.getDamageType()) || ArmadilloBrain.this.panicking;
        }

        @Override
        boolean canStillUse(long now) {
            return true;
        }

        @Override
        void start(long now) {
            ArmadilloBrain.this.armadillo.rollOut();
            ArmadilloBrain.this.panicking = true;
            ArmadilloBrain.this.walkTarget = null;
            ArmadilloBrain.this.armadillo.getNavigator().clearPath();
        }

        @Override
        void stop(long now) {
            ArmadilloBrain.this.panicking = false;
        }

        @Override
        void tick(long now) {
            if (ArmadilloBrain.this.armadillo.getNavigator().noPath()) {
                Vec3d to = null;
                if (ArmadilloBrain.this.armadillo.isBurning()) {
                    BlockPos water = lookForWater();
                    if (water != null) {
                        to = new Vec3d(water.getX() + 0.5D, water.getY(), water.getZ() + 0.5D);
                    }
                }
                if (to == null) {
                    to = landPos(5, 4);
                }
                if (to != null) {
                    ArmadilloBrain.this.walkTarget = new WalkTarget(new BlockPos(to), SPEED_WHEN_PANICKING, 0);
                }
            }
        }
    }

    /** 1.20's {@code LookAtTargetSink(45, 90)}. */
    final class LookAtTarget extends Behavior {
        LookAtTarget() {
            super(45, 90);
        }

        @Override
        boolean memoriesOk() {
            return ArmadilloBrain.this.look != null;
        }

        @Override
        boolean canStillUse(long now) {
            Look l = ArmadilloBrain.this.look;
            if (l == null) {
                return false;
            }
            // EntityTracker.isVisibleBy: a living target must be alive and among the visible entities.
            return !(l.entity instanceof EntityLivingBase) || l.entity.isEntityAlive() && isVisible(l.entity);
        }

        @Override
        void tick(long now) {
            Look l = ArmadilloBrain.this.look;
            if (l != null) {
                EntityArmadillo a = ArmadilloBrain.this.armadillo;
                Vec3d p = l.position();
                a.getLookHelper().setLookPosition(p.x, p.y, p.z, a.getHorizontalFaceSpeed(), a.getVerticalFaceSpeed());
            }
        }

        @Override
        void stop(long now) {
            ArmadilloBrain.this.look = null;
        }
    }

    /**
     * 1.20's {@code MoveToTargetSink}, which does not start while the armadillo is rolled up: follows the walk
     * target's path for 150-250 ticks, repaths when the target moves more than two blocks, and on a failed
     * path walks towards it; when stuck it waits up to 40 ticks before trying again.
     */
    final class MoveToTarget extends Behavior {
        @Nullable private Path path;
        @Nullable private BlockPos lastTargetPos;
        private float speed;
        private int remainingCooldown;

        MoveToTarget() {
            super(150, 250);
        }

        @Override
        boolean memoriesOk() {
            return ArmadilloBrain.this.walkTarget != null;
        }

        @Override
        boolean extra() {
            if (ArmadilloBrain.this.armadillo.isScared()) {
                return false;
            }
            if (this.remainingCooldown > 0) {
                this.remainingCooldown--;
                return false;
            }
            WalkTarget target = ArmadilloBrain.this.walkTarget;
            if (!reached(target) && computePath(target)) {
                this.lastTargetPos = target.blockPos();
                return true;
            }
            ArmadilloBrain.this.walkTarget = null;
            return false;
        }

        @Override
        boolean canStillUse(long now) {
            WalkTarget target = ArmadilloBrain.this.walkTarget;
            boolean spectator = target != null && target.entity instanceof EntityPlayer && ((EntityPlayer) target.entity).isSpectator();
            return this.path != null && this.lastTargetPos != null && target != null
                    && !ArmadilloBrain.this.armadillo.getNavigator().noPath() && !reached(target) && !spectator;
        }

        @Override
        void start(long now) {
            ArmadilloBrain.this.armadillo.getNavigator().setPath(this.path, this.speed);
        }

        @Override
        void tick(long now) {
            Path current = ArmadilloBrain.this.armadillo.getNavigator().getPath();
            this.path = current;
            WalkTarget target = ArmadilloBrain.this.walkTarget;
            if (current != null && this.lastTargetPos != null && target != null
                    && target.blockPos().distanceSq(this.lastTargetPos) > 4.0D && computePath(target)) {
                this.lastTargetPos = target.blockPos();
                start(now);
            }
        }

        @Override
        void stop(long now) {
            EntityArmadillo a = ArmadilloBrain.this.armadillo;
            WalkTarget target = ArmadilloBrain.this.walkTarget;
            if (target != null && !reached(target) && stuck()) {
                this.remainingCooldown = ArmadilloBrain.this.rand.nextInt(40);
            }
            a.getNavigator().clearPath();
            ArmadilloBrain.this.walkTarget = null;
            this.path = null;
        }

        /** 1.12.2's navigator gives up on a path it cannot follow by clearing it; 1.20 calls that stuck. */
        private boolean stuck() {
            return this.path != null && !this.path.isFinished() && ArmadilloBrain.this.armadillo.getNavigator().noPath();
        }

        /**
         * 1.20: reached when the target block is within its close-enough distance (Manhattan) of the mob's block.
         * Measured from the node 1.12.2's navigator walked the mob to ({@code node + (int) (width + 1) / 2}).
         */
        boolean reached(WalkTarget target) {
            BlockPos t = target.blockPos();
            EntityArmadillo a = ArmadilloBrain.this.armadillo;
            double half = (int) (a.width + 1.0F) * 0.5D;
            BlockPos at = new BlockPos(MathHelper.floor(a.posX - half + 0.5D), MathHelper.floor(a.posY),
                    MathHelper.floor(a.posZ - half + 0.5D));
            int d = Math.abs(t.getX() - at.getX()) + Math.abs(t.getY() - at.getY()) + Math.abs(t.getZ() - at.getZ());
            return d <= target.closeEnough;
        }

        private boolean computePath(WalkTarget target) {
            EntityArmadillo a = ArmadilloBrain.this.armadillo;
            this.speed = target.speed;
            BlockPos t = target.blockPos();
            this.path = a.getNavigator().getPathToPos(t);
            if (reached(target)) {
                return false;
            }
            if (this.path != null) {
                return true;
            }
            Vec3d towards = RandomPositionGenerator.findRandomTargetBlockTowards(a, 10, 7,
                    new Vec3d(t.getX() + 0.5D, t.getY(), t.getZ() + 0.5D));
            if (towards != null) {
                this.path = a.getNavigator().getPathToXYZ(towards.x, towards.y, towards.z);
                return this.path != null;
            }
            return false;
        }
    }

    /** 1.20's {@code CountDownCooldownTicks(TEMPTATION_COOLDOWN_TICKS)}. */
    final class CountDownTemptation extends Behavior {
        CountDownTemptation() {
            super(60, 60);
        }

        @Override
        boolean memoriesOk() {
            return ArmadilloBrain.this.temptationCooldown >= 0;
        }

        @Override
        boolean canStillUse(long now) {
            return ArmadilloBrain.this.temptationCooldown > 0;
        }

        @Override
        boolean timedOut(long now) {
            return false;
        }

        @Override
        void tick(long now) {
            ArmadilloBrain.this.temptationCooldown--;
        }

        @Override
        void stop(long now) {
            ArmadilloBrain.this.temptationCooldown = -1;
        }
    }

    /** 1.20's {@code CountDownCooldownTicks(GAZE_COOLDOWN_TICKS)}. */
    final class CountDownGaze extends Behavior {
        CountDownGaze() {
            super(60, 60);
        }

        @Override
        boolean memoriesOk() {
            return ArmadilloBrain.this.gazeCooldown >= 0;
        }

        @Override
        boolean canStillUse(long now) {
            return ArmadilloBrain.this.gazeCooldown > 0;
        }

        @Override
        boolean timedOut(long now) {
            return false;
        }

        @Override
        void tick(long now) {
            ArmadilloBrain.this.gazeCooldown--;
        }

        @Override
        void stop(long now) {
            ArmadilloBrain.this.gazeCooldown = -1;
        }
    }

    /** 1.20.5's {@code ARMADILLO_ROLLING_OUT}: with no danger remembered, a rolled-up armadillo unrolls. */
    final class RollingOut extends OneShot {
        @Override
        boolean trigger() {
            if (ArmadilloBrain.this.danger <= 0 && ArmadilloBrain.this.armadillo.isScared()) {
                ArmadilloBrain.this.armadillo.rollOut();
                return true;
            }
            return false;
        }
    }

    // ---------------------------------------------------------------- idle

    /** 1.20's {@code SetEntityLookTargetSometimes.create(PLAYER, 6, 30-60)}. */
    final class LookAtPlayerSometimes extends OneShot {
        private int ticksUntilNextStart;

        @Override
        boolean trigger() {
            if (ArmadilloBrain.this.look != null || ArmadilloBrain.this.visible == null) {
                return false;
            }
            EntityLivingBase target = null;
            for (EntityLivingBase e : ArmadilloBrain.this.visible) {
                if (e instanceof EntityPlayer && e.getDistanceSq(ArmadilloBrain.this.armadillo) <= 36.0D) {
                    target = e;
                    break;
                }
            }
            if (target == null || !tickDownAndCheck()) {
                return false;
            }
            lookAt(target, true);
            return true;
        }

        /** 1.20's {@code Ticker.tickDownAndCheck}: true once every 30-60 calls. */
        private boolean tickDownAndCheck() {
            if (this.ticksUntilNextStart == 0) {
                this.ticksUntilNextStart = 30 + ArmadilloBrain.this.rand.nextInt(31) - 1;
                return false;
            }
            return --this.ticksUntilNextStart == 0;
        }
    }

    /** 1.20's {@code AnimalMakeLove(ARMADILLO, 1.0, 1)}. */
    final class MakeLove extends Behavior {
        private long spawnChildAt;

        MakeLove() {
            super(110, 110);
        }

        @Override
        boolean memoriesOk() {
            return ArmadilloBrain.this.visible != null && ArmadilloBrain.this.breedTarget == null
                    && !ArmadilloBrain.this.panicking;
        }

        @Override
        boolean extra() {
            return ArmadilloBrain.this.armadillo.isInLove() && partner() != null;
        }

        @Nullable
        private EntityArmadillo partner() {
            for (EntityLivingBase e : ArmadilloBrain.this.visible) {
                if (e instanceof EntityArmadillo && ArmadilloBrain.this.armadillo.canMateWith((EntityArmadillo) e)
                        && !((EntityArmadillo) e).isPanicking()) {
                    return (EntityArmadillo) e;
                }
            }
            return null;
        }

        @Override
        void start(long now) {
            EntityArmadillo other = partner();
            ArmadilloBrain.this.breedTarget = other;
            other.brain.breedTarget = ArmadilloBrain.this.armadillo;
            lockGazeAndWalk(other);
            this.spawnChildAt = now + 60 + ArmadilloBrain.this.rand.nextInt(50);
        }

        /** 1.20's {@code BehaviorUtils.lockGazeAndWalkToEachOther}. */
        private void lockGazeAndWalk(EntityArmadillo other) {
            EntityArmadillo self = ArmadilloBrain.this.armadillo;
            lookAt(other, true);
            other.brain.lookAt(self, true);
            ArmadilloBrain.this.walkTarget = new WalkTarget(other, SPEED_WHEN_MAKING_LOVE, 1);
            other.brain.walkTarget = new WalkTarget(self, SPEED_WHEN_MAKING_LOVE, 1);
        }

        @Override
        boolean canStillUse(long now) {
            EntityArmadillo other = ArmadilloBrain.this.breedTarget;
            return other != null && other.isEntityAlive() && ArmadilloBrain.this.armadillo.canMateWith(other)
                    && isVisible(other) && now <= this.spawnChildAt && !ArmadilloBrain.this.panicking && !other.isPanicking();
        }

        @Override
        void tick(long now) {
            EntityArmadillo other = ArmadilloBrain.this.breedTarget;
            lockGazeAndWalk(other);
            if (ArmadilloBrain.this.armadillo.getDistanceSq(other) < 9.0D && now >= this.spawnChildAt) {
                ArmadilloBrain.this.armadillo.spawnChildFromBreeding(other);
                ArmadilloBrain.this.breedTarget = null;
                other.brain.breedTarget = null;
            }
        }

        @Override
        void stop(long now) {
            ArmadilloBrain.this.breedTarget = null;
            ArmadilloBrain.this.walkTarget = null;
            ArmadilloBrain.this.look = null;
            this.spawnChildAt = 0L;
        }
    }

    /** 1.20's {@code FollowTemptation}, 1.25 fast, close enough at 1 block (baby) or 2. */
    final class FollowTemptation extends Behavior {
        FollowTemptation() {
            super(60, 60);
        }

        @Override
        boolean memoriesOk() {
            return ArmadilloBrain.this.temptingPlayer != null && ArmadilloBrain.this.temptationCooldown < 0
                    && ArmadilloBrain.this.breedTarget == null && !ArmadilloBrain.this.panicking;
        }

        @Override
        boolean timedOut(long now) {
            return false;
        }

        @Override
        boolean canStillUse(long now) {
            return ArmadilloBrain.this.temptingPlayer != null && ArmadilloBrain.this.breedTarget == null
                    && !ArmadilloBrain.this.panicking;
        }

        @Override
        void start(long now) {
            ArmadilloBrain.this.tempted = true;
        }

        @Override
        void stop(long now) {
            ArmadilloBrain.this.temptationCooldown = TEMPTATION_COOLDOWN;
            ArmadilloBrain.this.tempted = false;
            ArmadilloBrain.this.walkTarget = null;
            ArmadilloBrain.this.look = null;
        }

        @Override
        void tick(long now) {
            EntityPlayer player = ArmadilloBrain.this.temptingPlayer;
            lookAt(player, true);
            double close = ArmadilloBrain.this.armadillo.isChild() ? 1.0D : 2.0D;
            if (ArmadilloBrain.this.armadillo.getDistanceSq(player) < close * close) {
                ArmadilloBrain.this.walkTarget = null;
            } else {
                ArmadilloBrain.this.walkTarget = new WalkTarget(player, SPEED_WHEN_TEMPTED, 2);
            }
        }
    }

    /** 1.20's {@code BabyFollowAdult.create(5-16, 1.25)}: a baby walks to an adult 5 to 17 blocks away. */
    final class FollowAdult extends OneShot {
        @Override
        boolean trigger() {
            EntityArmadillo a = ArmadilloBrain.this.armadillo;
            EntityArmadillo adult = ArmadilloBrain.this.nearestVisibleAdult;
            if (adult == null || ArmadilloBrain.this.walkTarget != null || !a.isChild()) {
                return false;
            }
            double d = a.getDistanceSq(adult);
            if (d < 17.0D * 17.0D && d >= 5.0D * 5.0D) {
                lookAt(adult, true);
                ArmadilloBrain.this.walkTarget = new WalkTarget(adult, SPEED_WHEN_FOLLOWING_ADULT, 4);
                return true;
            }
            return false;
        }
    }

    /** 1.20's {@code RandomLookAround(150-250, 30, 0, 0)}: glance up to 30 degrees aside, level. */
    final class RandomLookAround extends Behavior {
        RandomLookAround() {
            super(60, 60);
        }

        @Override
        boolean memoriesOk() {
            return ArmadilloBrain.this.look == null && ArmadilloBrain.this.gazeCooldown < 0;
        }

        @Override
        void start(long now) {
            EntityArmadillo a = ArmadilloBrain.this.armadillo;
            Random r = ArmadilloBrain.this.rand;
            float pitch = MathHelper.clamp(r.nextFloat() * 0.0F + 0.0F, -90.0F, 90.0F);
            float yaw = MathHelper.wrapDegrees(a.rotationYaw + 2.0F * r.nextFloat() * 30.0F - 30.0F);
            Vec3d dir = Vec3d.fromPitchYaw(pitch, yaw);
            Vec3d eye = new Vec3d(a.posX, a.posY + a.getEyeHeight(), a.posZ);
            ArmadilloBrain.this.look = new Look(null, false, eye.add(dir));
            ArmadilloBrain.this.gazeCooldown = 150 + r.nextInt(101);
        }
    }

    /** 1.20's {@code RandomStroll.stroll(1.0)}: a land spot within 10 across and 7 up or down. */
    final class RandomStroll extends OneShot {
        @Override
        boolean trigger() {
            if (ArmadilloBrain.this.walkTarget != null) {
                return false;
            }
            Vec3d to = landPos(10, 7);
            if (to != null) {
                ArmadilloBrain.this.walkTarget = new WalkTarget(new BlockPos(to), SPEED_WHEN_IDLING, 0);
            }
            return true;
        }
    }

    /** 1.20's {@code SetWalkTargetFromLookTarget.create(1.0, 3)}. */
    final class WalkToLookTarget extends OneShot {
        @Override
        boolean trigger() {
            Look l = ArmadilloBrain.this.look;
            if (l == null || ArmadilloBrain.this.walkTarget != null) {
                return false;
            }
            ArmadilloBrain.this.walkTarget = l.entity != null ? new WalkTarget(l.entity, SPEED_WHEN_IDLING, 3)
                    : new WalkTarget(l.blockPos(), SPEED_WHEN_IDLING, 3);
            return true;
        }
    }

    /** 1.20's {@code DoNothing(30, 60)}. */
    final class DoNothing extends Behavior {
        DoNothing() {
            super(30, 60);
        }

        @Override
        boolean canStillUse(long now) {
            return true;
        }
    }

    // ---------------------------------------------------------------- panic

    /**
     * 1.20.5's {@code ArmadilloBallUp}: on the ground it rolls up, and stays rolled up for up to 5 minutes while
     * it feels threatened. Rolling for more than 10 ticks it is rolled up (the landing sound if on the ground);
     * rolled up, it peeks out every 150-450 ticks while danger was seen within the last 5 ticks of the memory,
     * and starts unrolling when the memory has under 30 ticks left; new danger while unrolling rolls it back up.
     */
    final class BallUp extends Behavior {
        int nextPeekTimer;
        boolean dangerWasAround;

        BallUp() {
            super(BALL_UP_STAY_IN_STATE, BALL_UP_STAY_IN_STATE);
        }

        @Override
        boolean extra() {
            return ArmadilloBrain.this.armadillo.onGround;
        }

        @Override
        boolean canStillUse(long now) {
            return ArmadilloBrain.this.armadillo.getState().isThreatened();
        }

        @Override
        void start(long now) {
            ArmadilloBrain.this.armadillo.rollUp();
        }

        @Override
        void stop(long now) {
            if (!ArmadilloBrain.this.armadillo.canStayRolledUp()) {
                ArmadilloBrain.this.armadillo.rollOut();
            }
        }

        @Override
        void tick(long now) {
            EntityArmadillo a = ArmadilloBrain.this.armadillo;
            if (this.nextPeekTimer > 0) {
                this.nextPeekTimer--;
            }
            if (a.shouldSwitchToScaredState()) {
                a.switchToState(ArmadilloState.SCARED);
                if (a.onGround) {
                    a.playSound(ArmadilloContent.LAND, 1.0F, 1.0F);
                }
                return;
            }
            ArmadilloState state = a.getState();
            int left = ArmadilloBrain.this.danger;
            boolean dangerIsAround = left > DANGER_THRESHOLD;
            if (dangerIsAround != this.dangerWasAround) {
                this.nextPeekTimer = pickNextPeekTimer();
            }
            this.dangerWasAround = dangerIsAround;
            if (state == ArmadilloState.SCARED) {
                if (this.nextPeekTimer == 0 && a.onGround && dangerIsAround) {
                    a.world.setEntityState(a, EntityArmadillo.EVENT_PEEK);
                    this.nextPeekTimer = pickNextPeekTimer();
                }
                if (left < ArmadilloState.UNROLLING.animationDuration()) {
                    a.playSound(ArmadilloContent.UNROLL_START, 1.0F, 1.0F);
                    a.switchToState(ArmadilloState.UNROLLING);
                }
            } else if (state == ArmadilloState.UNROLLING && left > ArmadilloState.UNROLLING.animationDuration()) {
                a.switchToState(ArmadilloState.SCARED);
            }
        }

        private int pickNextPeekTimer() {
            return ArmadilloState.SCARED.animationDuration() + 100 + ArmadilloBrain.this.rand.nextInt(301);
        }
    }
}
