package com.exiledradio.trailsandtalesbackport.camel;

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
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.function.BooleanSupplier;

/**
 * The camel's brain: 1.20's {@code CamelAi} and the vanilla behaviors it is made of, on 1.12.2.
 *
 * <h2>How 1.20 runs it, and how this does</h2>
 * 1.20 gives the camel a {@code Brain}: memories, sensors that fill some of them every 20 ticks, and
 * behaviors by priority. Each tick every stopped behavior starts if its memories and its own conditions
 * allow, lowest priority number first; every running behavior then ticks until it times out (a random time
 * between its minimum and maximum) or its own condition fails. Behaviors run side by side; there is no
 * mutex. The camel has two activities, core and idle, both always active, so this is one list in 1.20's
 * priority order (core's six are numbered 0-5 by their place, idle's five are 0-4):
 * <pre>
 *   0 Swim(0.8)                         0 SetEntityLookTargetSometimes(player, 6, 30-60)
 *   1 CamelPanic(4.0)                   1 AnimalMakeLove(camel)
 *   2 LookAtTargetSink(45, 90)          2 RunOne(FollowTemptation 2.5, BabyFollowAdult 5-16 2.5)
 *   3 MoveToTargetSink(150, 250)        3 RandomLookAround(150-250, 30, 0, 0)
 *   4 CountDown(temptation cooldown)    4 RunOne(stroll 2.0, walk to look target 2.0, sit or stand, DoNothing 30-60)
 *   5 CountDown(gaze cooldown)
 * </pre>
 * It is ticked from the entity's {@code updateAITasks}, after 1.12.2's navigation and before its move and
 * look helpers, the order 1.20's {@code Mob.serverAiStep} has. Nothing is saved: 1.20 saves no camel memory.
 */
final class CamelBrain {

    static final float SPEED_WHEN_PANICKING = 4.0F;
    static final float SPEED_WHEN_IDLING = 2.0F;
    static final float SPEED_WHEN_TEMPTED = 2.5F;
    static final float SPEED_WHEN_FOLLOWING_ADULT = 2.5F;
    static final float SPEED_WHEN_MAKING_LOVE = 1.0F;
    static final int TEMPTATION_COOLDOWN = 100;
    static final int ADULT_FOLLOW_MIN = 5;
    static final int ADULT_FOLLOW_MAX = 16;
    static final int MINIMAL_POSE_TICKS = 20 * 20;

    private final EntityCamel camel;
    private final Random rand;

    // ---------------------------------------------------------------- memories
    @Nullable WalkTarget walkTarget;
    @Nullable Entity lookEntity;
    @Nullable Vec3d lookPos;
    boolean panicking;
    @Nullable EntityCamel breedTarget;
    @Nullable EntityPlayer temptingPlayer;
    boolean tempted;
    /** Ticks left; 0 when absent. */
    int temptationCooldown;
    int gazeCooldown;
    /** The visible living entities, closest first; null until the first scan. */
    @Nullable List<EntityLivingBase> visible;
    @Nullable EntityCamel nearestAdult;
    @Nullable DamageSource hurtBy;

    private int sensorTimer;
    private final List<Behavior> behaviors = new ArrayList<Behavior>();

    CamelBrain(EntityCamel camel) {
        this.camel = camel;
        this.rand = camel.getRNG();
        this.sensorTimer = this.rand.nextInt(20);
        this.behaviors.add(new Swim());
        this.behaviors.add(new LookAtPlayerSometimes());
        this.behaviors.add(new Panic());
        this.behaviors.add(new MakeLove());
        this.behaviors.add(new LookAtTarget());
        RunOne follow = new RunOne(() -> true);
        follow.add(new FollowTemptation(), 1);
        follow.add(new BabyFollowAdult(), 1);
        this.behaviors.add(follow);
        this.behaviors.add(new MoveToTarget());
        this.behaviors.add(new RandomLookAround());
        this.behaviors.add(new CountDown(true));
        RunOne idle = new RunOne(() -> this.walkTarget == null);
        idle.add(new RandomStroll(), 1);
        idle.add(new WalkToLookTarget(), 1);
        idle.add(new RandomSitting(), 1);
        idle.add(new DoNothing(), 1);
        this.behaviors.add(idle);
        this.behaviors.add(new CountDown(false));
    }

