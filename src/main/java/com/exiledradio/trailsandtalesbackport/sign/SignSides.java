package com.exiledradio.trailsandtalesbackport.sign;

import net.minecraft.block.Block;
import net.minecraft.block.BlockStandingSign;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;

import javax.annotation.Nullable;

/**
 * Both faces of a sign, 1.20's {@code SignText} pair, read and changed through one place.
 *
 * <p>The front's lines are vanilla's {@code signText}; its color is Village & Pillage's tag (see
 * {@link SignData}); everything else is in {@link SignData}. With this stream off (no capability) a sign
 * has only its vanilla front, black and not glowing.
 */
public final class SignSides {

    /** Village & Pillage's front color tag (its {@code flora.SignColor}), in the tile's Forge data. */
    public static final String FRONT_COLOR_KEY = "villagerbackport:Color";

    /** 1.20's {@code DyeColor.getTextColor()}, by 1.12.2 metadata (white first). */
    private static final int[] TEXT_COLORS = {
            0xFFFFFF, 0xFF681F, 0xFF00FF, 0x9AC0CD,
            0xFFFF00, 0xBFFF00, 0xFF69B4, 0x808080,
            0xD3D3D3, 0x00FFFF, 0xA020F0, 0x0000FF,
            0x8B4513, 0x00FF00, 0xFF0000, 0x000000,
    };

    /** 1.20's {@code SignBlockEntity.playerIsTooFarAwayToEdit}: farther than 8 blocks from the sign's center. */
    public static final double EDIT_DISTANCE_SQ = 64.0D;

    private SignSides() {
    }

    @Nullable
    public static SignData data(TileEntity tile) {
        return SignData.CAP == null || tile == null || !tile.hasCapability(SignData.CAP, null)
                ? null : tile.getCapability(SignData.CAP, null);
    }

    /** @return the four lines of one face; the front's are the tile's own array, the back's the data's */
    public static ITextComponent[] lines(TileEntitySign sign, boolean front) {
        SignData d = data(sign);
        if (front || d == null) {
            return front ? sign.signText : new ITextComponent[] {
                    new TextComponentString(""), new TextComponentString(""), new TextComponentString(""), new TextComponentString("")};
        }
        return d.back;
    }

    public static EnumDyeColor color(TileEntitySign sign, boolean front) {
        if (front) {
            EnumDyeColor c = parseColor(sign.getTileData().getString(FRONT_COLOR_KEY));
            return c == null ? EnumDyeColor.BLACK : c;
        }
        SignData d = data(sign);
        return d == null ? EnumDyeColor.BLACK : d.backColor;
    }

    /** @return true if the color changed */
    public static boolean setColor(TileEntitySign sign, boolean front, EnumDyeColor color) {
        if (color(sign, front) == color) {
            return false;
        }
        if (front) {
            NBTTagCompound data = sign.getTileData();
            if (color == EnumDyeColor.BLACK) {
                data.removeTag(FRONT_COLOR_KEY);
            } else {
                data.setString(FRONT_COLOR_KEY, colorName(color));
            }
            return true;
        }
        SignData d = data(sign);
        if (d == null) {
            return false;
        }
        d.backColor = color;
        return true;
    }

    public static boolean glowing(TileEntitySign sign, boolean front) {
        SignData d = data(sign);
        return d != null && (front ? d.frontGlowing : d.backGlowing);
    }

    /** @return true if it changed */
    public static boolean setGlowing(TileEntitySign sign, boolean front, boolean glowing) {
        SignData d = data(sign);
        if (d == null || glowing(sign, front) == glowing) {
            return false;
        }
        if (front) {
            d.frontGlowing = glowing;
        } else {
            d.backGlowing = glowing;
        }
        return true;
    }

    public static boolean waxed(TileEntitySign sign) {
        SignData d = data(sign);
        return d != null && d.waxed;
    }

    /** 1.20's {@code setWaxed}: @return true if it changed */
    public static boolean setWaxed(TileEntitySign sign, boolean waxed) {
        SignData d = data(sign);
        if (d == null || d.waxed == waxed) {
            return false;
        }
        d.waxed = waxed;
        return true;
    }

