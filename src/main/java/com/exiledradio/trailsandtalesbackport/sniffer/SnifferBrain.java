package com.exiledradio.trailsandtalesbackport.sniffer;

import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.pathfinding.Path;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * The sniffer's brain: 1.20's {@code SnifferAi} and the vanilla behaviors it is made of, on 1.12.2.
 *
 * <h2>How 1.20 runs it, and how this does</h2>
 * 1.20 gives the sniffer a {@code Brain}: memories, sensors that fill some of them every 20 ticks, and
 * behaviors in activities. Each tick every stopped behavior of the core activity and of the one active
 * activity (the first of dig, sniff and idle whose memory conditions hold) starts if its memories and
 * extra conditions allow; every running behavior - of any activity - then ticks until it times out (a random
 * time between its minimum and maximum) or its own condition fails. Behaviors run side by side; there is
 * no mutex. This class is that machine with the sniffer's own memories as fields and every behavior it
 * uses ported from 1.20 (current Java where they differ: the walk-to-target time is 500-700 ticks).
 *
 * <p>It is ticked from the entity's {@code updateAITasks}, ahead of 1.12.2's move and look helpers, as
 * 1.20 runs it from {@code customServerAiStep}. Saved with the entity: the sniff cooldown and the explored
 * positions, the two sniffer memories 1.20 saves.
 */
final class SnifferBrain {

    static final int SNIFFING_COOLDOWN_TICKS = 9600;
    static final float SPEED_WHEN_IDLING = 1.0F;
    static final float SPEED_WHEN_PANICKING = 2.0F;
    static final float SPEED_WHEN_SNIFFING = 1.25F;
    static final float SPEED_WHEN_TEMPTED = 1.25F;
    static final int TEMPTATION_COOLDOWN = 100;
    static final int MAX_EXPLORED = 20;

    private final EntitySniffer sniffer;
    private final Random rand;

    // ---------------------------------------------------------------- memories
    @Nullable WalkTarget walkTarget;
    @Nullable Entity lookEntity;
    @Nullable BlockPos lookPos;
    boolean panicking;
    @Nullable BlockPos sniffingTarget;
    boolean digging;
    boolean happy;
    /** Ticks left of the sniff cooldown; 0 when absent. */
    int sniffCooldown;
    /** Most recent first: {dimension, x, y, z}. */
    final List<int[]> explored = new ArrayList<int[]>();
    @Nullable EntitySniffer breedTarget;
    @Nullable EntityPlayer temptingPlayer;
    boolean tempted;
    /** Ticks left before temptation can start again; 0 when absent. */
    int temptationCooldown;
    /** The nearest living entities' visible ones, closest first; null until the first scan. */
    @Nullable List<EntityLivingBase> visible;
    @Nullable DamageSource hurtBy;

    private int sensorTimer;
    private Activity active = Activity.IDLE;

    // ---------------------------------------------------------------- behaviors
    private final List<Behavior> core = new ArrayList<Behavior>();
    private final List<Behavior> idle = new ArrayList<Behavior>();
    private final List<Behavior> sniff = new ArrayList<Behavior>();
    private final List<Behavior> dig = new ArrayList<Behavior>();
    private final MoveToTarget moveToTarget = new MoveToTarget();

    enum Activity { DIG, SNIFF, IDLE }

    SnifferBrain(EntitySniffer sniffer) {
        this.sniffer = sniffer;
        this.rand = sniffer.getRNG();
        this.sensorTimer = this.rand.nextInt(20);
        this.core.add(new Swim());
        this.core.add(new Panic());
        this.core.add(this.moveToTarget);
        this.core.add(new CountDownTemptation());
        this.idle.add(new MakeLove());
        this.idle.add(new FollowTemptation());
        this.idle.add(new LookAtTarget());
        this.idle.add(new FeelingHappy());
        RunOne runOne = new RunOne();
        runOne.add(new WalkToLookTarget(), 2);
        runOne.add(new Scenting(), 1);
        runOne.add(new Sniffing(), 1);
        runOne.add(new LookAtPlayer(), 1);
        runOne.add(new RandomStroll(), 1);
        runOne.add(new DoNothing(), 2);
        this.idle.add(runOne);
        this.sniff.add(new Searching());
        this.dig.add(new Digging());
        this.dig.add(new FinishedDigging());
    }

