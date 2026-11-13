package com.exiledradio.trailsandtalesbackport.armadillo;

/**
 * 1.20's {@code AnimationState}: when an animation started, in entity ticks, or stopped.
 *
 * <p>Its time is the milliseconds since the start at 20 ticks a second, as current Java reads it from the
 * tick count (1.20.6 sums the same time frame by frame).
 */
public final class AnimationClock {

    private static final float STOPPED = Float.NEGATIVE_INFINITY;
    private float startTick = STOPPED;

    public void start(int tick) {
        this.startTick = tick;
    }

    public void startIfStopped(int tick) {
        if (!isStarted()) {
            start(tick);
        }
    }

    public void stop() {
        this.startTick = STOPPED;
    }

    public boolean isStarted() {
        return this.startTick != STOPPED;
    }

    /** 1.20's {@code fastForward(ticks, speed)}: moves a started animation this many ticks on. */
    public void fastForward(int ticks, float speed) {
        if (isStarted()) {
            this.startTick -= ticks * speed;
        }
    }

    /** @return milliseconds since the start at this (partial) tick */
    public long timeInMillis(float ageInTicks) {
        return (long) ((ageInTicks - this.startTick) * 50.0F);
    }
}