    /** 1.20's {@code SignText.hasMessage}: some line of the face has text. */
    public static boolean hasMessage(TileEntitySign sign, boolean front) {
        for (ITextComponent line : lines(sign, front)) {
            if (line != null && !line.getUnformattedText().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 1.20's {@code SignBlock.hasEditableText}: every line is plain text (1.20: literal contents), so a sign
     * made with translated or score text by commands is never opened for editing.
     */
    public static boolean editableText(TileEntitySign sign, boolean front) {
        for (ITextComponent line : lines(sign, front)) {
            if (line != null && !(line instanceof TextComponentString)) {
                return false;
            }
        }
        return true;
    }

    /** 1.20's {@code SignBlockEntity.isFacingFrontText}: the player is within 90 degrees of the front's facing. */
    public static boolean isFacingFront(TileEntitySign sign, EntityPlayer player) {
        IBlockState state = sign.getWorld().getBlockState(sign.getPos());
        double cx = 0.5D;
        double cz = 0.5D;
        float facing = yRotation(state, sign);
        if (!isStanding(state.getBlock()) && !isCeilingHanging(state.getBlock())) {
            // 1.20's WallSignBlock / WallHangingSignBlock: the center of the outline.
            AxisAlignedBB box = state.getBoundingBox(sign.getWorld(), sign.getPos());
            cx = (box.minX + box.maxX) / 2.0D;
            cz = (box.minZ + box.maxZ) / 2.0D;
        }
        double dx = player.posX - (sign.getPos().getX() + cx);
        double dz = player.posZ - (sign.getPos().getZ() + cz);
        float toPlayer = (float) (MathHelper.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
        return Math.abs(MathHelper.wrapDegrees(facing - toPlayer)) <= 90.0F;
    }

    /** The front face's yaw, 1.20's {@code getYRotationDegrees}: south 0, west 90, north 180, east 270. */
    public static float yRotation(IBlockState state, TileEntitySign sign) {
        Block block = state.getBlock();
        if (isStanding(block) || isCeilingHanging(block)) {
            return sign.getBlockMetadata() * 22.5F;
        }
        for (net.minecraft.block.properties.IProperty<?> p : state.getPropertyKeys()) {
            if ("facing".equals(p.getName()) && state.getValue(p) instanceof EnumFacing) {
                return ((EnumFacing) state.getValue(p)).getHorizontalAngle();
            }
        }
        return EnumFacing.byIndex(sign.getBlockMetadata()).getHorizontalAngle();
    }

    static boolean isStanding(Block block) {
        return block instanceof BlockStandingSign;
    }

    /** A ceiling hanging sign of ours: rotation in metadata like a standing sign. Found by name, not class. */
    static boolean isCeilingHanging(Block block) {
        String path = block.getRegistryName() == null ? "" : block.getRegistryName().getPath();
        return path.endsWith("_hanging_sign") && !path.endsWith("_wall_hanging_sign");
    }

    /** @return true for a hanging sign of either kind (1.20's smaller text area and its own edit screen) */
    public static boolean isHanging(Block block) {
        String path = block.getRegistryName() == null ? "" : block.getRegistryName().getPath();
        return path.endsWith("_hanging_sign");
    }

    /** 1.20's text color of a dye, full strength. */
    public static int textColor(EnumDyeColor color) {
        return TEXT_COLORS[color.getMetadata()];
    }

    /** 1.20's {@code SignRenderer.getDarkColor}: 40% of the text color; black glowing text: 0xF0EBCC. */
    public static int darkColor(EnumDyeColor color, boolean glowing) {
        int rgb = textColor(color);
        if (color == EnumDyeColor.BLACK && glowing) {
            return 0xF0EBCC;
        }
        int r = (int) ((rgb >> 16 & 255) * 0.4D);
        int g = (int) ((rgb >> 8 & 255) * 0.4D);
        int b = (int) ((rgb & 255) * 0.4D);
        return r << 16 | g << 8 | b;
    }

    /** 1.14+ names: 1.12.2's "silver" is "light_gray". */
    public static String colorName(EnumDyeColor color) {
        return color == EnumDyeColor.SILVER ? "light_gray" : color.getName();
    }

    @Nullable
    public static EnumDyeColor parseColor(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        if ("light_gray".equals(name)) {
            return EnumDyeColor.SILVER;
        }
        for (EnumDyeColor color : EnumDyeColor.values()) {
            if (color.getName().equals(name)) {
                return color;
            }
        }
        return null;
    }
}