    // ---------------------------------------------------------------- the tick

    void tick() {
        long now = this.sniffer.world.getTotalWorldTime();
        if (--this.sensorTimer <= 0) {
            this.sensorTimer = 20;
            sense();
        }
        if (this.sniffCooldown > 0) {
            this.sniffCooldown--;
        }
        for (Behavior b : this.core) {
            if (!b.running) {
                b.tryStart(now);
            }
        }
        for (Behavior b : list(this.active)) {
            if (!b.running) {
                b.tryStart(now);
            }
        }
        for (List<Behavior> list : new List[] {this.core, this.idle, this.sniff, this.dig}) {
            for (Behavior b : list) {
                if (b.running) {
                    b.tickOrStop(now);
                }
            }
        }
        // 1.20's SnifferAi.updateActivity: the first of dig, sniff and idle whose conditions hold.
        if (!this.panicking && this.walkTarget == null && this.digging) {
            this.active = Activity.DIG;
        } else if (!this.panicking && this.sniffingTarget != null && this.walkTarget != null) {
            this.active = Activity.SNIFF;
        } else if (!this.digging) {
            this.active = Activity.IDLE;
        }
    }

    Activity activity() {
        return this.active;
    }

    private List<Behavior> list(Activity a) {
        switch (a) {
            case DIG:
                return this.dig;
            case SNIFF:
                return this.sniff;
            default:
                return this.idle;
        }
    }