    // ---------------------------------------------------------------- the tick

    void tick() {
        long now = this.camel.world.getTotalWorldTime();
        if (--this.sensorTimer <= 0) {
            this.sensorTimer = 20;
            sense();
        }
        for (Behavior b : this.behaviors) {
            if (!b.running) {
                b.tryStart(now);
            }
        }
        for (Behavior b : this.behaviors) {
            if (b.running) {
                b.tickOrStop(now);
            }
        }
    }

    /**
     * 1.20's sensors: nearest living entities (and the visible ones), hurt by, the camel's temptation (a player
     * within 10 blocks holding cactus, not riding it) and the nearest visible grown camel.
     */
    void sense() {
        EntityCamel c = this.camel;
        List<EntityLivingBase> near = c.world.getEntitiesWithinAABB(EntityLivingBase.class,
                c.getEntityBoundingBox().grow(16.0D, 16.0D, 16.0D), e -> e != c && e != null && e.isEntityAlive());
        near.sort(Comparator.comparingDouble(c::getDistanceSq));
        List<EntityLivingBase> seen = new ArrayList<EntityLivingBase>();
        EntityCamel adult = null;
        for (EntityLivingBase e : near) {
            if (c.getDistanceSq(e) <= 256.0D && !(e instanceof EntityPlayer && ((EntityPlayer) e).isSpectator())
                    && c.canEntityBeSeen(e)) {
                seen.add(e);
                if (adult == null && e instanceof EntityCamel && !e.isChild()) {
                    adult = (EntityCamel) e;
                }
            }
        }
        this.visible = seen;
        this.nearestAdult = adult;
        this.hurtBy = c.getLastDamageSource();

        EntityPlayer best = null;
        double bestDist = Double.MAX_VALUE;
        for (EntityPlayer p : c.world.playerEntities) {
            if (p.isSpectator() || !p.isEntityAlive() || c.isPassenger(p)) {
                continue;
            }
            double d = c.getDistanceSq(p);
            if (d < 100.0D && (c.isBreedingItem(p.getHeldItemMainhand()) || c.isBreedingItem(p.getHeldItemOffhand()))
                    && d < bestDist) {
                best = p;
                bestDist = d;
            }
        }
        this.temptingPlayer = best;
    }

    boolean isVisible(Entity e) {
        return this.visible != null && this.visible.contains(e);
    }

    void lookAt(Entity e) {
        this.lookEntity = e;
        this.lookPos = null;
    }

    void clearLook() {
        this.lookEntity = null;
        this.lookPos = null;
    }

    boolean hasLook() {
        return this.lookEntity != null || this.lookPos != null;
    }

    /** 1.20's {@code LandRandomPos.getPos}: a random standing spot on land, as 1.12.2 finds one. */
    @Nullable
    Vec3d landPos(int xz, int y) {
        return RandomPositionGenerator.getLandPos(this.camel, xz, y);
    }

