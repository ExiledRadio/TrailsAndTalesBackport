package com.exiledradio.trailsandtalesbackport.lookfeel;

import com.exiledradio.backportcore.presentation.LoadingStatus;

/**
 * The plain rules behind stream Lf's client pieces, kept free of client classes so the dedicated-server self-test
 * can check them.
 */
public final class LookRules {

    private LookRules() {
    }

    /**
     * 1.20.6 {@code VideoSettingsScreen.mouseScrolled}: one Ctrl + wheel step of GUI Scale.
     *
     * @param current the GUI scale, 0 for Auto
     * @param step    +1 (wheel up) or -1
     * @param max     the largest scale the window fits (what Auto picks)
     * @return the new scale: Auto counts as max + 1, and a step that leaves 1 to max changes nothing
     */
    public static int nextGuiScale(int current, int step, int max) {
        int from = current == 0 ? max + 1 : current;
        int to = from + step;
        return to != 0 && to <= max && to >= 1 ? to : current;
    }

    /**
     * @return a chunk status's color in 1.20's world-loading grid (23w16a), 0xRRGGBB. Core's table is 1.14's; 1.20
     *         gave carvers the old liquid carvers' blue (liquid carvers are gone) and light a pale yellow of its own
     *         (light's old gray became the new Initialize Light). Untracked cells are black.
     */
    public static int gridColor(LoadingStatus status) {
        if (status == null) {
            return LoadingStatus.UNTRACKED_COLOR;
        }
        switch (status) {
            case CARVERS:
            case LIQUID_CARVERS:
                return 0x303572;
            case LIGHT:
                return 0xFFE0A0;
            default:
                return status.color();
        }
    }
}
