package com.exiledradio.trailsandtalesbackport.presentation1205.client;

import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import com.exiledradio.trailsandtalesbackport.presentation1205.ChunkFailures;
import com.exiledradio.trailsandtalesbackport.presentation1205.DiskSpace;
import com.exiledradio.trailsandtalesbackport.presentation1205.LowDiskSpaceTicker;
import com.exiledradio.trailsandtalesbackport.presentation1205.Presentation1205;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.File;

/**
 * Stream presentation1205, client side: the toasts (1.20.6's {@code SystemToast.onLowDiskSpace}, {@code
 * onChunkLoadFailure}, {@code onChunkSaveFailure}, called the way {@code IntegratedServer} calls them), the world list
 * and its checks ({@link WorldListHandler}, {@link WorldOpening}), the pause check of {@link LowDiskSpaceTicker} and
 * Viossa ({@link ViossaPack}).
 */
@SideOnly(Side.CLIENT)
public final class Presentation1205Client {

    private boolean wasPaused;

    private Presentation1205Client() {
    }

    public static void init() {
        Minecraft mc = Minecraft.getMinecraft();
        MinecraftForge.EVENT_BUS.register(new Presentation1205Client());
        MinecraftForge.EVENT_BUS.register(new WorldListHandler());
        MinecraftForge.EVENT_BUS.register(new LowDiskSpaceTicker());
        LowDiskSpaceTicker.setListener(() -> mc.addScheduledTask(() -> onLowDiskSpace(mc)));
        ChunkFailures.setListener((load, pos) -> mc.addScheduledTask(() -> {
            // IntegratedServer.reportChunkLoadFailure / reportChunkSaveFailure: the disk first, then the chunk.
            File folder = LowDiskSpaceTicker.worldFolder();
            if (Presentation1205.lowDiskSpace() && folder != null && DiskSpace.isLow(folder)) {
                onLowDiskSpace(mc);
            }
            if (load) {
                onChunkLoadFailure(mc, pos);
            } else {
                onChunkSaveFailure(mc, pos);
            }
        }));
        ChunkFailures.install();
        ViossaPack.install(mc);

        // `runClient -Pselftest=screens1205`: the client check, reached by name (a release jar has no test classes).
        String selftest = System.getProperty(TrailsAndTalesBackport.MOD_ID + ".selftest");
        if (selftest != null && selftest.contains("screens1205")) {
            try {
                Class.forName("com.exiledradio.trailsandtalesbackport.selftest.ClientPresentation1205Test").getMethod("register").invoke(null);
            } catch (ReflectiveOperationException e) {
                TrailsAndTalesBackport.LOGGER.error("CLIENT SELFTEST DONE pass=0 fail=1: the test classes are not in this build", e);
            }
        }
    }

    public static void onLowDiskSpace(Minecraft mc) {
        SystemToast1205.addOrUpdate(mc.getToastGui(), SystemToast1205.Id.LOW_DISK_SPACE,
                I18n.format("chunk.toast.lowDiskSpace"), I18n.format("chunk.toast.lowDiskSpace.description"));
    }

    public static void onChunkLoadFailure(Minecraft mc, ChunkPos pos) {
        SystemToast1205.addOrUpdate(mc.getToastGui(), SystemToast1205.Id.CHUNK_LOAD_FAILURE,
                TextFormatting.RED + I18n.format("chunk.toast.loadFailure", pos), I18n.format("chunk.toast.checkLog"));
    }

    public static void onChunkSaveFailure(Minecraft mc, ChunkPos pos) {
        SystemToast1205.addOrUpdate(mc.getToastGui(), SystemToast1205.Id.CHUNK_SAVE_FAILURE,
                TextFormatting.RED + I18n.format("chunk.toast.saveFailure", pos), I18n.format("chunk.toast.checkLog"));
    }

    /** The integrated server's worlds load: the appender goes back on if a log4j reconfiguration took it off. */
    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        if (!event.getWorld().isRemote && event.getWorld().provider.getDimension() == 0 && Presentation1205.chunkToasts()) {
            ChunkFailures.install();
        }
    }

    /** IntegratedServer.tickServer: pausing saves everything, which ends with the disk check. */
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        boolean paused = mc.isGamePaused() && mc.getIntegratedServer() != null;
        if (paused && !this.wasPaused) {
            mc.getIntegratedServer().addScheduledTask(LowDiskSpaceTicker::check);
        }
        this.wasPaused = paused;
    }
}
