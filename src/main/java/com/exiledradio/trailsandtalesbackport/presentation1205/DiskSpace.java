package com.exiledradio.trailsandtalesbackport.presentation1205;

import javax.annotation.Nullable;
import java.io.File;
import java.nio.file.Files;

/** 1.20.6's {@code LevelStorageAccess.checkForLowDiskSpace}: under 64 MB usable on the drive of a world's folder. */
public final class DiskSpace {

    /** 1.20.6's {@code LevelStorageSource.DISK_SPACE_WARNING_THRESHOLD}. */
    public static final long WARNING_THRESHOLD = 67108864L;

    /** For the self-tests: the usable space every check sees, or null for the drive's own. */
    @Nullable
    public static volatile Long testUsableSpace;

    private DiskSpace() {
    }

    /** @return the usable bytes on the folder's drive; Long.MAX_VALUE when it cannot be read, as 1.20.6 */
    public static long estimate(File folder) {
        Long test = testUsableSpace;
        if (test != null) {
            return test;
        }
        try {
            return Files.getFileStore(folder.toPath()).getUsableSpace();
        } catch (Exception e) {
            return Long.MAX_VALUE;
        }
    }

    public static boolean isLow(File folder) {
        return estimate(folder) < WARNING_THRESHOLD;
    }
}
