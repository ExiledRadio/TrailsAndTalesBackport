package com.exiledradio.trailsandtalesbackport.sign.client;

import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import com.exiledradio.trailsandtalesbackport.network.NetworkHandler;
import com.exiledradio.trailsandtalesbackport.sign.SignNetwork;
import com.exiledradio.trailsandtalesbackport.sign.SignSides;

import net.minecraft.block.Block;
import net.minecraft.block.BlockStandingSign;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.model.ModelSign;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.ChatAllowedCharacters;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import org.lwjgl.input.Keyboard;

/**
 * 1.20's sign edit screens ({@code SignEditScreen} and {@code HangingSignEditScreen}, through
 * {@code AbstractSignEditScreen}), for one face of a sign.
 *
 * <ul>
 *   <li>Title "Edit Sign Message" (hanging: "Edit Hanging Sign Message") at y 40; Done at
 *       {@code height / 4 + 144}, 200 wide.</li>
 *   <li>A sign: its board (and stick, if standing) at 62.5x, centered at x, y 90 (+31 for the model); text
 *       at 0.977x, lines 10 apart, 90 wide. A hanging sign: its 16x16 {@code gui/hanging_signs/<wood>} at
 *       4.5x, centered 13 above y 125; text at 1x, lines 9 apart, 60 wide.</li>
 *   <li>The text in the face's color (full color when it glows), a blinking cursor - "_" at the end of a
 *       line, else a bar - and the selection; typing replaces the selection, a character goes in only while
 *       the line still fits its width.</li>
 *   <li>Keys: up and down (and Enter) change line; left, right, Home and End move (Shift selects); Backspace
 *       and Delete; Ctrl+A, C, X, V. Escape or Done closes.</li>
 *   <li>Each change shows on the sign in the world at once; on closing the face's lines go to the server
 *       (packet 30). The screen closes by itself if the sign goes or the player moves more than 8 blocks
 *       away.</li>
 * </ul>
 */
public class SignEditScreen extends GuiScreen {

    private static final ModelSign MODEL = new ModelSign();

    private final TileEntitySign sign;
    private final boolean front;
    private final boolean hanging;
    /** 26.2's flat texture for this sign, looked up once per screen ({@link #flatTexture}). */
    private ResourceLocation flat;
    private boolean flatChecked;
    private final int lineHeight;
    private final int maxWidth;
    private final String[] messages = new String[4];
    private int line;
    private int cursor;
    private int selection;
    private int frame;
    private boolean sent;

