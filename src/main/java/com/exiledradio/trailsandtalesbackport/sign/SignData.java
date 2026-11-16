package com.exiledradio.trailsandtalesbackport.sign;

import com.exiledradio.trailsandtalesbackport.common.Content;

import net.minecraft.item.EnumDyeColor;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * What a 1.20 sign keeps that 1.12.2's {@code TileEntitySign} has no room for, as a capability on every
 * sign tile (vanilla's, ours, hanging signs, Village & Pillage's and any other mod's that extends it).
 *
 * <h2>Storage</h2>
 * Forge saves a tile's capabilities under {@code ForgeCaps}, and a sign's update packet is its whole save,
 * so this reaches clients whenever the sign loads or changes, with no packet of ours. The tag mirrors
 * 1.20's {@code SignBlockEntity}:
 * <pre>
 *   trailsandtalesbackport:sign {
 *     front_text: {has_glowing_text}             // front lines stay in vanilla's Text1-4; front color: below
 *     back_text:  {messages: [4 x JSON], color, has_glowing_text}
 *     is_waxed
 *   }
 * </pre>
 * The front color is Village & Pillage's: its sign dyeing (1.14) keeps the color in the tile's Forge data
 * under {@code villagerbackport:Color}, which this mod's wood sign renderer already reads. Using the same
 * key means a sign dyed before keeps its color and both mods' renderers agree ({@link SignSides#color}).
 *
 * <p>{@link #editor} (1.20's {@code playerWhoMayEdit}) is not saved, as in 1.20.
 */
public final class SignData {

    public static final ResourceLocation KEY = Content.id("sign");

    @CapabilityInject(SignData.class)
    public static Capability<SignData> CAP;

    public final ITextComponent[] back = {
            new TextComponentString(""), new TextComponentString(""), new TextComponentString(""), new TextComponentString("")
    };
    public EnumDyeColor backColor = EnumDyeColor.BLACK;
    public boolean frontGlowing;
    public boolean backGlowing;
    public boolean waxed;

    /** The one player who may send this sign new text, until they do; null for nobody. */
    @Nullable
    public UUID editor;

    static void register() {
        CapabilityManager.INSTANCE.register(SignData.class, new Capability.IStorage<SignData>() {
            @Override
            public NBTBase writeNBT(Capability<SignData> capability, SignData instance, EnumFacing side) {
                return instance.write();
            }

            @Override
            public void readNBT(Capability<SignData> capability, SignData instance, EnumFacing side, NBTBase nbt) {
                if (nbt instanceof NBTTagCompound) {
                    instance.read((NBTTagCompound) nbt);
                }
            }
        }, SignData::new);
    }

    public NBTTagCompound write() {
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagCompound front = new NBTTagCompound();
        front.setBoolean("has_glowing_text", this.frontGlowing);
        tag.setTag("front_text", front);
        NBTTagCompound backTag = new NBTTagCompound();
        NBTTagList messages = new NBTTagList();
        for (ITextComponent line : this.back) {
            messages.appendTag(new NBTTagString(ITextComponent.Serializer.componentToJson(
                    line == null ? new TextComponentString("") : line)));
        }
        backTag.setTag("messages", messages);
        backTag.setString("color", SignSides.colorName(this.backColor));
        backTag.setBoolean("has_glowing_text", this.backGlowing);
        tag.setTag("back_text", backTag);
        tag.setBoolean("is_waxed", this.waxed);
        return tag;
    }

    public void read(NBTTagCompound tag) {
        this.frontGlowing = tag.getCompoundTag("front_text").getBoolean("has_glowing_text");
        NBTTagCompound backTag = tag.getCompoundTag("back_text");
        NBTTagList messages = backTag.getTagList("messages", 8);
        for (int i = 0; i < 4; i++) {
            ITextComponent line = null;
            if (i < messages.tagCount()) {
                try {
                    line = ITextComponent.Serializer.jsonToComponent(messages.getStringTagAt(i));
                } catch (RuntimeException ignored) {
                    // 1.20 reads a line it cannot parse as empty.
                }
            }
            this.back[i] = line == null ? new TextComponentString("") : line;
        }
        EnumDyeColor color = SignSides.parseColor(backTag.getString("color"));
        this.backColor = color == null ? EnumDyeColor.BLACK : color;
        this.backGlowing = backTag.getBoolean("has_glowing_text");
        this.waxed = tag.getBoolean("is_waxed");
    }

    /**
     * Selectors, scores and other entity text on the back resolved as the sign loads, as the front's are
     * (1.12.2's {@code TileEntitySign.readFromNBT}; 1.20.6's {@code SignBlockEntity.loadLines} does both faces): the
     * same sender (the sign, permission level 2), on a server world only, as 1.20.6.
     */
    void resolve(final net.minecraft.tileentity.TileEntitySign sign) {
        final net.minecraft.world.World world = sign.getWorld();
        if (world == null || world.isRemote || world.getMinecraftServer() == null) {
            return;
        }
        net.minecraft.command.ICommandSender sender = new net.minecraft.command.ICommandSender() {
            @Override
            public String getName() {
                return "Sign";
            }

            @Override
            public boolean canUseCommand(int permLevel, String commandName) {
                return permLevel <= 2;
            }

            @Override
            public net.minecraft.util.math.BlockPos getPosition() {
                return sign.getPos();
            }

            @Override
            public net.minecraft.util.math.Vec3d getPositionVector() {
                return new net.minecraft.util.math.Vec3d(sign.getPos()).add(0.5D, 0.5D, 0.5D);
            }

            @Override
            public net.minecraft.world.World getEntityWorld() {
                return world;
            }

            @Override
            public net.minecraft.server.MinecraftServer getServer() {
                return world.getMinecraftServer();
            }
        };
        for (int i = 0; i < 4; i++) {
            try {
                this.back[i] = net.minecraft.util.text.TextComponentUtils.processComponent(sender, this.back[i], null);
            } catch (net.minecraft.command.CommandException e) {
                // As vanilla: the line stays as written.
            }
        }
        resolvedLoads++;
    }

    /** For the self-test: how many loads resolved a back face. */
    public static volatile int resolvedLoads;

    /** Attached to each sign tile by {@link SignsContent#attach}. */
    static final class Provider implements ICapabilitySerializable<NBTTagCompound> {
        final SignData data = new SignData();
        @Nullable
        private final net.minecraft.tileentity.TileEntitySign sign;

        Provider(@Nullable net.minecraft.tileentity.TileEntitySign sign) {
            this.sign = sign;
        }

        @Override
        public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
            return capability == CAP;
        }

        @Nullable
        @Override
        public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
            return capability == CAP ? CAP.cast(this.data) : null;
        }

        @Override
        public NBTTagCompound serializeNBT() {
            return this.data.write();
        }

        @Override
        public void deserializeNBT(NBTTagCompound nbt) {
            this.data.read(nbt);
            if (this.sign != null) {
                this.data.resolve(this.sign);
            }
        }
    }
}
