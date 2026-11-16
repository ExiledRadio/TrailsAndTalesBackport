package com.exiledradio.trailsandtalesbackport.presentation1205.client;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.List;

/** 1.20.6's ConfirmScreen, AlertScreen and GenericMessageScreen, laid out as 1.20.6 lays them out. */
@SideOnly(Side.CLIENT)
public final class Screens1206 {

    /** 1.20.6's {@code MultiLineLabel} and font line height. */
    static final int LINE = 9;

    private Screens1206() {
    }

    /** I18n text with the language file's {@code \n} turned into line breaks (1.12.2's files keep it literal). */
    public static String text(String key, Object... args) {
        return I18n.format(key, args).replace("\\n", "\n");
    }

    public interface Callback {
        void accept(boolean yes);
    }

    static List<String> wrap(GuiScreen screen, String text, int width) {
        return new ArrayList<String>(screen.mc.fontRenderer.listFormattedStringToWidth(text, width));
    }

    static void drawCentered(GuiScreen screen, List<String> lines, int y) {
        for (int i = 0; i < lines.size(); i++) {
            screen.drawCenteredString(screen.mc.fontRenderer, lines.get(i), screen.width / 2, y + i * LINE, 0xFFFFFF);
        }
    }

    /** 1.20.6's {@code ConfirmScreen}: title, message and two buttons; Escape answers no. */
    public static class Confirm extends GuiScreen {
        public final String title;
        public final String message;
        final String yes;
        final String no;
        final Callback callback;
        private List<String> lines = new ArrayList<String>();

        public Confirm(Callback callback, String title, String message, String yes, String no) {
            this.callback = callback;
            this.title = title;
            this.message = message;
            this.yes = yes;
            this.no = no;
        }

        @Override
        public void initGui() {
            this.buttonList.clear();
            this.lines = wrap(this, this.message, this.width - 50);
            int y = MathHelper.clamp(messageTop() + messageHeight() + 20, this.height / 6 + 96, this.height - 24);
            this.buttonList.add(new GuiButton(0, this.width / 2 - 155, y, 150, 20, this.yes));
            this.buttonList.add(new GuiButton(1, this.width / 2 - 155 + 160, y, 150, 20, this.no));
        }

        private int titleTop() {
            return MathHelper.clamp((this.height - messageHeight()) / 2 - 20 - LINE, 10, 80);
        }

        private int messageTop() {
            return titleTop() + 20;
        }

        private int messageHeight() {
            return this.lines.size() * LINE;
        }

        @Override
        protected void actionPerformed(GuiButton button) {
            this.callback.accept(button.id == 0);
        }

        @Override
        protected void keyTyped(char typedChar, int keyCode) {
            if (keyCode == Keyboard.KEY_ESCAPE) {
                this.callback.accept(false);
            }
        }

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            this.drawDefaultBackground();
            super.drawScreen(mouseX, mouseY, partialTicks);
            this.drawCenteredString(this.fontRenderer, this.title, this.width / 2, titleTop(), 0xFFFFFF);
            drawCentered(this, this.lines, messageTop());
        }

        /** For the self-tests: presses yes or no. */
        public void answer(boolean yes) {
            this.callback.accept(yes);
        }
    }

    /** 1.20.6's {@code AlertScreen}: title at 70, message at 90, one button (Back); Escape closes the screen. */
    public static class Alert extends GuiScreen {
        public final String title;
        public final String message;
        final Runnable callback;
        private List<String> lines = new ArrayList<String>();

        public Alert(Runnable callback, String title, String message) {
            this.callback = callback;
            this.title = title;
            this.message = message;
        }

        @Override
        public void initGui() {
            this.buttonList.clear();
            this.lines = wrap(this, this.message, this.width - 50);
            int y = MathHelper.clamp(90 + this.lines.size() * LINE + 12, this.height / 6 + 96, this.height - 24);
            this.buttonList.add(new GuiButton(0, (this.width - 150) / 2, y, 150, 20, I18n.format("gui.back")));
        }

        @Override
        protected void actionPerformed(GuiButton button) {
            this.callback.run();
        }

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            this.drawDefaultBackground();
            super.drawScreen(mouseX, mouseY, partialTicks);
            this.drawCenteredString(this.fontRenderer, this.title, this.width / 2, 70, 0xFFFFFF);
            drawCentered(this, this.lines, 90);
        }

        public void ok() {
            this.callback.run();
        }
    }

    /**
     * 1.20.6's {@code GenericMessageScreen} (one centered line), shown for a frame before slow work, as 1.20.6's
     * {@code forceSetScreen} draws it: the work runs on the tick after the screen was first drawn.
     */
    public static class Message extends GuiScreen {
        public final String title;
        private Runnable then;
        private boolean drawn;

        public Message(String title, Runnable then) {
            this.title = title;
            this.then = then;
        }

        @Override
        public void updateScreen() {
            if (this.drawn && this.then != null) {
                Runnable r = this.then;
                this.then = null;
                r.run();
            }
        }

        @Override
        protected void keyTyped(char typedChar, int keyCode) {
            // shouldCloseOnEsc false
        }

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            this.drawDefaultBackground();
            this.drawCenteredString(this.fontRenderer, this.title, this.width / 2, this.height / 2 - LINE / 2, 0xFFFFFF);
            super.drawScreen(mouseX, mouseY, partialTicks);
            this.drawn = true;
        }
    }
}