    public SignEditScreen(TileEntitySign sign, boolean front) {
        this.sign = sign;
        this.front = front;
        Block block = sign.getBlockType();
        this.hanging = block != null && SignSides.isHanging(block);
        this.lineHeight = this.hanging ? 9 : 10;
        this.maxWidth = this.hanging ? 60 : 90;
        ITextComponent[] lines = SignSides.lines(sign, front);
        for (int i = 0; i < 4; i++) {
            this.messages[i] = i < lines.length && lines[i] != null ? lines[i].getUnformattedText() : "";
        }
        this.cursor = this.selection = this.messages[0].length();
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        Keyboard.enableRepeatEvents(true);
        this.addButton(new GuiButton(0, this.width / 2 - 100, this.height / 4 + 144, 200, 20, I18n.format("gui.done")));
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        if (!this.sent && NetworkHandler.channel != null && this.mc.getConnection() != null) {
            this.sent = true;
            NetworkHandler.channel.sendToServer(new SignNetwork.Update(this.sign.getPos(), this.front, this.messages));
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void updateScreen() {
        this.frame++;
        if (!valid()) {
            this.mc.displayGuiScreen(null);
        }
    }

    /** 1.20's {@code isValid}: the sign is still there and the player within 8 blocks of it. */
    private boolean valid() {
        return this.mc.player != null && this.mc.world != null && !this.sign.isInvalid()
                && this.mc.world.getTileEntity(this.sign.getPos()) == this.sign
                && this.mc.player.getDistanceSqToCenter(this.sign.getPos()) <= SignSides.EDIT_DISTANCE_SQ;
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.enabled && button.id == 0) {
            this.mc.displayGuiScreen(null);
        }
    }

    // --- typing (1.20's TextFieldHelper) --------------------------------------------------------

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            this.mc.displayGuiScreen(null);
            return;
        }
        if (keyCode == Keyboard.KEY_UP) {
            changeLine(-1);
            return;
        }
        if (keyCode == Keyboard.KEY_DOWN || keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
            changeLine(1);
            return;
        }
        String s = this.messages[this.line];
        boolean shift = isShiftKeyDown();
        if (isKeyComboCtrlA(keyCode)) {
            this.cursor = s.length();
            this.selection = 0;
            return;
        }
        if (isKeyComboCtrlC(keyCode)) {
            setClipboardString(selected());
            return;
        }
        if (isKeyComboCtrlX(keyCode)) {
            setClipboardString(selected());
            insert("");
            return;
        }
        if (isKeyComboCtrlV(keyCode)) {
            insert(ChatAllowedCharacters.filterAllowedCharacters(getClipboardString()));
            return;
        }
        switch (keyCode) {
            case Keyboard.KEY_BACK:
                if (this.cursor != this.selection) {
                    insert("");
                } else if (this.cursor > 0) {
                    set(s.substring(0, this.cursor - 1) + s.substring(this.cursor));
                    this.cursor = this.selection = this.cursor - 1;
                }
                return;
            case Keyboard.KEY_DELETE:
                if (this.cursor != this.selection) {
                    insert("");
                } else if (this.cursor < s.length()) {
                    set(s.substring(0, this.cursor) + s.substring(this.cursor + 1));
                }
                return;
            case Keyboard.KEY_LEFT:
                move(Math.max(0, this.cursor - 1), shift);
                return;
            case Keyboard.KEY_RIGHT:
                move(Math.min(s.length(), this.cursor + 1), shift);
                return;
            case Keyboard.KEY_HOME:
                move(0, shift);
                return;
            case Keyboard.KEY_END:
                move(s.length(), shift);
                return;
            default:
                break;
        }
        if (ChatAllowedCharacters.isAllowedCharacter(typedChar)) {
            insert(Character.toString(typedChar));
        }
    }

    private void changeLine(int by) {
        this.line = this.line + by & 3;
        this.cursor = this.selection = this.messages[this.line].length();
    }

    private void move(int to, boolean shift) {
        this.cursor = to;
        if (!shift) {
            this.selection = to;
        }
    }

    private String selected() {
        String s = this.messages[this.line];
        return s.substring(Math.min(this.cursor, this.selection), Math.max(this.cursor, this.selection));
    }

    /** Replaces the selection; kept only if the line still fits ({@code font.width(s) <= maxWidth}). */
    private void insert(String text) {
        String s = this.messages[this.line];
        int from = Math.min(this.cursor, this.selection);
        int to = Math.max(this.cursor, this.selection);
        String next = s.substring(0, from) + text + s.substring(to);
        if (this.fontRenderer.getStringWidth(next) <= this.maxWidth) {
            set(next);
            this.cursor = this.selection = from + text.length();
        }
    }

    /** The line, here and on the sign in the world (1.20 shows the change at once). */
    private void set(String s) {
        this.messages[this.line] = s;
        ITextComponent[] lines = SignSides.lines(this.sign, this.front);
        ITextComponent old = lines[this.line];
        lines[this.line] = new TextComponentString(s).setStyle(old == null ? new net.minecraft.util.text.Style()
                : old.getStyle().createShallowCopy());
    }

    // --- drawing ---------------------------------------------------------------------------------

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        RenderHelper.disableStandardItemLighting();
        drawDefaultBackground();
        String title = I18n.format(this.hanging ? "gui." + TrailsAndTalesBackport.MOD_ID + ".hanging_sign.edit"
                : "gui." + TrailsAndTalesBackport.MOD_ID + ".sign.edit");
        drawCenteredString(this.fontRenderer, title, this.width / 2, 40, 0xFFFFFF);
        renderSign();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void renderSign() {
        GlStateManager.pushMatrix();
        GlStateManager.translate(this.width / 2.0F, this.hanging ? 125.0F : 90.0F, 50.0F);
        GlStateManager.enableDepth();
        GlStateManager.disableLighting();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.pushMatrix();
        if (!this.flatChecked) {
            this.flat = this.hanging ? null : flatTexture(this.sign.getBlockType());
            this.flatChecked = true;
        }
        ResourceLocation flat;
        if (this.hanging) {
            GlStateManager.translate(0.0F, -13.0F, 0.0F);
            GlStateManager.scale(4.5F, 4.5F, 1.0F);
            this.mc.getTextureManager().bindTexture(hangingTexture(this.sign.getBlockType()));
            drawModalRectWithCustomSizedTexture(-8, -8, 0.0F, 0.0F, 16, 16, 16.0F, 16.0F);
        } else if ((flat = this.flat) != null) {
            // 26.2: the sign drawn flat from gui/signs/<wood> at 3.9x, 27 below the text's center (12 high on a wall).
            GlStateManager.translate(0.0F, 27.0F, 0.0F);
            GlStateManager.scale(3.9F, 3.9F, 1.0F);
            this.mc.getTextureManager().bindTexture(flat);
            int shown = this.sign.getBlockType() instanceof BlockStandingSign ? 26 : 12;
            drawModalRectWithCustomSizedTexture(-12, -13, 0.0F, 0.0F, 24, shown, 24.0F, 26.0F);
        } else {
            GlStateManager.translate(0.0F, 31.0F, 0.0F);
            GlStateManager.scale(62.500004F, 62.500004F, -62.500004F);
            this.mc.getTextureManager().bindTexture(StandardSignRenderer.texture(this.sign.getBlockType()));
            MODEL.signStick.showModel = this.sign.getBlockType() instanceof BlockStandingSign;
            GlStateManager.disableCull();
            MODEL.renderSign();
            GlStateManager.enableCull();
        }
        GlStateManager.popMatrix();
        renderText();
        GlStateManager.popMatrix();
    }

    /** 1.20's {@code AbstractSignEditScreen.renderSignText}. */
    private void renderText() {
        GlStateManager.translate(0.0F, 0.0F, 4.0F);
        float scale = this.hanging ? 1.0F : 0.9765628F;
        GlStateManager.scale(scale, scale, scale);
        EnumDyeColor color = SignSides.color(this.sign, this.front);
        boolean glowing = SignSides.glowing(this.sign, this.front);
        int textColor = glowing ? SignSides.textColor(color) : SignSides.darkColor(color, false);
        boolean blink = this.frame / 6 % 2 == 0;
        int top = 4 * this.lineHeight / 2;
        int lineY = this.line * this.lineHeight - top;
        for (int i = 0; i < 4; i++) {
            String s = this.messages[i];
            this.fontRenderer.drawString(s, -this.fontRenderer.getStringWidth(s) / 2, i * this.lineHeight - top, textColor);
            if (i == this.line && blink && this.cursor >= s.length()) {
                int at = this.fontRenderer.getStringWidth(s) - this.fontRenderer.getStringWidth(s) / 2;
                this.fontRenderer.drawString("_", at, lineY, textColor);
            }
        }
        String s = this.messages[this.line];
        int half = this.fontRenderer.getStringWidth(s) / 2;
        int cursorX = this.fontRenderer.getStringWidth(s.substring(0, Math.min(this.cursor, s.length()))) - half;
        if (blink && this.cursor < s.length()) {
            Gui.drawRect(cursorX, lineY - 1, cursorX + 1, lineY + this.lineHeight, 0xFF000000 | textColor);
        }
        if (this.selection != this.cursor) {
            int a = this.fontRenderer.getStringWidth(s.substring(0, Math.min(this.cursor, this.selection))) - half;
            int b = this.fontRenderer.getStringWidth(s.substring(0, Math.max(this.cursor, this.selection))) - half;
            highlight(Math.min(a, b), lineY, Math.max(a, b), lineY + this.lineHeight);
        }
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /** The text selection, drawn as 1.12.2's text fields draw theirs (blue, OR_REVERSE). */
    private static void highlight(int left, int top, int right, int bottom) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        GlStateManager.color(0.0F, 0.0F, 255.0F, 255.0F);
        GlStateManager.disableTexture2D();
        GlStateManager.enableColorLogic();
        GlStateManager.colorLogicOp(GlStateManager.LogicOp.OR_REVERSE);
        buffer.begin(7, DefaultVertexFormats.POSITION);
        buffer.pos(left, bottom, 0.0D).endVertex();
        buffer.pos(right, bottom, 0.0D).endVertex();
        buffer.pos(right, top, 0.0D).endVertex();
        buffer.pos(left, top, 0.0D).endVertex();
        tessellator.draw();
        GlStateManager.disableColorLogic();
        GlStateManager.enableTexture2D();
    }

    /**
     * 26.2's flat sign texture for a standing or wall sign, {@code <namespace>:textures/gui/signs/<wood>.png} from the
     * sign block's name ({@code trailsandtalesbackport:cherry_wall_sign} gives cherry, Game Drops Backport's
     * {@code poplar_sign} poplar, 1.12.2's own sign {@code minecraft:} oak), when the newest installed version is 26.2
     * or later and a mod or resource pack has the file, else vanilla's {@code textures/gui/signs/<wood>.png} (the
     * newest backport's copy: Game Drops Backport supplies 26.3's twelve vanilla woods, oak among them); else null, and
     * the sign is drawn as 1.20's model.
     */
    @javax.annotation.Nullable
    static ResourceLocation flatTexture(Block block) {
        if (!com.exiledradio.backportcore.presentation.Presentation.isAtLeast(com.exiledradio.backportcore.presentation.LookArea.MENUS, "26.2")
                || block.getRegistryName() == null) {
            return null;
        }
        String ns = block.getRegistryName().getNamespace();
        String path = block.getRegistryName().getPath();
        String wood = "minecraft".equals(ns) ? "oak"
                : path.endsWith("_wall_sign") ? path.substring(0, path.length() - "_wall_sign".length())
                : path.endsWith("_sign") ? path.substring(0, path.length() - "_sign".length()) : null;
        if (wood == null) {
            return null;
        }
        ResourceLocation own = new ResourceLocation(ns, "textures/gui/signs/" + wood + ".png");
        if (exists(own)) {
            return own;
        }
        // A vanilla wood's sign from another mod (Update Aquatic's spruce sign): vanilla's own texture, which the newest
        // backport supplies (Game Drops Backport's 26.3 copies).
        ResourceLocation vanilla = new ResourceLocation("textures/gui/signs/" + wood + ".png");
        return exists(vanilla) ? vanilla : null;
    }

    private static boolean exists(ResourceLocation texture) {
        try {
            net.minecraft.client.Minecraft.getMinecraft().getResourceManager().getResource(texture).close();
            return true;
        } catch (java.io.IOException | RuntimeException e) {
            return false;
        }
    }

    /** {@code gui/hanging_signs/<wood>.png}, the wood from the block's name ({@code oak_hanging_sign}). */
    static ResourceLocation hangingTexture(Block block) {
        String path = block.getRegistryName() == null ? "oak_hanging_sign" : block.getRegistryName().getPath();
        String wood = path.endsWith("_wall_hanging_sign") ? path.substring(0, path.length() - "_wall_hanging_sign".length())
                : path.endsWith("_hanging_sign") ? path.substring(0, path.length() - "_hanging_sign".length()) : "oak";
        return new ResourceLocation(TrailsAndTalesBackport.MOD_ID, "textures/gui/hanging_signs/" + wood + ".png");
    }
}
