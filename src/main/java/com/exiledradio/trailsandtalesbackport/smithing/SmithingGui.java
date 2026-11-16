package com.exiledradio.trailsandtalesbackport.smithing;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

import javax.annotation.Nullable;

/**
 * This mod's GUI handler (the only one: Forge keeps one per mod). Id 0 is the smithing table; another stream that
 * needs a screen adds its own id here.
 */
public final class SmithingGui implements IGuiHandler {

    public static final int SMITHING = 0;

    @Nullable
    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        return id == SMITHING ? new ContainerSmithing(player.inventory, world, new BlockPos(x, y, z)) : null;
    }

    @Nullable
    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        return id == SMITHING ? com.exiledradio.trailsandtalesbackport.smithing.client.SmithingClient.screen(
                new ContainerSmithing(player.inventory, world, new BlockPos(x, y, z))) : null;
    }
}
