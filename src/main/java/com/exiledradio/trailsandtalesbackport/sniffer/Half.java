package com.exiledradio.trailsandtalesbackport.sniffer;

import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.util.IStringSerializable;

/** 1.13's {@code DoubleBlockHalf}, with its state names. */
public enum Half implements IStringSerializable {
    LOWER("lower"),
    UPPER("upper");

    public static final PropertyEnum<Half> PROPERTY = PropertyEnum.create("half", Half.class);

    private final String name;

    Half(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
