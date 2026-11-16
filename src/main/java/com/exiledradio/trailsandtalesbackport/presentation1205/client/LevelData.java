package com.exiledradio.trailsandtalesbackport.presentation1205.client;

import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.datafix.FixTypes;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.SignStyle;
import java.time.temporal.ChronoField;
import java.util.HashMap;
import java.util.Map;

/**
 * A world folder's {@code level.dat} and {@code level.dat_old} as 1.20.6's {@code LevelStorageAccess} reads them:
 * {@link #issue} is what {@code getSummary(getDataTag())} / {@code getDataTagFallback()} throw (1.12.2's terms: the
 * file read with {@code Files.newInputStream}, gzip NBT, its {@code Data} through the data fixer into a {@code
 * WorldInfo}), and {@link #restoreFromOld} is {@code restoreLevelDataFromOld}.
 */
@SideOnly(Side.CLIENT)
public final class LevelData {

    public static final String LEVEL = "level.dat";
    public static final String OLD = "level.dat_old";

    /** 1.20.6's {@code FileNameDateFormatter}: {@code yyyy-MM-dd_HH-mm-ss}. */
    static final DateTimeFormatter FILE_DATE = new DateTimeFormatterBuilder()
            .appendValue(ChronoField.YEAR, 4, 10, SignStyle.EXCEEDS_PAD).appendLiteral('-')
            .appendValue(ChronoField.MONTH_OF_YEAR, 2).appendLiteral('-')
            .appendValue(ChronoField.DAY_OF_MONTH, 2).appendLiteral('_')
            .appendValue(ChronoField.HOUR_OF_DAY, 2).appendLiteral('-')
            .appendValue(ChronoField.MINUTE_OF_HOUR, 2).appendLiteral('-')
            .appendValue(ChronoField.SECOND_OF_MINUTE, 2).toFormatter();

    /** The world list reads every world's level.dat; a file unchanged since is not read again. */
    private static final Map<String, Object[]> CACHE = new HashMap<String, Object[]>();

    private LevelData() {
    }

    /** @return what reading the file throws, or null when it reads as a world */
    @Nullable
    public static Exception issue(File folder, boolean old) {
        Path path = new File(folder, old ? OLD : LEVEL).toPath();
        try (InputStream in = Files.newInputStream(path)) {
            NBTTagCompound root = CompressedStreamTools.readCompressed(in);
            NBTTagCompound data = root.getCompoundTag("Data");
            new WorldInfo(Minecraft.getMinecraft().getDataFixer().process(FixTypes.LEVEL, data));
            return null;
        } catch (IOException e) {
            return e;
        } catch (RuntimeException e) {
            // 1.20.6's NbtException / ReportedNbtException: what 1.12.2's reader throws on a damaged tag.
            return e;
        }
    }

    /** {@link #issue} of level.dat, remembered while the file's time and size stay the same. */
    @Nullable
    public static synchronized Exception cachedIssue(File folder) {
        File file = new File(folder, LEVEL);
        String key = file.getAbsolutePath();
        long stamp = file.lastModified() * 31L + file.length();
        Object[] hit = CACHE.get(key);
        if (hit != null && (Long) hit[0] == stamp) {
            return (Exception) hit[1];
        }
        Exception issue = issue(folder, false);
        CACHE.put(key, new Object[] {stamp, issue});
        return issue;
    }

    /** @return the file's modification time, or null (1.20.6's {@code getFileModificationTime}) */
    @Nullable
    public static Instant modified(File folder, boolean old) {
        try {
            return Files.getLastModifiedTime(new File(folder, old ? OLD : LEVEL).toPath()).toInstant();
        } catch (IOException e) {
            return null;
        }
    }

    /** 1.20.6's {@code LevelStorageSource.getFileModificationTime(LevelDirectory)}: level.dat's, else level.dat_old's, else -1. */
    public static long lastPlayed(File folder) {
        Instant t = modified(folder, false);
        if (t == null) {
            t = modified(folder, true);
        }
        return t == null ? -1L : t.toEpochMilli();
    }

    /** 1.20.6's world list shows a folder with either file present. */
    public static boolean listed(File folder) {
        return new File(folder, LEVEL).isFile() || new File(folder, OLD).isFile();
    }

    /**
     * 1.20.6's {@code restoreLevelDataFromOld}, {@code Util.safeReplaceOrMoveFile(level.dat, level.dat_old,
     * level.dat_corrupted_<date>, true)}: level.dat (if any) moves aside, then level.dat_old becomes level.dat.
     */
    public static boolean restoreFromOld(File folder) {
        // 1.12.2's own readers (SaveFormatOld.getWorldData, FMLClientHandler.tryLoadExistingWorld) leave a file that
        // is not gzip open until it is collected, and Windows will not move an open file.
        releaseLeakedStreams();
        Path level = new File(folder, LEVEL).toPath();
        Path old = new File(folder, OLD).toPath();
        Path corrupted = new File(folder, LEVEL + "_corrupted_" + LocalDateTime.now().format(FILE_DATE)).toPath();
        if (Files.exists(level) && !retry("create backup " + corrupted, () -> {
            Files.deleteIfExists(corrupted);
            Files.move(level, corrupted);
            return Files.isRegularFile(corrupted);
        })) {
            return false;
        }
        if (!retry("remove old " + level, () -> {
            Files.deleteIfExists(level);
            return !Files.exists(level);
        })) {
            return false;
        }
        // With restore-on-failure off (1.20.6 passes true), a failed move still counts as done.
        retry("replace " + level + " with " + old, () -> {
            Files.move(old, level, StandardCopyOption.REPLACE_EXISTING);
            return Files.isRegularFile(level);
        });
        return true;
    }

    private static void releaseLeakedStreams() {
        System.gc();
        System.runFinalization();
    }

    private interface Step {
        boolean run() throws IOException;
    }

    /** 1.20.6's {@code Util.runWithRetries(10, ...)}. */
    private static boolean retry(String what, Step step) {
        for (int i = 0; i < 10; i++) {
            try {
                if (step.run()) {
                    return true;
                }
            } catch (IOException e) {
                TrailsAndTalesBackport.LOGGER.warn("Failed to execute step {}", what, e);
            }
            TrailsAndTalesBackport.LOGGER.error("Failed to {}, retrying {}/{}", what, i, 10);
            releaseLeakedStreams();
        }
        TrailsAndTalesBackport.LOGGER.error("Failed to {}, aborting, progress might be lost", what);
        return false;
    }
}