    /** 1.20's {@code AnimalPanic.lookForWater}: the nearest 2x2 of water within 5 across and 1 up or down. */
    @Nullable
    private BlockPos lookForWater() {
        EntityCamel c = this.camel;
        BlockPos at = new BlockPos(c);
        if (c.world.getBlockState(at).getCollisionBoundingBox(c.world, at) != null) {
            return null;
        }
        boolean wide = MathHelper.ceil(c.width) == 2;
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.getAllInBox(at.add(-5, -1, -5), at.add(5, 1, 5))) {
            boolean water = isWater(p) && (!wide || isWater(p.south()) && isWater(p.east()) && isWater(p.south().east()));
            double d = p.distanceSq(at);
            if (water && d < bestDist) {
                best = p.toImmutable();
                bestDist = d;
            }
        }
        return best;
    }

    private boolean isWater(BlockPos p) {
        return this.camel.world.getBlockState(p).getMaterial() == Material.WATER;
    }

    // ---------------------------------------------------------------- walk targets

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

    /** 1.20's {@code Behavior}: a random run time between min and max, entry memories, start/tick/stop. */
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
                this.end = now + this.min + CamelBrain.this.rand.nextInt(this.max + 1 - this.min);
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
        private final BooleanSupplier entry;
        private final List<Behavior> children = new ArrayList<Behavior>();
        private final List<Integer> weights = new ArrayList<Integer>();

        RunOne(BooleanSupplier entry) {
            super(1, 1);
            this.entry = entry;
        }

        void add(Behavior b, int weight) {
            this.children.add(b);
            this.weights.add(weight);
        }

        @Override
        boolean tryStart(long now) {
            if (!this.entry.getAsBoolean()) {
                return false;
            }
            this.running = true;
            // 1.20's ShufflingList: each entry draws -rand^(1/weight) and the list is sorted by it.
            final double[] keys = new double[this.children.size()];
            List<Integer> order = new ArrayList<Integer>();
            for (int i = 0; i < keys.length; i++) {
                keys[i] = -Math.pow(CamelBrain.this.rand.nextFloat(), 1.0F / this.weights.get(i));
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

    /** 1.20's {@code Swim(0.8)}: jump in water or lava, 80% of ticks. */
    final class Swim extends Behavior {
        Swim() {
            super(60, 60);
        }

        @Override
        boolean extra() {
            return CamelBrain.this.camel.isInWater() || CamelBrain.this.camel.isInLava();
        }

        @Override
        boolean canStillUse(long now) {
            return extra();
        }

        @Override
        void tick(long now) {
            if (CamelBrain.this.rand.nextFloat() < 0.8F) {
                CamelBrain.this.camel.getJumpHelper().setJumping();
            }
        }
    }

    /** 1.20's {@code CamelPanic(4.0)}: {@code AnimalPanic}, standing up at once first. */
    final class Panic extends Behavior {
        Panic() {
            super(100, 120);
        }

        @Override
        boolean memoriesOk() {
            return CamelBrain.this.hurtBy != null || CamelBrain.this.panicking;
        }

        @Override
        boolean extra() {
            return CamelBrain.this.camel.getRevengeTarget() != null || CamelBrain.this.camel.isBurning();
        }

        @Override
        boolean canStillUse(long now) {
            return true;
        }

        @Override
        void start(long now) {
            CamelBrain.this.camel.standUpInstantly();
            CamelBrain.this.panicking = true;
            CamelBrain.this.walkTarget = null;
        }

        @Override
        void stop(long now) {
            CamelBrain.this.panicking = false;
        }

        @Override
        void tick(long now) {
            if (CamelBrain.this.camel.getNavigator().noPath()) {
                Vec3d to = null;
                if (CamelBrain.this.camel.isBurning()) {
                    BlockPos water = lookForWater();
                    if (water != null) {
                        to = new Vec3d(water.getX() + 0.5D, water.getY(), water.getZ() + 0.5D);
                    }
                }
                if (to == null) {
                    to = landPos(5, 4);
                }
                if (to != null) {
                    CamelBrain.this.walkTarget = new WalkTarget(new BlockPos(to), SPEED_WHEN_PANICKING, 0);
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
            return hasLook();
        }

        @Override
        boolean canStillUse(long now) {
            if (CamelBrain.this.lookEntity != null) {
                Entity e = CamelBrain.this.lookEntity;
                return !(e instanceof EntityLivingBase) || e.isEntityAlive() && isVisible(e);
            }
            return CamelBrain.this.lookPos != null;
        }

        @Override
        void tick(long now) {
            EntityCamel c = CamelBrain.this.camel;
            if (CamelBrain.this.lookEntity != null) {
                Entity e = CamelBrain.this.lookEntity;
                c.getLookHelper().setLookPosition(e.posX, e.posY + e.getEyeHeight(), e.posZ,
                        c.getHorizontalFaceSpeed(), c.getVerticalFaceSpeed());
            } else if (CamelBrain.this.lookPos != null) {
                Vec3d p = CamelBrain.this.lookPos;
                c.getLookHelper().setLookPosition(p.x, p.y, p.z, c.getHorizontalFaceSpeed(), c.getVerticalFaceSpeed());
            }
        }

        @Override
        void stop(long now) {
            clearLook();
        }
    }

    /** 1.20's {@code MoveToTargetSink(150, 250)}: follows the walk target's path. */
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
            return CamelBrain.this.walkTarget != null;
        }

        @Override
        boolean extra() {
            if (this.remainingCooldown > 0) {
                this.remainingCooldown--;
                return false;
            }
            WalkTarget target = CamelBrain.this.walkTarget;
            if (!reached(target) && computePath(target) && !endsHere(this.path)) {
                this.lastTargetPos = target.blockPos();
                return true;
            }
            CamelBrain.this.walkTarget = null;
            return false;
        }

        @Override
        boolean canStillUse(long now) {
            WalkTarget target = CamelBrain.this.walkTarget;
            return this.path != null && this.lastTargetPos != null && target != null
                    && !CamelBrain.this.camel.getNavigator().noPath() && !reached(target);
        }

        @Override
        void start(long now) {
            CamelBrain.this.camel.getNavigator().setPath(this.path, this.speed);
        }

        @Override
        void tick(long now) {
            Path current = CamelBrain.this.camel.getNavigator().getPath();
            if (current != null) {
                this.path = current;
            }
            WalkTarget target = CamelBrain.this.walkTarget;
            if (this.path != null && this.lastTargetPos != null && target != null
                    && target.blockPos().distanceSq(this.lastTargetPos) > 4.0D && computePath(target)) {
                this.lastTargetPos = target.blockPos();
                start(now);
            }
        }

        @Override
        void stop(long now) {
            CamelBrain.this.camel.getNavigator().clearPath();
            CamelBrain.this.walkTarget = null;
            this.path = null;
        }

        /**
         * 1.20: reached when the target block is within its close-enough distance (Manhattan) of the mob.
         *
         * <p>1.20 walks a mob's center to the middle of each path node. 1.12.2 walks it to
         * {@code node + (int) (width + 1) / 2}, a whole block off for a camel 1.7 wide, so the mob's block is
         * taken back to the node it walked to; without this a target "close enough at 0" is never reached.
         */
        boolean reached(WalkTarget target) {
            BlockPos t = target.blockPos();
            EntityCamel c = CamelBrain.this.camel;
            double half = (int) (c.width + 1.0F) * 0.5D;
            BlockPos at = new BlockPos(MathHelper.floor(c.posX - half + 0.5D), MathHelper.floor(c.posY),
                    MathHelper.floor(c.posZ - half + 0.5D));
            int d = Math.abs(t.getX() - at.getX()) + Math.abs(t.getY() - at.getY()) + Math.abs(t.getZ() - at.getZ());
            return d <= target.closeEnough;
        }

        /**
         * @return true if a path goes nowhere: it ends on the node the camel stands on. 1.12.2's pathfinder
         * has then brought it as close as it can, and the target is let go.
         */
        private boolean endsHere(Path path) {
            PathPoint end = path.getFinalPathPoint();
            if (end == null) {
                return true;
            }
            EntityCamel c = CamelBrain.this.camel;
            double half = (int) (c.width + 1.0F) * 0.5D;
            return end.x == MathHelper.floor(c.posX - half + 0.5D) && end.z == MathHelper.floor(c.posZ - half + 0.5D)
                    && Math.abs(end.y - MathHelper.floor(c.posY)) <= 1;
        }

        private boolean computePath(WalkTarget target) {
            EntityCamel c = CamelBrain.this.camel;
            this.speed = target.speed;
            this.path = target.entity != null ? c.getNavigator().getPathToEntityLiving(target.entity)
                    : c.getNavigator().getPathToPos(target.blockPos());
            if (reached(target)) {
                return false;
            }
            if (this.path != null) {
                return true;
            }
            BlockPos t = target.blockPos();
            Vec3d towards = RandomPositionGenerator.findRandomTargetBlockTowards(c, 10, 7,
                    new Vec3d(t.getX() + 0.5D, t.getY(), t.getZ() + 0.5D));
            if (towards != null) {
                this.path = c.getNavigator().getPathToXYZ(towards.x, towards.y, towards.z);
                return this.path != null;
            }
            return false;
        }
    }

    /** 1.20's {@code CountDownCooldownTicks} on the temptation or the gaze cooldown. */
    final class CountDown extends Behavior {
        private final boolean temptation;

        CountDown(boolean temptation) {
            super(60, 60);
            this.temptation = temptation;
        }

        private int value() {
            return this.temptation ? CamelBrain.this.temptationCooldown : CamelBrain.this.gazeCooldown;
        }

        private void set(int v) {
            if (this.temptation) {
                CamelBrain.this.temptationCooldown = v;
            } else {
                CamelBrain.this.gazeCooldown = v;
            }
        }

        @Override
        boolean memoriesOk() {
            return value() > 0;
        }

        @Override
        boolean canStillUse(long now) {
            return value() > 0;
        }

        @Override
        boolean timedOut(long now) {
            return false;
        }

        @Override
        void tick(long now) {
            set(value() - 1);
        }

        @Override
        void stop(long now) {
            set(0);
        }
    }

    // ---------------------------------------------------------------- idle

    /** 1.20's {@code SetEntityLookTargetSometimes(PLAYER, 6, 30-60)}. */
    final class LookAtPlayerSometimes extends OneShot {
        private int ticksUntilNextStart;

        @Override
        boolean trigger() {
            if (hasLook() || CamelBrain.this.visible == null) {
                return false;
            }
            EntityPlayer player = null;
            for (EntityLivingBase e : CamelBrain.this.visible) {
                if (e instanceof EntityPlayer && CamelBrain.this.camel.getDistanceSq(e) <= 36.0D) {
                    player = (EntityPlayer) e;
                    break;
                }
            }
            if (player == null) {
                return false;
            }
            // 1.20's Ticker.
            if (this.ticksUntilNextStart == 0) {
                this.ticksUntilNextStart = 30 + CamelBrain.this.rand.nextInt(31) - 1;
                return false;
            }
            if (--this.ticksUntilNextStart != 0) {
                return false;
            }
            lookAt(player);
            return true;
        }
    }

    /** 1.20's {@code AnimalMakeLove(CAMEL)}: speed 1.0, close enough at 2, a baby after 60-109 ticks. */
    final class MakeLove extends Behavior {
        private long spawnChildAt;

        MakeLove() {
            super(110, 110);
        }

        @Override
        boolean memoriesOk() {
            return CamelBrain.this.visible != null && CamelBrain.this.breedTarget == null && !CamelBrain.this.panicking;
        }

        @Override
        boolean extra() {
            return CamelBrain.this.camel.isInLove() && partner() != null;
        }

        @Nullable
        private EntityCamel partner() {
            for (EntityLivingBase e : CamelBrain.this.visible) {
                if (e instanceof EntityCamel && CamelBrain.this.camel.canMateWith((EntityCamel) e)
                        && !((EntityCamel) e).isPanicking()) {
                    return (EntityCamel) e;
                }
            }
            return null;
        }

        @Override
        void start(long now) {
            EntityCamel other = partner();
            CamelBrain.this.breedTarget = other;
            other.brain.breedTarget = CamelBrain.this.camel;
            lockGazeAndWalk(other);
            this.spawnChildAt = now + 60 + CamelBrain.this.rand.nextInt(50);
        }

        private void lockGazeAndWalk(EntityCamel other) {
            CamelBrain.this.lookAt(other);
            other.brain.lookAt(CamelBrain.this.camel);
            CamelBrain.this.walkTarget = new WalkTarget(other, SPEED_WHEN_MAKING_LOVE, 2);
            other.brain.walkTarget = new WalkTarget(CamelBrain.this.camel, SPEED_WHEN_MAKING_LOVE, 2);
        }

        @Override
        boolean canStillUse(long now) {
            EntityCamel other = CamelBrain.this.breedTarget;
            return other != null && other.isEntityAlive() && CamelBrain.this.camel.canMateWith(other)
                    && isVisible(other) && now <= this.spawnChildAt
                    && !CamelBrain.this.panicking && !other.isPanicking();
        }

        @Override
        void tick(long now) {
            EntityCamel other = CamelBrain.this.breedTarget;
            lockGazeAndWalk(other);
            if (CamelBrain.this.camel.getDistanceSq(other) < 9.0D && now >= this.spawnChildAt) {
                CamelBrain.this.camel.spawnChildFromBreeding(other);
                CamelBrain.this.breedTarget = null;
                other.brain.breedTarget = null;
            }
        }

        @Override
        void stop(long now) {
            CamelBrain.this.breedTarget = null;
            CamelBrain.this.walkTarget = null;
            clearLook();
            this.spawnChildAt = 0L;
        }
    }

    /** 1.20's {@code FollowTemptation}, 2.5 fast, close enough at 2.5 (baby) or 3.5 blocks. */
    final class FollowTemptation extends Behavior {
        FollowTemptation() {
            super(60, 60);
        }

        @Override
        boolean memoriesOk() {
            return CamelBrain.this.temptingPlayer != null && CamelBrain.this.temptationCooldown <= 0
                    && CamelBrain.this.breedTarget == null && !CamelBrain.this.panicking;
        }

        @Override
        boolean timedOut(long now) {
            return false;
        }

        @Override
        boolean canStillUse(long now) {
            return CamelBrain.this.temptingPlayer != null && CamelBrain.this.breedTarget == null
                    && !CamelBrain.this.panicking;
        }

        @Override
        void start(long now) {
            CamelBrain.this.tempted = true;
        }

        @Override
        void stop(long now) {
            CamelBrain.this.temptationCooldown = TEMPTATION_COOLDOWN;
            CamelBrain.this.tempted = false;
            CamelBrain.this.walkTarget = null;
            clearLook();
        }

        @Override
        void tick(long now) {
            EntityPlayer player = CamelBrain.this.temptingPlayer;
            lookAt(player);
            double close = CamelBrain.this.camel.isChild() ? 2.5D : 3.5D;
            if (CamelBrain.this.camel.getDistanceSq(player) < close * close) {
                CamelBrain.this.walkTarget = null;
            } else {
                CamelBrain.this.walkTarget = new WalkTarget(player, SPEED_WHEN_TEMPTED, 2);
            }
        }
    }

    /** 1.20's {@code BabyFollowAdult.create(5-16, 2.5)}, only while the camel will move. */
    final class BabyFollowAdult extends OneShot {
        @Override
        boolean trigger() {
            EntityCamel c = CamelBrain.this.camel;
            EntityCamel adult = CamelBrain.this.nearestAdult;
            if (c.refuseToMove() || adult == null || CamelBrain.this.walkTarget != null || !c.isChild()) {
                return false;
            }
            double d = c.getDistanceSq(adult);
            double far = ADULT_FOLLOW_MAX + 1;
            if (d < far * far && !(d < ADULT_FOLLOW_MIN * ADULT_FOLLOW_MIN)) {
                lookAt(adult);
                CamelBrain.this.walkTarget = new WalkTarget(adult, SPEED_WHEN_FOLLOWING_ADULT, ADULT_FOLLOW_MIN - 1);
                return true;
            }
            return false;
        }
    }

    /** 1.20's {@code RandomLookAround(150-250, 30, 0, 0)}: a glance up to 30 degrees aside, level. */
    final class RandomLookAround extends Behavior {
        RandomLookAround() {
            super(60, 60);
        }

        @Override
        boolean memoriesOk() {
            return !hasLook() && CamelBrain.this.gazeCooldown <= 0;
        }

        @Override
        void start(long now) {
            EntityCamel c = CamelBrain.this.camel;
            float pitch = 0.0F;
            float yaw = MathHelper.wrapDegrees(c.rotationYaw + 2.0F * CamelBrain.this.rand.nextFloat() * 30.0F - 30.0F);
            // 1.20's Vec3.directionFromRotation(pitch, yaw).
            float f = MathHelper.cos(-yaw * 0.017453292F - (float) Math.PI);
            float f1 = MathHelper.sin(-yaw * 0.017453292F - (float) Math.PI);
            float f2 = -MathHelper.cos(-pitch * 0.017453292F);
            float f3 = MathHelper.sin(-pitch * 0.017453292F);
            Vec3d dir = new Vec3d(f1 * f2, f3, f * f2);
            CamelBrain.this.lookEntity = null;
            CamelBrain.this.lookPos = new Vec3d(c.posX, c.posY + c.getEyeHeight(), c.posZ).add(dir);
            CamelBrain.this.gazeCooldown = 150 + CamelBrain.this.rand.nextInt(101);
        }
    }

    /** 1.20's {@code RandomStroll.stroll(2.0)}: a land spot within 10 across and 7 up or down. */
    final class RandomStroll extends OneShot {
        @Override
        boolean trigger() {
            if (CamelBrain.this.camel.refuseToMove() || CamelBrain.this.walkTarget != null) {
                return false;
            }
            Vec3d to = landPos(10, 7);
            if (to != null) {
                CamelBrain.this.walkTarget = new WalkTarget(new BlockPos(to), SPEED_WHEN_IDLING, 0);
            }
            return true;
        }
    }

    /** 1.20's {@code SetWalkTargetFromLookTarget.create(2.0, 3)}. */
    final class WalkToLookTarget extends OneShot {
        @Override
        boolean trigger() {
            if (CamelBrain.this.camel.refuseToMove() || !hasLook() || CamelBrain.this.walkTarget != null) {
                return false;
            }
            CamelBrain.this.walkTarget = CamelBrain.this.lookEntity != null
                    ? new WalkTarget(CamelBrain.this.lookEntity, SPEED_WHEN_IDLING, 3)
                    : new WalkTarget(new BlockPos(CamelBrain.this.lookPos), SPEED_WHEN_IDLING, 3);
            return true;
        }
    }

    /** 1.20's {@code CamelAi.RandomSitting(20)}: sits down, or stands up, after 20 seconds in a pose. */
    final class RandomSitting extends Behavior {
        RandomSitting() {
            super(60, 60);
        }

        @Override
        boolean extra() {
            EntityCamel c = CamelBrain.this.camel;
            return !c.isInWater() && c.getPoseTime() >= MINIMAL_POSE_TICKS && !c.getLeashed() && c.onGround
                    && c.getControllingPassenger() == null && c.canCamelChangePose();
        }

        @Override
        void start(long now) {
            EntityCamel c = CamelBrain.this.camel;
            if (c.isCamelSitting()) {
                c.standUp();
            } else if (!CamelBrain.this.panicking) {
                c.sitDown();
            }
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
}
