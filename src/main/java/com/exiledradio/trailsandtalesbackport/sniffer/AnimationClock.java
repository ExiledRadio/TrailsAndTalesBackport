package com.exiledradio.trailsandtalesbackport.sniffer;

/**
 * 1.20's {@code AnimationState}: when an animation started, in entity ticks, or stopped.
 *
 * <p>Its time is the milliseconds since the start at 20 ticks a second, as current Java reads it from
 * {@code tickCount} (1.20.1 summed the same time frame by frame).
 */
public final class AnimationClock {

    private static final long STOPPED = Long.MIN_VALUE;
    private long startTick = STOPPED;

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

    /** @return milliseconds since the start at this (partial) tick */
    public long timeInMillis(float ageInTicks) {
        return (long) ((ageInTicks - this.startTick) * 50.0F);
    }
}
