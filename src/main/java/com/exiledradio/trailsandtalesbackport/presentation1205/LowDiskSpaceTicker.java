package com.exiledradio.trailsandtalesbackport.presentation1205;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import javax.annotation.Nullable;
import java.io.File;

/**
 * When 1.20.6's integrated server looks at the disk: {@code IntegratedServer.saveEverything} ends with {@code
 * warnOnLowDiskSpace}, and the server saves everything on its autosave (every 6000 ticks, {@code
 * computeNextAutosaveInterval} at 20 ticks a second, counted only while the game runs), on {@code /save-all}, and when
 * the game pauses (the pause is the client's, {@code Presentation1205Client}). 1.12.2 autosaves every 900 ticks; the
 * toast keeps 1.20.6's cadence rather than following 1.12.2's saves.
 *
 * <p>Registered by the client only: a dedicated server shows nothing, as 1.20.6's.
 */
public final class LowDiskSpaceTicker {

    /** 1.20.6's autosave interval at 20 ticks a second. */
    public static final int AUTOSAVE_INTERVAL = 6000;

    /** Who shows the toast. */
    public interface Listener {
        void lowDiskSpace();
    }

    @Nullable
    private static volatile Listener listener;
    private int ticksUntilAutosave = AUTOSAVE_INTERVAL;
    /** Set by {@code /save-all}, which runs before its save; the check comes at the end of that tick. */
    private volatile boolean afterSaveAll;

    /** For the self-tests: how often the disk was looked at. */
    public static volatile int checks;

    public static void setListener(@Nullable Listener l) {
        listener = l;
    }

    /** @return the integrated server's overworld folder, or null on a dedicated server or with no world */
    @Nullable
    public static File worldFolder() {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null || server.isDedicatedServer()) {
            return null;
        }
        WorldServer overworld = server.getWorld(0);
        return overworld == null ? null : overworld.getSaveHandler().getWorldDirectory();
    }

    /** 1.20.6's {@code IntegratedServer.warnOnLowDiskSpace}. */
    public static void check() {
        if (!Presentation1205.lowDiskSpace()) {
            return;
        }
        File folder = worldFolder();
        if (folder == null) {
            return;
        }
        checks++;
        Listener l = listener;
        if (l != null && DiskSpace.isLow(folder)) {
            l.lowDiskSpace();
        }
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        if (!event.getWorld().isRemote && event.getWorld().provider.getDimension() == 0) {
            this.ticksUntilAutosave = AUTOSAVE_INTERVAL;
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            if (this.afterSaveAll) {
                this.afterSaveAll = false;
                check();
            }
            advance(1);
        }
    }

    /** Counts server ticks toward the next autosave check. */
    public void advance(int ticks) {
        this.ticksUntilAutosave -= ticks;
        if (this.ticksUntilAutosave <= 0) {
            this.ticksUntilAutosave = AUTOSAVE_INTERVAL;
            check();
        }
    }

    /** {@code /save-all} saves everything: the check follows once the command has run. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onCommand(CommandEvent event) {
        if (!"save-all".equals(event.getCommand().getName())) {
            return;
        }
        MinecraftServer server = event.getSender().getServer();
        if (server != null && !server.isDedicatedServer()) {
            this.afterSaveAll = true;
        }
    }
}
