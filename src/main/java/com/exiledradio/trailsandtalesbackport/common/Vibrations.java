package com.exiledradio.trailsandtalesbackport.common;

import com.exiledradio.backportcore.gameevent.GameEvent;
import com.exiledradio.backportcore.gameevent.GameEventContext;
import com.exiledradio.backportcore.gameevent.GameEvents;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * 1.20's {@code gameEvent} calls, on Backport Core's shared game events: what this mod's blocks and mobs post,
 * heard by every backport's sculk sensors, wardens and allays. Nothing happens unless some backport has game
 * events on.
 */
public final class Vibrations {

    private Vibrations() {
    }

    /** {@code Entity.gameEvent(event)}: at the entity, the entity as the source. */
    public static void entity(Entity entity, GameEvent event) {
        entity(entity, event, entity);
    }

    /** {@code Entity.gameEvent(event, source)}: at the entity, another source. */
    public static void entity(Entity entity, GameEvent event, @Nullable Entity source) {
        if (!entity.world.isRemote) {
            GameEvents.emit(entity.world, event, new Vec3d(entity.posX, entity.posY, entity.posZ), GameEventContext.of(source));
        }
    }

    /** {@code Level.gameEvent(source, event, pos)}: at the block's center, no block state. */
    public static void block(World world, GameEvent event, BlockPos pos, @Nullable Entity source) {
        if (!world.isRemote) {
            GameEvents.emit(world, event, pos, GameEventContext.of(source));
        }
    }

    /** {@code Level.gameEvent(event, pos, Context.of(source, state))}. */
    public static void block(World world, GameEvent event, BlockPos pos, @Nullable Entity source, @Nullable IBlockState state) {
        if (!world.isRemote) {
            GameEvents.emit(world, event, pos, GameEventContext.of(source, state));
        }
    }
}
