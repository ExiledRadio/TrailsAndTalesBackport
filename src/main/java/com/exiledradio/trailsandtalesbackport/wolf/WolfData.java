package com.exiledradio.trailsandtalesbackport.wolf;

import com.exiledradio.trailsandtalesbackport.common.Content;

import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;

import javax.annotation.Nullable;

/**
 * What 1.20.5 adds to a wolf, kept beside 1.12.2's: its variant, its body armor and whether its health was raised
 * to 1.20.5's tamed 40.
 *
 * <p>Saved under {@code ForgeCaps/trailsandtalesbackport:wolf} as {@code variant} (1.20's id, such as
 * {@code minecraft:ashen}), {@code body_armor} (the item) and {@code health40}. A wolf saved before this mod has
 * none of it: its variant stays unset until it joins the world, where {@link WolfContent} makes it pale.
 */
public final class WolfData {

    public static final ResourceLocation KEY = Content.id("wolf");

    @CapabilityInject(WolfData.class)
    public static Capability<WolfData> CAPABILITY;

    /** Null until decided (a new wolf by its biome, an old one pale). */
    @Nullable
    public WolfVariant variant;
    public ItemStack armor = ItemStack.EMPTY;
    /** Set once a tamed wolf's health has been doubled to 1.20.5's scale, so it is never doubled again. */
    public boolean health40;

    static void register() {
        CapabilityManager.INSTANCE.register(WolfData.class, new Capability.IStorage<WolfData>() {
            @Override
            public NBTBase writeNBT(Capability<WolfData> capability, WolfData instance, EnumFacing side) {
                return instance.write();
            }

            @Override
            public void readNBT(Capability<WolfData> capability, WolfData instance, EnumFacing side, NBTBase nbt) {
                if (nbt instanceof NBTTagCompound) {
                    instance.read((NBTTagCompound) nbt);
                }
            }
        }, WolfData::new);
    }

    /** @return the wolf's data; never null while the stream is on (every wolf gets it) */
    @Nullable
    public static WolfData of(EntityWolf wolf) {
        return CAPABILITY == null ? null : wolf.getCapability(CAPABILITY, null);
    }

    public WolfVariant variantOrPale() {
        return this.variant == null ? WolfVariant.PALE : this.variant;
    }

    public boolean hasArmor() {
        return !this.armor.isEmpty();
    }

    NBTTagCompound write() {
        NBTTagCompound tag = new NBTTagCompound();
        if (this.variant != null) {
            tag.setString("variant", this.variant.id());
        }
        if (!this.armor.isEmpty()) {
            tag.setTag("body_armor", this.armor.writeToNBT(new NBTTagCompound()));
        }
        if (this.health40) {
            tag.setBoolean("health40", true);
        }
        return tag;
    }

    void read(NBTTagCompound tag) {
        if (tag.hasKey("variant", 8)) {
            WolfVariant v = WolfVariant.byId(tag.getString("variant"));
            this.variant = v == null ? WolfVariant.PALE : v;
        }
        this.armor = tag.hasKey("body_armor", 10) ? new ItemStack(tag.getCompoundTag("body_armor")) : ItemStack.EMPTY;
        this.health40 = tag.getBoolean("health40");
    }

    /** Attaches the data to a wolf. */
    static final class Provider implements ICapabilitySerializable<NBTTagCompound> {
        private final WolfData data = new WolfData();

        @Override
        public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
            return capability == CAPABILITY;
        }

        @Override
        @Nullable
        public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
            return capability == CAPABILITY ? CAPABILITY.cast(this.data) : null;
        }

        @Override
        public NBTTagCompound serializeNBT() {
            return this.data.write();
        }

        @Override
        public void deserializeNBT(NBTTagCompound nbt) {
            this.data.read(nbt);
        }
    }
}
