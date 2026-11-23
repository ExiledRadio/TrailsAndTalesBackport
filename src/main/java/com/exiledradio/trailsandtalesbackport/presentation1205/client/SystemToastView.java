package com.exiledradio.trailsandtalesbackport.presentation1205.client;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

/** A shown system toast's text and size, from Backport Core's drawer or {@link SystemToast1205}. */
@SideOnly(Side.CLIENT)
public interface SystemToastView {
    String title();

    @Nullable
    String message();

    int width();

    int height();
}
