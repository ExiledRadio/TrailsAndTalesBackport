package com.exiledradio.trailsandtalesbackport.armadillo;

/**
 * 1.20.5's {@code Armadillo.ArmadilloState}: idle, rolling up, rolled up (scared) and unrolling, with each
 * state's animation length in ticks and when the shell hides the rest of the body.
 */
public enum ArmadilloState {
    IDLE("idle", false, 0) {
        @Override
        public boolean shouldHideInShell(long inStateTicks) {
            return false;
        }
    },
    ROLLING("rolling", true, 10) {
        @Override
        public boolean shouldHideInShell(long inStateTicks) {
            return inStateTicks > 5L;
        }
    },
    SCARED("scared", true, 50) {
        @Override
        public boolean shouldHideInShell(long inStateTicks) {
            return true;
        }
    },
    UNROLLING("unrolling", true, 30) {
        @Override
        public boolean shouldHideInShell(long inStateTicks) {
            return inStateTicks < 26L;
        }
    };

    private final String serializedName;
    private final boolean threatened;
    private final int animationDuration;

    ArmadilloState(String serializedName, boolean threatened, int animationDuration) {
        this.serializedName = serializedName;
        this.threatened = threatened;
        this.animationDuration = animationDuration;
    }

    public abstract boolean shouldHideInShell(long inStateTicks);

    public boolean isThreatened() {
        return this.threatened;
    }

    public int animationDuration() {
        return this.animationDuration;
    }

    /** 1.20's saved name ({@code "state"} in the entity's NBT). */
    public String serializedName() {
        return this.serializedName;
    }

    /** @return the state saved under this name, or idle (1.20's codec default) */
    public static ArmadilloState fromName(String name) {
        for (ArmadilloState s : values()) {
            if (s.serializedName.equals(name)) {
                return s;
            }
        }
        return IDLE;
    }

    /** @return the state with this network id (1.20's ids are the ordinals), or idle when out of range */
    public static ArmadilloState byId(int id) {
        ArmadilloState[] all = values();
        return id >= 0 && id < all.length ? all[id] : IDLE;
    }
}
