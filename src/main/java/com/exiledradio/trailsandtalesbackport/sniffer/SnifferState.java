package com.exiledradio.trailsandtalesbackport.sniffer;

/** 1.20's {@code Sniffer.State}, in its order (the ordinal is what the client is sent). */
public enum SnifferState {
    IDLING,
    FEELING_HAPPY,
    SCENTING,
    SNIFFING,
    SEARCHING,
    DIGGING,
    RISING;

    private static final SnifferState[] VALUES = values();

    static SnifferState of(int ordinal) {
        return ordinal >= 0 && ordinal < VALUES.length ? VALUES[ordinal] : IDLING;
    }
}
