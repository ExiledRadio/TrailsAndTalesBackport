package com.exiledradio.trailsandtalesbackport.presentation1205;

import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;
import net.minecraft.world.chunk.storage.IChunkLoader;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.message.Message;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 1.20.5's chunk toasts, the part that finds the failures: 1.20.6's {@code ChunkMap} calls {@code
 * MinecraftServer.reportChunkLoadFailure / reportChunkSaveFailure} (which only the integrated server answers, with
 * the toast) wherever it catches a chunk that cannot be read or written. 1.12.2 catches the same failures and only
 * logs them, so an appender on the loggers of those catch blocks hears every one, as it happens, on the thread that
 * caught it:
 *
 * <table>
 *   <tr><th>logger</th><th>line</th><th>where 1.12.2 writes it</th><th>chunk</th></tr>
 *   <tr><td>FML</td><td>"Unhandled exception loading chunk:", then the {@code QueuedChunk}'s text</td>
 *       <td>Forge's chunk loading threads, for every chunk read from disk while playing</td><td>the QueuedChunk</td></tr>
 *   <tr><td>AnvilChunkLoader</td><td>"Failed to save chunk"</td><td>the file IO thread (writing a region file) or
 *       the server thread (turning a chunk into NBT)</td><td>{@code chunksBeingSaved}, or patch P4's</td></tr>
 *   <tr><td>ChunkProviderServer</td><td>"Couldn't load chunk" / "Couldn't save chunk ..."</td><td>the server thread
 *       </td><td>patch P4's</td></tr>
 * </table>
 * Patch P4 hands this class the chunk {@code ChunkProviderServer.saveChunkData} and {@code loadChunkFromFile} are
 * working on, on their first line, since those log lines name no chunk. Failures that are an {@link Error} are not
 * reported, as 1.20.6 crashes on those instead.
 *
 * <p>The appender is installed only on a client (1.20.6's dedicated server reports nothing), and by the self-tests.
 */
public final class ChunkFailures {

    public static final String APPENDER = "TrailsAndTalesBackport-ChunkFailures";
    static final String ANVIL = "net.minecraft.world.chunk.storage.AnvilChunkLoader";
    static final String PROVIDER = "net.minecraft.world.gen.ChunkProviderServer";
    static final String FML = "FML";
    static final String QUEUED_CHUNK = "net.minecraftforge.common.chunkio.QueuedChunk";

    /** Who shows the toast (the client). */
    public interface Listener {
        /** Called on the thread that caught the failure. */
        void chunkFailure(boolean load, ChunkPos pos);
    }

    @Nullable
    private static volatile Listener listener;
    private static volatile boolean active;
    private static Appender appender;

    private static final ThreadLocal<int[]> SAVING = new ThreadLocal<int[]>();
    private static final ThreadLocal<int[]> LOADING = new ThreadLocal<int[]>();
    private static final ThreadLocal<Boolean> FML_PENDING = new ThreadLocal<Boolean>();

    private static Field beingSaved;
    private static final Pattern QUEUED = Pattern.compile("QueuedChunk \\{\\s*x: (-?\\d+)\\s*z: (-?\\d+)");

    /** For the self-tests: the reports so far, and the last one ("load [x, z]" / "save [x, z]"). */
    public static volatile int reports;
    @Nullable
    public static volatile String lastReport;

    private ChunkFailures() {
    }

    public static void setListener(@Nullable Listener l) {
        listener = l;
    }

    // ---- Patch P4's hooks (first line of the method; the server thread) ----

    /** {@code ChunkProviderServer.saveChunkData(chunk)} starts. */
    public static void saving(Chunk chunk) {
        if (active && chunk != null) {
            remember(SAVING, chunk.x, chunk.z);
        }
    }

    /** {@code ChunkProviderServer.loadChunkFromFile(x, z)} starts. */
    public static void loading(int x, int z) {
        if (active) {
            remember(LOADING, x, z);
        }
    }

    private static void remember(ThreadLocal<int[]> slot, int x, int z) {
        int[] pos = slot.get();
        if (pos == null) {
            pos = new int[2];
            slot.set(pos);
        }
        pos[0] = x;
        pos[1] = z;
    }

    @Nullable
    private static ChunkPos recalled(ThreadLocal<int[]> slot) {
        int[] pos = slot.get();
        return pos == null ? null : new ChunkPos(pos[0], pos[1]);
    }

    // ---- The appender ----

    /**
     * Adds the appender to the three loggers (again, if a log4j reconfiguration dropped it). Safe to call often.
     *
     * @return true when it is in place
     */
    public static synchronized boolean install() {
        try {
            if (appender == null) {
                appender = new Appender();
                appender.start();
            }
            for (String name : new String[] {ANVIL, PROVIDER, FML}) {
                Logger logger = (Logger) LogManager.getLogger(name);
                if (!logger.getAppenders().containsKey(APPENDER)) {
                    logger.getContext().getConfiguration().addLoggerAppender(logger, appender);
                }
            }
            active = true;
            return true;
        } catch (Throwable t) {
            // Another logging setup (not log4j's core): no toasts, nothing else changes.
            TrailsAndTalesBackport.LOGGER.warn("Chunk toasts: cannot watch the chunk loggers ({}); no chunk toasts", t.toString());
            return false;
        }
    }

    public static boolean isActive() {
        return active;
    }

    static void report(boolean load, @Nullable ChunkPos pos) {
        if (pos == null || !Presentation1205.chunkToasts()) {
            return;
        }
        reports++;
        lastReport = (load ? "load " : "save ") + pos;
        Listener l = listener;
        if (l != null) {
            l.chunkFailure(load, pos);
        }
    }

    static void onEvent(String logger, Message message, @Nullable Throwable thrown) {
        // The text as written: log4j's garbage-free events keep no format or parameters of their own.
        String format = message.getFormattedMessage();
        if (FML.equals(logger)) {
            if ("Unhandled exception loading chunk:".equals(format)) {
                FML_PENDING.set(thrown instanceof Error ? null : Boolean.TRUE);
                return;
            }
            if (Boolean.TRUE.equals(FML_PENDING.get()) && format != null && format.startsWith(QUEUED_CHUNK)) {
                FML_PENDING.set(null);
                report(true, queuedPos(format));
            }
            return;
        }
        if (format == null || thrown instanceof Error) {
            return;
        }
        if (ANVIL.equals(logger) && "Failed to save chunk".equals(format)) {
            ChunkPos pos = isServerThread() ? recalled(SAVING) : beingSaved();
            report(false, pos);
        } else if (PROVIDER.equals(logger)) {
            if ("Couldn't load chunk".equals(format)) {
                report(true, recalled(LOADING));
            } else if (format.startsWith("Couldn't save chunk")) {
                report(false, recalled(SAVING));
            }
        }
    }

    private static boolean isServerThread() {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        return server != null && server.isCallingFromMinecraftThread();
    }

    /** The chunk the file IO thread is writing: the one entry of a loader's {@code chunksBeingSaved}. */
    @Nullable
    private static ChunkPos beingSaved() {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null) {
            return null;
        }
        ChunkPos found = null;
        try {
            if (beingSaved == null) {
                beingSaved = ReflectionHelper.findField(AnvilChunkLoader.class, "chunksBeingSaved", "field_193415_c");
            }
            for (WorldServer world : server.worlds) {
                if (world == null) {
                    continue;
                }
                IChunkLoader loader = ((ChunkProviderServer) world.getChunkProvider()).chunkLoader;
                if (loader instanceof AnvilChunkLoader) {
                    for (Object pos : (Set<?>) beingSaved.get(loader)) {
                        if (found != null) {
                            return null;
                        }
                        found = (ChunkPos) pos;
                    }
                }
            }
        } catch (Exception e) {
            return null;
        }
        return found;
    }

    /** QueuedChunk.toString: {@code "net.minecraftforge.common.chunkio.QueuedChunk {
 x: 1
 z: 2
 world: ..."}. */
    @Nullable
    static ChunkPos queuedPos(String text) {
        Matcher m = QUEUED.matcher(text);
        return m.find() ? new ChunkPos(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2))) : null;
    }

    private static final class Appender extends AbstractAppender {

        @SuppressWarnings("deprecation")
        Appender() {
            super(APPENDER, null, null, true);
        }

        @Override
        public void append(LogEvent event) {
            if (event.getLevel() != Level.ERROR) {
                return;
            }
            try {
                onEvent(event.getLoggerName(), event.getMessage(), event.getThrown());
            } catch (Throwable ignored) {
                // Never let a toast break logging.
            }
        }
    }
}