    /** 1.20's sensors: nearest living entities, hurt by, and the sniffer's temptation (torchflower seeds). */
    private void sense() {
        EntitySniffer s = this.sniffer;
        List<EntityLivingBase> near = s.world.getEntitiesWithinAABB(EntityLivingBase.class,
                s.getEntityBoundingBox().grow(16.0D, 16.0D, 16.0D), e -> e != s && e != null && e.isEntityAlive());
        near.sort(Comparator.comparingDouble(s::getDistanceSq));
        List<EntityLivingBase> seen = new ArrayList<EntityLivingBase>();
        for (EntityLivingBase e : near) {
            if (s.getDistanceSq(e) <= 256.0D && !(e instanceof EntityPlayer && ((EntityPlayer) e).isSpectator())
                    && s.canEntityBeSeen(e)) {
                seen.add(e);
            }
        }
        this.visible = seen;
        this.hurtBy = s.getLastDamageSource();

        EntityPlayer best = null;
        double bestDist = Double.MAX_VALUE;
        for (EntityPlayer p : s.world.playerEntities) {
            if (p.isSpectator() || !p.isEntityAlive() || p.isRidingOrBeingRiddenBy(s)) {
                continue;
            }
            double d = s.getDistanceSq(p);
            if (d < 100.0D && (s.isBreedingItem(p.getHeldItemMainhand()) || s.isBreedingItem(p.getHeldItemOffhand()))
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

    /** 1.20's {@code SnifferAi.resetSniffing}. */
    void resetSniffing() {
        this.digging = false;
        this.sniffingTarget = null;
        this.sniffer.transitionTo(SnifferState.IDLING);
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

    // ---------------------------------------------------------------- explored positions

    boolean isExplored(BlockPos pos) {
        int dim = this.sniffer.dimension;
        for (int[] e : this.explored) {
            if (e[0] == dim && e[1] == pos.getX() && e[2] == pos.getY() && e[3] == pos.getZ()) {
                return true;
            }
        }
        return false;
    }

    /** 1.20's {@code storeExploredPosition}: the newest first, keeping at most 20 older ones. */
    void storeExplored(BlockPos pos) {
        while (this.explored.size() > MAX_EXPLORED) {
            this.explored.remove(this.explored.size() - 1);
        }
        this.explored.add(0, new int[] {this.sniffer.dimension, pos.getX(), pos.getY(), pos.getZ()});
    }

    void write(NBTTagCompound tag) {
        tag.setInteger("SniffCooldown", this.sniffCooldown);
        NBTTagList list = new NBTTagList();
        for (int[] e : this.explored) {
            NBTTagCompound t = new NBTTagCompound();
            t.setInteger("Dimension", e[0]);
            t.setInteger("X", e[1]);
            t.setInteger("Y", e[2]);
            t.setInteger("Z", e[3]);
            list.appendTag(t);
        }
        tag.setTag("ExploredPositions", list);
    }

    void read(NBTTagCompound tag) {
        this.sniffCooldown = Math.max(0, tag.getInteger("SniffCooldown"));
        this.explored.clear();
        NBTTagList list = tag.getTagList("ExploredPositions", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < list.tagCount() && i <= MAX_EXPLORED; i++) {
            NBTTagCompound t = list.getCompoundTagAt(i);
            this.explored.add(new int[] {t.getInteger("Dimension"), t.getInteger("X"), t.getInteger("Y"), t.getInteger("Z")});
        }
    }

    // ---------------------------------------------------------------- positions

    /** 1.20's {@code LandRandomPos.getPos}: a random standing spot on land, as 1.12.2 finds one. */
    @Nullable
    Vec3d landPos(int xz, int y) {
        return RandomPositionGenerator.getLandPos(this.sniffer, xz, y);
    }

    /** 1.20's {@code AnimalPanic.lookForWater}: the nearest 2x2 of water within 5 across and 1 up or down. */
    @Nullable
    private BlockPos lookForWater() {
        EntitySniffer s = this.sniffer;
        BlockPos at = new BlockPos(s);
        if (s.world.getBlockState(at).getCollisionBoundingBox(s.world, at) != null) {
            return null;
        }
        boolean wide = MathHelper.ceil(s.width) == 2;
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
        return this.sniffer.world.getBlockState(p).getMaterial() == Material.WATER;
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
                this.end = now + this.min + SnifferBrain.this.rand.nextInt(this.max + 1 - this.min);
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
        private final List<Behavior> children = new ArrayList<Behavior>();
        private final List<Integer> weights = new ArrayList<Integer>();

        RunOne() {
            super(1, 1);
        }

        void add(Behavior b, int weight) {
            this.children.add(b);
            this.weights.add(weight);
        }

        @Override
        boolean tryStart(long now) {
            this.running = true;
            // 1.20's ShufflingList: each entry draws -rand^(1/weight) and the list is sorted by it.
            final double[] keys = new double[this.children.size()];
            List<Integer> order = new ArrayList<Integer>();
            for (int i = 0; i < keys.length; i++) {
                keys[i] = -Math.pow(SnifferBrain.this.rand.nextFloat(), 1.0F / this.weights.get(i));
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
            return SnifferBrain.this.sniffer.isInWater() || SnifferBrain.this.sniffer.isInLava();
        }

        @Override
        boolean canStillUse(long now) {
            return extra();
        }

        @Override
        void tick(long now) {
            if (SnifferBrain.this.rand.nextFloat() < 0.8F) {
                SnifferBrain.this.sniffer.getJumpHelper().setJumping();
            }
        }
    }

    /** 1.20's {@code AnimalPanic(2.0)}, which first resets the sniffing. */
    final class Panic extends Behavior {
        Panic() {
            super(100, 120);
        }

        @Override
        boolean memoriesOk() {
            return SnifferBrain.this.hurtBy != null;
        }

        @Override
        boolean extra() {
            return SnifferBrain.this.sniffer.getRevengeTarget() != null || SnifferBrain.this.sniffer.isBurning();
        }

        @Override
        boolean canStillUse(long now) {
            return true;
        }

        @Override
        void start(long now) {
            resetSniffing();
            SnifferBrain.this.panicking = true;
            SnifferBrain.this.walkTarget = null;
        }

        @Override
        void stop(long now) {
            SnifferBrain.this.panicking = false;
        }

        @Override
        void tick(long now) {
            if (SnifferBrain.this.sniffer.getNavigator().noPath()) {
                Vec3d to = null;
                if (SnifferBrain.this.sniffer.isBurning()) {
                    BlockPos water = lookForWater();
                    if (water != null) {
                        to = new Vec3d(water.getX() + 0.5D, water.getY(), water.getZ() + 0.5D);
                    }
                }
                if (to == null) {
                    to = landPos(5, 4);
                }
                if (to != null) {
                    SnifferBrain.this.walkTarget = new WalkTarget(new BlockPos(to), SPEED_WHEN_PANICKING, 0);
                }
            }
        }
    }

    /** 1.20's {@code MoveToTargetSink}: follows the walk target's path; current Java's 500-700 ticks. */
    final class MoveToTarget extends Behavior {
        @Nullable private Path path;
        @Nullable private BlockPos lastTargetPos;
        private float speed;
        private int remainingCooldown;

        MoveToTarget() {
            super(500, 700);
        }

        @Override
        boolean memoriesOk() {
            return SnifferBrain.this.walkTarget != null;
        }

        @Override
        boolean extra() {
            if (this.remainingCooldown > 0) {
                this.remainingCooldown--;
                return false;
            }
            WalkTarget target = SnifferBrain.this.walkTarget;
            if (!reached(target) && computePath(target)
                    && (!endsHere(this.path) || target.blockPos().equals(SnifferBrain.this.sniffingTarget))) {
                this.lastTargetPos = target.blockPos();
                return true;
            }
            SnifferBrain.this.walkTarget = null;
            return false;
        }

        @Override
        boolean canStillUse(long now) {
            WalkTarget target = SnifferBrain.this.walkTarget;
            return this.path != null && this.lastTargetPos != null && target != null
                    && !SnifferBrain.this.sniffer.getNavigator().noPath() && !reached(target);
        }

        @Override
        void start(long now) {
            SnifferBrain.this.sniffer.getNavigator().setPath(this.path, this.speed);
        }

        @Override
        void tick(long now) {
            Path current = SnifferBrain.this.sniffer.getNavigator().getPath();
            if (current != null) {
                this.path = current;
            }
            WalkTarget target = SnifferBrain.this.walkTarget;
            if (this.path != null && this.lastTargetPos != null && target != null
                    && target.blockPos().distanceSq(this.lastTargetPos) > 4.0D && computePath(target)) {
                this.lastTargetPos = target.blockPos();
                start(now);
            }
        }

        @Override
        void stop(long now) {
            SnifferBrain.this.sniffer.getNavigator().clearPath();
            this.path = null;
        }

        /**
         * 1.20: reached when the target block is within its close-enough distance (Manhattan) of the mob.
         *
         * <p>1.20 walks a mob's center to the middle of each path node, so its block is the node's. 1.12.2
         * walks it to {@code node + (int) (width + 1) / 2}, a whole block off for a sniffer 1.9 wide, so the mob's
         * block is taken back to the node it walked to; without this a target "close enough at 0" is never
         * reached and the sniffer walks to it for ever.
         */
        boolean reached(WalkTarget target) {
            BlockPos t = target.blockPos();
            EntitySniffer s = SnifferBrain.this.sniffer;
            double half = (int) (s.width + 1.0F) * 0.5D;
            BlockPos at = new BlockPos(MathHelper.floor(s.posX - half + 0.5D), MathHelper.floor(s.posY),
                    MathHelper.floor(s.posZ - half + 0.5D));
            int d = Math.abs(t.getX() - at.getX()) + Math.abs(t.getY() - at.getY()) + Math.abs(t.getZ() - at.getZ());
            return d <= target.closeEnough;
        }

        /**
         * @return true if a path goes nowhere: it ends on the node the sniffer stands on. 1.12.2's pathfinder
         * then has brought it as close as it can, and a target it cannot get nearer to is let go, where
         * 1.20 would keep pathing to it; only the sniff target is kept, for Searching to time out as in 1.20.
         */
        private boolean endsHere(Path path) {
            PathPoint end = path.getFinalPathPoint();
            if (end == null) {
                return true;
            }
            EntitySniffer s = SnifferBrain.this.sniffer;
            double half = (int) (s.width + 1.0F) * 0.5D;
            return end.x == MathHelper.floor(s.posX - half + 0.5D) && end.z == MathHelper.floor(s.posZ - half + 0.5D)
                    && Math.abs(end.y - MathHelper.floor(s.posY)) <= 1;
        }

        private boolean computePath(WalkTarget target) {
            EntitySniffer s = SnifferBrain.this.sniffer;
            this.speed = target.speed;
            this.path = target.entity != null ? s.getNavigator().getPathToEntityLiving(target.entity)
                    : s.getNavigator().getPathToPos(target.blockPos());
            if (reached(target)) {
                return false;
            }
            if (this.path != null) {
                return true;
            }
            BlockPos t = target.blockPos();
            Vec3d towards = RandomPositionGenerator.findRandomTargetBlockTowards(s, 10, 7,
                    new Vec3d(t.getX() + 0.5D, t.getY(), t.getZ() + 0.5D));
            if (towards != null) {
                this.path = s.getNavigator().getPathToXYZ(towards.x, towards.y, towards.z);
                return this.path != null;
            }
            return false;
        }
    }

    /** 1.20's {@code CountDownCooldownTicks} on the temptation cooldown. */
    final class CountDownTemptation extends Behavior {
        CountDownTemptation() {
            super(60, 60);
        }

        @Override
        boolean memoriesOk() {
            return SnifferBrain.this.temptationCooldown > 0;
        }

        @Override
        boolean canStillUse(long now) {
            return SnifferBrain.this.temptationCooldown > 0;
        }

        @Override
        boolean timedOut(long now) {
            return false;
        }

        @Override
        void tick(long now) {
            SnifferBrain.this.temptationCooldown--;
        }
    }

    // ---------------------------------------------------------------- idle

    /** 1.20's {@code AnimalMakeLove}, which first resets the sniffing; the "child" is an egg. */
    final class MakeLove extends Behavior {
        private long spawnChildAt;

        MakeLove() {
            super(110, 110);
        }

        @Override
        boolean memoriesOk() {
            return SnifferBrain.this.visible != null && SnifferBrain.this.breedTarget == null;
        }

        @Override
        boolean extra() {
            return SnifferBrain.this.sniffer.isInLove() && partner() != null;
        }

        @Nullable
        private EntitySniffer partner() {
            for (EntityLivingBase e : SnifferBrain.this.visible) {
                if (e instanceof EntitySniffer && SnifferBrain.this.sniffer.canMateWith((EntitySniffer) e)) {
                    return (EntitySniffer) e;
                }
            }
            return null;
        }

        @Override
        void start(long now) {
            resetSniffing();
            EntitySniffer other = partner();
            SnifferBrain.this.breedTarget = other;
            other.brain.breedTarget = SnifferBrain.this.sniffer;
            lockGazeAndWalk(other);
            this.spawnChildAt = now + 60 + SnifferBrain.this.rand.nextInt(50);
        }

        private void lockGazeAndWalk(EntitySniffer other) {
            SnifferBrain.this.lookAt(other);
            other.brain.lookAt(SnifferBrain.this.sniffer);
            SnifferBrain.this.walkTarget = new WalkTarget(other, SPEED_WHEN_IDLING, 2);
            other.brain.walkTarget = new WalkTarget(SnifferBrain.this.sniffer, SPEED_WHEN_IDLING, 2);
        }

        @Override
        boolean canStillUse(long now) {
            EntitySniffer other = SnifferBrain.this.breedTarget;
            return other != null && other.isEntityAlive() && SnifferBrain.this.sniffer.canMateWith(other)
                    && isVisible(other) && now <= this.spawnChildAt;
        }

        @Override
        void tick(long now) {
            EntitySniffer other = SnifferBrain.this.breedTarget;
            lockGazeAndWalk(other);
            if (SnifferBrain.this.sniffer.getDistanceSq(other) < 9.0D && now >= this.spawnChildAt) {
                SnifferBrain.this.sniffer.spawnEggFromBreeding(other);
                SnifferBrain.this.breedTarget = null;
                other.brain.breedTarget = null;
            }
        }

        @Override
        void stop(long now) {
            SnifferBrain.this.breedTarget = null;
            SnifferBrain.this.walkTarget = null;
            clearLook();
            this.spawnChildAt = 0L;
        }
    }

    /** 1.20's {@code FollowTemptation}, 1.25 fast, close enough at 2.5 (snifflet) or 3.5 blocks. */
    final class FollowTemptation extends Behavior {
        FollowTemptation() {
            super(60, 60);
        }

        @Override
        boolean memoriesOk() {
            return SnifferBrain.this.temptingPlayer != null && SnifferBrain.this.temptationCooldown <= 0
                    && SnifferBrain.this.breedTarget == null && !SnifferBrain.this.panicking;
        }

        @Override
        boolean timedOut(long now) {
            return false;
        }

        @Override
        boolean canStillUse(long now) {
            return SnifferBrain.this.temptingPlayer != null && SnifferBrain.this.breedTarget == null
                    && !SnifferBrain.this.panicking;
        }

        @Override
        void start(long now) {
            resetSniffing();
            SnifferBrain.this.tempted = true;
        }

        @Override
        void stop(long now) {
            SnifferBrain.this.temptationCooldown = TEMPTATION_COOLDOWN;
            SnifferBrain.this.tempted = false;
            SnifferBrain.this.walkTarget = null;
            clearLook();
        }

        @Override
        void tick(long now) {
            EntityPlayer player = SnifferBrain.this.temptingPlayer;
            lookAt(player);
            double close = SnifferBrain.this.sniffer.isChild() ? 2.5D : 3.5D;
            if (SnifferBrain.this.sniffer.getDistanceSq(player) < close * close) {
                SnifferBrain.this.walkTarget = null;
            } else {
                SnifferBrain.this.walkTarget = new WalkTarget(player, SPEED_WHEN_TEMPTED, 2);
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
            if (SnifferBrain.this.lookEntity != null) {
                Entity e = SnifferBrain.this.lookEntity;
                return !(e instanceof EntityLivingBase) || e.isEntityAlive() && isVisible(e);
            }
            return SnifferBrain.this.lookPos != null;
        }

        @Override
        void tick(long now) {
            EntitySniffer s = SnifferBrain.this.sniffer;
            if (SnifferBrain.this.lookEntity != null) {
                Entity e = SnifferBrain.this.lookEntity;
                s.getLookHelper().setLookPosition(e.posX, e.posY + e.getEyeHeight(), e.posZ,
                        s.getHorizontalFaceSpeed(), s.getVerticalFaceSpeed());
            } else if (SnifferBrain.this.lookPos != null) {
                BlockPos p = SnifferBrain.this.lookPos;
                s.getLookHelper().setLookPosition(p.getX() + 0.5D, p.getY() + 0.5D, p.getZ() + 0.5D,
                        s.getHorizontalFaceSpeed(), s.getVerticalFaceSpeed());
            }
        }

        @Override
        void stop(long now) {
            clearLook();
        }
    }

    /** 1.20's {@code SnifferAi.FeelingHappy(40, 100)}. */
    final class FeelingHappy extends Behavior {
        FeelingHappy() {
            super(40, 100);
        }

        @Override
        boolean memoriesOk() {
            return SnifferBrain.this.happy;
        }

        @Override
        boolean canStillUse(long now) {
            return true;
        }

        @Override
        void start(long now) {
            SnifferBrain.this.sniffer.transitionTo(SnifferState.FEELING_HAPPY);
        }

        @Override
        void stop(long now) {
            SnifferBrain.this.sniffer.transitionTo(SnifferState.IDLING);
            SnifferBrain.this.happy = false;
        }
    }

    /** 1.20's {@code SetWalkTargetFromLookTarget.create(1.0, 3)}. */
    final class WalkToLookTarget extends OneShot {
        @Override
        boolean trigger() {
            if (!hasLook() || SnifferBrain.this.walkTarget != null) {
                return false;
            }
            SnifferBrain.this.walkTarget = SnifferBrain.this.lookEntity != null
                    ? new WalkTarget(SnifferBrain.this.lookEntity, SPEED_WHEN_IDLING, 3)
                    : new WalkTarget(SnifferBrain.this.lookPos, SPEED_WHEN_IDLING, 3);
            return true;
        }
    }

    /** 1.20's {@code SetEntityLookTarget.create(PLAYER, 6)}: the closest visible player within 6 blocks. */
    final class LookAtPlayer extends OneShot {
        @Override
        boolean trigger() {
            if (hasLook() || SnifferBrain.this.visible == null) {
                return false;
            }
            for (EntityLivingBase e : SnifferBrain.this.visible) {
                if (e instanceof EntityPlayer && SnifferBrain.this.sniffer.getDistanceSq(e) <= 36.0D) {
                    lookAt(e);
                    return true;
                }
            }
            return false;
        }
    }

    /** 1.20's {@code RandomStroll.stroll(1.0)}: a land spot within 10 across and 7 up or down. */
    final class RandomStroll extends OneShot {
        @Override
        boolean trigger() {
            if (SnifferBrain.this.walkTarget != null) {
                return false;
            }
            Vec3d to = landPos(10, 7);
            if (to != null) {
                SnifferBrain.this.walkTarget = new WalkTarget(new BlockPos(to), SPEED_WHEN_IDLING, 0);
            }
            return true;
        }
    }

    /** 1.20's {@code DoNothing(5, 20)}. */
    final class DoNothing extends Behavior {
        DoNothing() {
            super(5, 20);
        }

        @Override
        boolean canStillUse(long now) {
            return true;
        }
    }

    /** 1.20's {@code SnifferAi.Scenting(40, 80)}: lifts its nose and sniffs the air. */
    final class Scenting extends Behavior {
        Scenting() {
            super(40, 80);
        }

        @Override
        boolean memoriesOk() {
            return !SnifferBrain.this.panicking && !SnifferBrain.this.digging && SnifferBrain.this.sniffingTarget == null
                    && !SnifferBrain.this.happy && SnifferBrain.this.breedTarget == null;
        }

        @Override
        boolean extra() {
            return !SnifferBrain.this.tempted;
        }

        @Override
        boolean canStillUse(long now) {
            return true;
        }

        @Override
        void start(long now) {
            SnifferBrain.this.sniffer.transitionTo(SnifferState.SCENTING);
        }

        @Override
        void stop(long now) {
            SnifferBrain.this.sniffer.transitionTo(SnifferState.IDLING);
        }
    }

    /** 1.20's {@code SnifferAi.Sniffing(40, 80)}: a long sniff that, run to the end, picks a spot to dig. */
    final class Sniffing extends Behavior {
        Sniffing() {
            super(40, 80);
        }

        @Override
        boolean memoriesOk() {
            return SnifferBrain.this.walkTarget == null && SnifferBrain.this.sniffingTarget == null
                    && SnifferBrain.this.sniffCooldown <= 0;
        }

        @Override
        boolean extra() {
            return !SnifferBrain.this.sniffer.isChild() && SnifferBrain.this.sniffer.canSniff();
        }

        @Override
        boolean canStillUse(long now) {
            return SnifferBrain.this.sniffer.canSniff();
        }

        @Override
        void start(long now) {
            SnifferBrain.this.sniffer.transitionTo(SnifferState.SNIFFING);
        }

        @Override
        void stop(long now) {
            boolean done = timedOut(now);
            SnifferBrain.this.sniffer.transitionTo(SnifferState.IDLING);
            if (done) {
                BlockPos at = SnifferBrain.this.sniffer.calculateDigPosition();
                if (at != null) {
                    SnifferBrain.this.sniffingTarget = at;
                    SnifferBrain.this.walkTarget = new WalkTarget(at, SPEED_WHEN_SNIFFING, 0);
                }
            }
        }
    }

    // ---------------------------------------------------------------- sniff

    /** 1.20's {@code SnifferAi.Searching}: walks to the spot with its nose down, for up to 600 ticks. */
    final class Searching extends Behavior {
        Searching() {
            super(600, 600);
        }

        @Override
        boolean memoriesOk() {
            return SnifferBrain.this.walkTarget != null && !SnifferBrain.this.panicking
                    && SnifferBrain.this.sniffingTarget != null;
        }

        @Override
        boolean extra() {
            return SnifferBrain.this.sniffer.canSniff();
        }

        @Override
        boolean canStillUse(long now) {
            if (!SnifferBrain.this.sniffer.canSniff()) {
                SnifferBrain.this.sniffer.transitionTo(SnifferState.IDLING);
                return false;
            }
            WalkTarget target = SnifferBrain.this.walkTarget;
            BlockPos sniffing = SnifferBrain.this.sniffingTarget;
            return target != null && sniffing != null && sniffing.equals(target.blockPos());
        }

        @Override
        void start(long now) {
            SnifferBrain.this.sniffer.transitionTo(SnifferState.SEARCHING);
        }

        @Override
        void stop(long now) {
            if (SnifferBrain.this.sniffer.canDig() && SnifferBrain.this.sniffer.canSniff()) {
                SnifferBrain.this.digging = true;
            }
            SnifferBrain.this.walkTarget = null;
            SnifferBrain.this.sniffingTarget = null;
        }
    }

    // ---------------------------------------------------------------- dig

    /** 1.20's {@code SnifferAi.Digging(160, 180)}. */
    final class Digging extends Behavior {
        Digging() {
            super(160, 180);
        }

        @Override
        boolean memoriesOk() {
            return !SnifferBrain.this.panicking && SnifferBrain.this.walkTarget == null && SnifferBrain.this.digging
                    && SnifferBrain.this.sniffCooldown <= 0;
        }

        @Override
        boolean extra() {
            return SnifferBrain.this.sniffer.canSniff();
        }

        @Override
        boolean canStillUse(long now) {
            return SnifferBrain.this.digging && SnifferBrain.this.sniffer.canDig() && !SnifferBrain.this.sniffer.isInLove();
        }

        @Override
        void start(long now) {
            SnifferBrain.this.sniffer.transitionTo(SnifferState.DIGGING);
        }

        @Override
        void stop(long now) {
            if (timedOut(now)) {
                SnifferBrain.this.sniffCooldown = SNIFFING_COOLDOWN_TICKS;
            } else {
                resetSniffing();
            }
        }
    }

    /** 1.20's {@code SnifferAi.FinishedDigging(40)}: stands up, remembers the spot, is happy. */
    final class FinishedDigging extends Behavior {
        FinishedDigging() {
            super(40, 40);
        }

        @Override
        boolean memoriesOk() {
            return !SnifferBrain.this.panicking && SnifferBrain.this.walkTarget == null && SnifferBrain.this.digging
                    && SnifferBrain.this.sniffCooldown > 0;
        }

        @Override
        boolean canStillUse(long now) {
            return SnifferBrain.this.digging;
        }

        @Override
        void start(long now) {
            SnifferBrain.this.sniffer.transitionTo(SnifferState.RISING);
        }

        @Override
        void stop(long now) {
            boolean done = timedOut(now);
            SnifferBrain.this.sniffer.transitionTo(SnifferState.IDLING);
            SnifferBrain.this.sniffer.onDiggingComplete(done);
            SnifferBrain.this.digging = false;
            SnifferBrain.this.happy = true;
        }
    }
}
