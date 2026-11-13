package com.exiledradio.trailsandtalesbackport.armadillo.client;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 1.20's keyframe animations ({@code AnimationDefinition}, {@code AnimationChannel}, {@code Keyframe} and
 * {@code KeyframeAnimations.animate}) for 1.12.2 models made of {@link AnimatedPart}s.
 *
 * <p>Each bone has channels; a channel moves, turns or scales its part by keyframes. At a time, the two
 * keyframes around it are found, the later one's interpolation (linear or Catmull-Rom) gives the value,
 * which is multiplied by a scale (the walk's strength) and added to the part's pose, as 1.20 does.
 */
public final class KeyframeAnimation {

    public static final int POSITION = 0;
    public static final int ROTATION = 1;
    public static final int SCALE = 2;

    /** How a keyframe is reached from the one before. */
    public interface Interpolation {
        void apply(float[] out, float alpha, Key[] keys, int from, int to, float scale);
    }

    public static final Interpolation LINEAR = (out, alpha, keys, from, to, scale) -> {
        Key a = keys[from];
        Key b = keys[to];
        out[0] = (a.x + (b.x - a.x) * alpha) * scale;
        out[1] = (a.y + (b.y - a.y) * alpha) * scale;
        out[2] = (a.z + (b.z - a.z) * alpha) * scale;
    };

    public static final Interpolation CATMULLROM = (out, alpha, keys, from, to, scale) -> {
        Key p0 = keys[Math.max(0, from - 1)];
        Key p1 = keys[from];
        Key p2 = keys[to];
        Key p3 = keys[Math.min(keys.length - 1, to + 1)];
        out[0] = catmullrom(alpha, p0.x, p1.x, p2.x, p3.x) * scale;
        out[1] = catmullrom(alpha, p0.y, p1.y, p2.y, p3.y) * scale;
        out[2] = catmullrom(alpha, p0.z, p1.z, p2.z, p3.z) * scale;
    };

    /** 1.20's {@code Mth.catmullrom}. */
    static float catmullrom(float t, float p0, float p1, float p2, float p3) {
        return 0.5F * (2.0F * p1 + (p2 - p0) * t + (2.0F * p0 - 5.0F * p1 + 4.0F * p2 - p3) * t * t
                + (3.0F * p1 - p0 - 3.0F * p2 + p3) * t * t * t);
    }

    /** A keyframe: a time in seconds and a target, reached by its interpolation. */
    public static final class Key {
        final float time;
        final float x;
        final float y;
        final float z;
        final Interpolation interpolation;

        Key(float time, float[] target, Interpolation interpolation) {
            this.time = time;
            this.x = target[0];
            this.y = target[1];
            this.z = target[2];
            this.interpolation = interpolation;
        }
    }

    private static final class Channel {
        final int target;
        final Key[] keys;

        Channel(int target, Key[] keys) {
            this.target = target;
            this.keys = keys;
        }
    }

    private final float lengthInSeconds;
    private boolean looping;
    private final Map<String, List<Channel>> bones = new LinkedHashMap<String, List<Channel>>();

    private KeyframeAnimation(float lengthInSeconds) {
        this.lengthInSeconds = lengthInSeconds;
    }

    public static KeyframeAnimation withLength(float seconds) {
        return new KeyframeAnimation(seconds);
    }

    public KeyframeAnimation looping() {
        this.looping = true;
        return this;
    }

    public KeyframeAnimation add(String bone, int target, Key... keys) {
        List<Channel> list = this.bones.get(bone);
        if (list == null) {
            list = new ArrayList<Channel>();
            this.bones.put(bone, list);
        }
        list.add(new Channel(target, keys));
        return this;
    }

    public static Key key(float time, float[] target, Interpolation interpolation) {
        return new Key(time, target, interpolation);
    }

    /** 1.20's {@code posVec}: model y points down, so y is negated. */
    public static float[] pos(float x, float y, float z) {
        return new float[] {x, -y, z};
    }

    /** 1.20's {@code degreeVec}, in radians. */
    public static float[] deg(float x, float y, float z) {
        float r = (float) (Math.PI / 180.0D);
        return new float[] {x * r, y * r, z * r};
    }

    /** 1.20's {@code scaleVec}: stored as the change from 1. */
    public static float[] scale(float x, float y, float z) {
        return new float[] {x - 1.0F, y - 1.0F, z - 1.0F};
    }

    /** 1.20's {@code KeyframeAnimations.animate}: adds this animation at a time to the parts' poses. */
    public void apply(Map<String, AnimatedPart> parts, long millis, float scale) {
        float seconds = millis / 1000.0F;
        if (this.looping) {
            seconds %= this.lengthInSeconds;
        }
        float[] v = new float[3];
        for (Map.Entry<String, List<Channel>> bone : this.bones.entrySet()) {
            AnimatedPart part = parts.get(bone.getKey());
            if (part == null) {
                continue;
            }
            for (Channel channel : bone.getValue()) {
                Key[] keys = channel.keys;
                int from = Math.max(0, firstAtOrAfter(keys, seconds) - 1);
                int to = Math.min(keys.length - 1, from + 1);
                float delta = seconds - keys[from].time;
                float alpha = to != from ? clamp(delta / (keys[to].time - keys[from].time)) : 0.0F;
                keys[to].interpolation.apply(v, alpha, keys, from, to, scale);
                switch (channel.target) {
                    case POSITION:
                        part.rotationPointX += v[0];
                        part.rotationPointY += v[1];
                        part.rotationPointZ += v[2];
                        break;
                    case ROTATION:
                        part.rotateAngleX += v[0];
                        part.rotateAngleY += v[1];
                        part.rotateAngleZ += v[2];
                        break;
                    default:
                        part.xScale += v[0];
                        part.yScale += v[1];
                        part.zScale += v[2];
                        break;
                }
            }
        }
    }

    /** 1.20's {@code Mth.binarySearch} for the first keyframe whose time is at or after the given seconds. */
    private static int firstAtOrAfter(Key[] keys, float seconds) {
        int lo = 0;
        int hi = keys.length;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (seconds <= keys[mid].time) {
                hi = mid;
            } else {
                lo = mid + 1;
            }
        }
        return lo;
    }

    private static float clamp(float f) {
        return f < 0.0F ? 0.0F : Math.min(f, 1.0F);
    }
}
