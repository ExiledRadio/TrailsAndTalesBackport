package com.exiledradio.trailsandtalesbackport.selftest;

import com.exiledradio.trailsandtalesbackport.common.Content;
import com.exiledradio.trailsandtalesbackport.heads.BlockPiglinHead;
import com.exiledradio.trailsandtalesbackport.heads.HeadsContent;
import com.exiledradio.trailsandtalesbackport.heads.ItemPiglinHead;
import com.exiledradio.trailsandtalesbackport.heads.NoteBlockHeads;
import com.exiledradio.trailsandtalesbackport.heads.TilePiglinHead;

import net.minecraft.block.BlockDispenser;
import net.minecraft.block.BlockSkull;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.state.IBlockState;
import net.minecraft.block.BlockSourceImpl;
import net.minecraft.dispenser.IBehaviorDispenseItem;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntityNote;
import net.minecraft.tileentity.TileEntitySkull;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Mirror;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Rotation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameType;
import net.minecraft.world.IWorldEventListener;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/** Stream Hd: the piglin head and mob heads on note blocks. */
public final class HeadsSelfTest implements SelfTest {

    @Override
    public String name() {
        return "heads";
    }

    @Override
    public void run(Check c) {
        BlockPiglinHead head = HeadsContent.head;
        ItemPiglinHead item = HeadsContent.headItem;
        if (!c.ok(head != null && item != null, "piglin head registered")) {
            return;
        }
        registration(c, head, item);
        placement(c, head, item);
        noteBlocks(c, head, item);
        drops(c, item);
        dispenser(c, item);
    }

    private void registration(Check c, BlockPiglinHead head, ItemPiglinHead item) {
        c.eq(Content.id("piglin_head"), head.getRegistryName(), "block id");
        c.eq(Content.id("piglin_head"), item.getRegistryName(), "item id");
        c.eq("tile.trailsandtalesbackport.piglin_head", item.getTranslationKey(new ItemStack(item)), "translation key");
        c.eq(5, head.getBlockState().getValidStates().size(), "standing + 4 wall facings");
        for (IBlockState s : head.getBlockState().getValidStates()) {
            c.eq(s, head.getStateFromMeta(head.getMetaFromState(s)), "meta round trip " + s);
        }
        IBlockState up = head.getDefaultState();
        c.eq(1.0F, up.getBlockHardness(c.world, BlockPos.ORIGIN), "strength 1");
        c.eq(EnumPushReaction.DESTROY, up.getPushReaction(), "pistons destroy it");
        c.ok(!up.isOpaqueCube() && !up.isFullCube(), "not a full block");
        c.eq(new AxisAlignedBB(3 / 16D, 0, 3 / 16D, 13 / 16D, 0.5, 13 / 16D), up.getBoundingBox(c.world, BlockPos.ORIGIN), "standing shape 10x8x10");
        c.eq(new AxisAlignedBB(3 / 16D, 0.25, 0.5, 13 / 16D, 0.75, 1), up.withProperty(BlockPiglinHead.FACING, EnumFacing.NORTH)
                .getBoundingBox(c.world, BlockPos.ORIGIN), "north wall shape");
        c.eq(new AxisAlignedBB(0, 0.25, 3 / 16D, 0.5, 0.75, 13 / 16D), up.withProperty(BlockPiglinHead.FACING, EnumFacing.EAST)
                .getBoundingBox(c.world, BlockPos.ORIGIN), "east wall shape");
        c.eq(EnumFacing.SOUTH, up.withProperty(BlockPiglinHead.FACING, EnumFacing.EAST).withRotation(Rotation.CLOCKWISE_90)
                .getValue(BlockPiglinHead.FACING), "wall head rotates");
        c.eq(EnumFacing.WEST, up.withProperty(BlockPiglinHead.FACING, EnumFacing.EAST).withMirror(Mirror.FRONT_BACK)
                .getValue(BlockPiglinHead.FACING), "wall head mirrors");
        c.eq(up, up.withRotation(Rotation.CLOCKWISE_90), "standing head keeps its state when rotated");
        ItemStack stack = new ItemStack(item);
        c.eq(EntityEquipmentSlot.HEAD, EntityLiving.getSlotForItemStack(stack), "worn in the head slot");
        c.ok(item.isValidArmor(stack, EntityEquipmentSlot.HEAD, null) && !item.isValidArmor(stack, EntityEquipmentSlot.CHEST, null),
                "fits the head armor slot only");
        c.eq(item, head.getItemDropped(up, c.world.rand, 0), "drops itself");
        c.ok(HeadsContent.enabled(), "config heads.enabled");
        int sounds = 0;
        for (String mob : new String[]{"skeleton", "wither_skeleton", "zombie", "creeper", "ender_dragon", "piglin"}) {
            sounds += ForgeRegistries.SOUND_EVENTS.containsKey(Content.id("block.note_block.imitate." + mob)) ? 1 : 0;
        }
        c.eq(6, sounds, "6 note block imitate sounds");

        TilePiglinHead tile = new TilePiglinHead();
        tile.setRotation(13);
        NBTTagCompound tag = tile.writeToNBT(new NBTTagCompound());
        TilePiglinHead read = new TilePiglinHead();
        read.readFromNBT(tag);
        c.eq(13, read.getRotation(), "rotation saved");
        read.rotate(Rotation.CLOCKWISE_90);
        c.eq(1, read.getRotation(), "standing rotation turns with the structure (13 + 4) & 15");
        read.mirror(Mirror.LEFT_RIGHT);
        c.eq(7, read.getRotation(), "standing rotation mirrors left-right: (8 - 1) & 15");
        c.eq(0.0F, read.getAnimation(0.5F), "no animation until powered (client only)");
        c.eq(0, ItemPiglinHead.rotationFor(0.0F), "yaw 0 -> rotation 0");
        c.eq(4, ItemPiglinHead.rotationFor(90.0F), "yaw 90 -> rotation 4");
        c.eq(8, ItemPiglinHead.rotationFor(-180.0F), "yaw -180 -> rotation 8");
        c.eq(15, ItemPiglinHead.rotationFor(-20.0F), "yaw -20 -> rotation 15");
    }

    private void placement(Check c, BlockPiglinHead head, ItemPiglinHead item) {
        BlockPos area = c.area();
        BlockPos floor = area.add(4, -1, 4);
        EntityPlayerMP player = Players.real(c.world, "headplacer");
        player.setPosition(floor.getX() + 0.5, floor.getY() + 1, floor.getZ() - 1.5);
        player.rotationYaw = 90.0F;
        ItemStack heads = new ItemStack(item, 3);
        player.setHeldItem(EnumHand.MAIN_HAND, heads);

        use(player, floor, EnumFacing.UP, heads);
        BlockPos standing = floor.up();
        IBlockState s = c.world.getBlockState(standing);
        c.eq(head, s.getBlock(), "placed standing on a top face");
        c.eq(EnumFacing.UP, s.getValue(BlockPiglinHead.FACING), "standing facing up");
        c.ok(c.world.getTileEntity(standing) instanceof TilePiglinHead, "has its tile entity");
        c.eq(4, ((TilePiglinHead) c.world.getTileEntity(standing)).getRotation(), "turned to the player (yaw 90)");
        c.eq(2, heads.getCount(), "one head used");

        BlockPos wall = area.add(8, 0, 8);
        c.world.setBlockState(wall, Blocks.STONE.getDefaultState());
        use(player, wall, EnumFacing.EAST, heads);
        IBlockState w = c.world.getBlockState(wall.east());
        c.eq(head, w.getBlock(), "placed on a wall");
        c.eq(EnumFacing.EAST, w.getValue(BlockPiglinHead.FACING), "wall head looks out of the wall");
        c.eq(0, ((TilePiglinHead) c.world.getTileEntity(wall.east())).getRotation(), "wall head keeps rotation 0");

        int before = heads.getCount();
        use(player, wall.up(2), EnumFacing.DOWN, heads);
        c.eq(before, heads.getCount(), "not placed on a bottom face");
        c.eq(item, c.world.getBlockState(standing).getBlock().getItemDropped(c.world.getBlockState(standing), c.world.rand, 0),
                "breaking it gives the head");
    }

    private static void use(EntityPlayerMP player, BlockPos pos, EnumFacing face, ItemStack stack) {
        player.interactionManager.processRightClickBlock(player, player.world, stack, EnumHand.MAIN_HAND, pos, face, 0.5F, 0.5F, 0.5F);
    }

    // --- note blocks -----------------------------------------------------------------------------

    /** Records every sound played to the world's listeners at one position. */
    private static final class Sounds implements IWorldEventListener {
        final List<SoundEvent> heard = new ArrayList<SoundEvent>();
        final List<Float> pitches = new ArrayList<Float>();
        BlockPos at;

        @Override
        public void playSoundToAllNearExcept(@Nullable EntityPlayer player, SoundEvent sound, SoundCategory category,
                                             double x, double y, double z, float volume, float pitch) {
            if (this.at != null && new BlockPos(x, y, z).equals(this.at)) {
                this.heard.add(sound);
                this.pitches.add(pitch);
                if (category != SoundCategory.RECORDS || volume != 3.0F) {
                    this.heard.add(null);
                }
            }
        }

        void clear() {
            this.heard.clear();
            this.pitches.clear();
        }

        // Nothing else is needed.
        @Override public void notifyBlockUpdate(World w, BlockPos p, IBlockState o, IBlockState n, int f) { }
        @Override public void notifyLightSet(BlockPos p) { }
        @Override public void markBlockRangeForRenderUpdate(int a, int b, int c, int d, int e, int f) { }
        @Override public void playRecord(SoundEvent s, BlockPos p) { }
        @Override public void spawnParticle(int id, boolean i, double x, double y, double z, double a, double b, double c, int... p) { }
        @Override public void spawnParticle(int id, boolean i, boolean m, double x, double y, double z, double a, double b, double c, int... p) { }
        @Override public void onEntityAdded(Entity e) { }
        @Override public void onEntityRemoved(Entity e) { }
        @Override public void broadcastSound(int id, BlockPos p, int d) { }
        @Override public void playEvent(EntityPlayer pl, int t, BlockPos p, int d) { }
        @Override public void sendBlockBreakProgress(int id, BlockPos p, int prog) { }
    }

    private void noteBlocks(Check c, BlockPiglinHead head, ItemPiglinHead item) {
        BlockPos area = c.area();
        Sounds sounds = new Sounds();
        c.world.addEventListener(sounds);
        try {
            noteBlocks(c, head, item, area, sounds);
        } finally {
            c.world.removeEventListener(sounds);
        }
    }

    private void noteBlocks(Check c, BlockPiglinHead head, ItemPiglinHead item, BlockPos area, Sounds sounds) {
        BlockPos note = area.add(4, 0, 4);
        c.world.setBlockState(note, Blocks.NOTEBLOCK.getDefaultState());
        sounds.at = note;

        // Each vanilla head type on top, and ours.
        SoundEvent[] expected = {HeadsContent.IMITATE_SKELETON, HeadsContent.IMITATE_WITHER_SKELETON, HeadsContent.IMITATE_ZOMBIE,
                null, HeadsContent.IMITATE_CREEPER, HeadsContent.IMITATE_ENDER_DRAGON};
        for (int type = 0; type < 6; type++) {
            c.world.setBlockState(note.up(), Blocks.SKULL.getDefaultState().withProperty(BlockSkull.FACING, EnumFacing.UP));
            ((TileEntitySkull) c.world.getTileEntity(note.up())).setType(type);
            c.eq(expected[type], NoteBlockHeads.soundFor(c.world, note), "skull type " + type + " plays");
            c.ok(NoteBlockHeads.hasHead(c.world, note), "skull type " + type + " counts as a head instrument");
        }
        c.world.setBlockState(note.up(), head.getDefaultState());
        c.eq(HeadsContent.IMITATE_PIGLIN, NoteBlockHeads.soundFor(c.world, note), "piglin head plays the piglin");
        c.world.setBlockState(note.up(), head.getDefaultState().withProperty(BlockPiglinHead.FACING, EnumFacing.NORTH));
        c.eq(HeadsContent.IMITATE_PIGLIN, NoteBlockHeads.soundFor(c.world, note), "a wall piglin head plays too");
        c.world.setBlockState(note.up(), Blocks.STONE.getDefaultState());
        c.eq(null, NoteBlockHeads.soundFor(c.world, note), "stone on top plays nothing of ours");
        c.ok(!NoteBlockHeads.hasHead(c.world, note), "stone is not a head");
        c.world.setBlockToAir(note.up());

        // A head item used on the top face goes on top without sneaking; the note block is not tuned.
        EntityPlayerMP player = Players.real(c.world, "musician");
        player.setPosition(note.getX() + 0.5, note.getY(), note.getZ() - 1.5);
        player.interactionManager.setGameType(GameType.SURVIVAL);
        TileEntityNote tile = (TileEntityNote) c.world.getTileEntity(note);
        tile.note = 3;
        ItemStack zombie = new ItemStack(Items.SKULL, 2, 2);
        player.setHeldItem(EnumHand.MAIN_HAND, zombie);
        use(player, note, EnumFacing.UP, zombie);
        c.eq(Blocks.SKULL, c.world.getBlockState(note.up()).getBlock(), "zombie head placed on the note block without sneaking");
        c.eq((byte) 3, tile.note, "the note block was not tuned");
        c.eq(1, zombie.getCount(), "the head item was used");
        c.eq(HeadsContent.IMITATE_ZOMBIE, NoteBlockHeads.soundFor(c.world, note), "it now plays the zombie");

        // A head item used on a side face is an ordinary use: the note block is tuned and plays.
        sounds.clear();
        use(player, note, EnumFacing.NORTH, zombie);
        c.eq((byte) 4, tile.note, "a side-face click with a head item tunes it");
        c.eq(1, zombie.getCount(), "nothing placed from the side");
        c.eq(listOf(HeadsContent.IMITATE_ZOMBIE), sounds.heard, "tuning plays the zombie, volume 3, records");
        c.eq(listOf(1.0F), sounds.pitches, "pitch 1 whatever the note");

        // Empty hand: tuned and plays.
        player.setHeldItem(EnumHand.MAIN_HAND, ItemStack.EMPTY);
        sounds.clear();
        use(player, note, EnumFacing.NORTH, ItemStack.EMPTY);
        c.eq((byte) 5, tile.note, "right click tunes it");
        c.eq(listOf(HeadsContent.IMITATE_ZOMBIE), sounds.heard, "right click plays the zombie");

        // Sneaking with an item: the block is not used, nothing plays.
        player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.STICK));
        player.setSneaking(true);
        sounds.clear();
        use(player, note, EnumFacing.NORTH, player.getHeldItemMainhand());
        c.eq(0, sounds.heard.size(), "sneaking with an item: no sound");
        player.setSneaking(false);
        player.setHeldItem(EnumHand.MAIN_HAND, ItemStack.EMPTY);

        // Hit in survival: plays; in creative the block just breaks (no hit).
        sounds.clear();
        player.interactionManager.onBlockClicked(note, EnumFacing.NORTH);
        c.eq(listOf(HeadsContent.IMITATE_ZOMBIE), sounds.heard, "hitting it plays the zombie");
        player.interactionManager.blockRemoving(note);

        // Redstone: tracked from the placement above; plays on each rising edge only.
        NoteBlockHeads.tick(c.world);
        c.eq(Boolean.FALSE, NoteBlockHeads.trackedPower(c.world, note), "tracked once a head went on it");
        sounds.clear();
        c.world.setBlockState(note.west(), Blocks.REDSTONE_BLOCK.getDefaultState());
        NoteBlockHeads.tick(c.world);
        c.eq(listOf(HeadsContent.IMITATE_ZOMBIE), sounds.heard, "powering it plays the zombie");
        NoteBlockHeads.tick(c.world);
        c.eq(1, sounds.heard.size(), "staying powered plays nothing more");
        c.world.setBlockToAir(note.west());
        NoteBlockHeads.tick(c.world);
        c.eq(1, sounds.heard.size(), "losing power plays nothing");
        c.world.setBlockState(note.west(), Blocks.REDSTONE_BLOCK.getDefaultState());
        NoteBlockHeads.tick(c.world);
        c.eq(2, sounds.heard.size(), "powering it again plays again");

        // Swapping the head for a player head while powered: silent; removing the head stops tracking.
        ((TileEntitySkull) c.world.getTileEntity(note.up())).setType(3);
        c.world.setBlockToAir(note.west());
        NoteBlockHeads.tick(c.world);
        c.world.setBlockState(note.west(), Blocks.REDSTONE_BLOCK.getDefaultState());
        NoteBlockHeads.tick(c.world);
        c.eq(2, sounds.heard.size(), "a player head plays nothing");
        c.world.setBlockToAir(note.up());
        NoteBlockHeads.tick(c.world);
        c.eq(null, NoteBlockHeads.trackedPower(c.world, note), "untracked once the head is gone");

        // A head placed on an already powered note block is silent until the next rising edge.
        ItemStack piglin = new ItemStack(item, 1);
        player.setHeldItem(EnumHand.MAIN_HAND, piglin);
        use(player, note, EnumFacing.UP, piglin);
        c.eq(item, net.minecraft.item.Item.getItemFromBlock(c.world.getBlockState(note.up()).getBlock()), "piglin head placed on top without sneaking");
        sounds.clear();
        NoteBlockHeads.tick(c.world);
        c.eq(Boolean.TRUE, NoteBlockHeads.trackedPower(c.world, note), "tracked as already powered");
        c.eq(0, sounds.heard.size(), "placing a head on a powered note block plays nothing");
        c.world.setBlockToAir(note.west());
        NoteBlockHeads.tick(c.world);
        c.world.setBlockState(note.west(), Blocks.REDSTONE_BLOCK.getDefaultState());
        NoteBlockHeads.tick(c.world);
        c.eq(listOf(HeadsContent.IMITATE_PIGLIN), sounds.heard, "then powering it plays the piglin");

        // Without a head the note block is vanilla's: our handler plays nothing.
        c.world.setBlockToAir(note.up());
        c.world.setBlockToAir(note.west());
        sounds.clear();
        player.setHeldItem(EnumHand.MAIN_HAND, ItemStack.EMPTY);
        use(player, note, EnumFacing.NORTH, ItemStack.EMPTY);
        c.ok(!sounds.heard.contains(HeadsContent.IMITATE_PIGLIN) && !sounds.heard.contains(HeadsContent.IMITATE_ZOMBIE),
                "no head: no mob sound");
    }

    private static <T> List<T> listOf(T one) {
        List<T> list = new ArrayList<T>();
        list.add(one);
        return list;
    }

    // --- the drop --------------------------------------------------------------------------------

    private void drops(Check c, ItemPiglinHead item) {
        BlockPos area = c.area();
        EntityZombie dead = new EntityZombie(c.world);
        dead.setPosition(area.getX() + 4.5, area.getY(), area.getZ() + 4.5);
        EntityCreeper charged = creeper(c, area, true);
        EntityCreeper plain = creeper(c, area, false);
        ResourceLocation piglin = HeadsContent.PIGLIN;
        c.eq(new ResourceLocation("netherupdatebackport", "piglin"), piglin, "Nether Update's piglin, by name");

        c.eq(null, HeadsContent.headDrop(dead, piglin, DamageSource.causeExplosionDamage(plain)), "an uncharged creeper drops no head");
        c.eq(null, HeadsContent.headDrop(dead, new ResourceLocation("minecraft", "zombie"), DamageSource.causeExplosionDamage(charged)),
                "only a piglin drops the piglin head");
        c.eq(null, HeadsContent.headDrop(dead, null, DamageSource.causeExplosionDamage(charged)), "an unregistered entity drops nothing");
        c.eq(null, HeadsContent.headDrop(dead, piglin, DamageSource.GENERIC), "no killer, no head");
        dead.setChild(true);
        c.eq(null, HeadsContent.headDrop(dead, piglin, DamageSource.causeExplosionDamage(charged)), "a baby drops no head");
        dead.setChild(false);
        EntityItem drop = HeadsContent.headDrop(dead, piglin, DamageSource.causeExplosionDamage(charged));
        c.ok(drop != null && drop.getItem().getItem() == item && drop.getItem().getCount() == 1, "a charged creeper's kill drops one piglin head");
        c.eq(null, HeadsContent.headDrop(dead, piglin, DamageSource.causeExplosionDamage(charged)), "one head per creeper");
        EntityCreeper other = creeper(c, area, true);
        c.world.getGameRules().setOrCreateGameRule("doMobLoot", "false");
        try {
            c.eq(null, HeadsContent.headDrop(dead, piglin, DamageSource.causeExplosionDamage(other)), "doMobLoot off: no head");
        } finally {
            c.world.getGameRules().setOrCreateGameRule("doMobLoot", "true");
        }
        c.ok(HeadsContent.headDrop(dead, piglin, DamageSource.causeExplosionDamage(other)) != null, "another charged creeper drops one");
    }

    private static EntityCreeper creeper(Check c, BlockPos area, boolean powered) {
        EntityCreeper creeper = new EntityCreeper(c.world);
        creeper.setPosition(area.getX() + 8.5, area.getY(), area.getZ() + 8.5);
        if (powered) {
            NBTTagCompound tag = new NBTTagCompound();
            creeper.writeEntityToNBT(tag);
            tag.setBoolean("powered", true);
            creeper.readEntityFromNBT(tag);
        }
        return creeper;
    }

    // --- the dispenser ---------------------------------------------------------------------------

    private void dispenser(Check c, ItemPiglinHead item) {
        BlockPos area = c.area();
        BlockPos pos = area.add(4, 0, 4);
        c.world.setBlockState(pos, Blocks.DISPENSER.getDefaultState().withProperty(BlockDispenser.FACING, EnumFacing.EAST));
        IBehaviorDispenseItem behavior = BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.getObject(item);
        BlockSourceImpl source = new BlockSourceImpl(c.world, pos);

        ItemStack stack = new ItemStack(item, 2);
        behavior.dispense(source, stack);
        c.eq(2, stack.getCount(), "nothing in front: the head stays in the dispenser");
        c.eq(0, c.world.getEntitiesWithinAABB(EntityItem.class, new AxisAlignedBB(pos).grow(2)).size(), "and is not thrown out");

        EntityArmorStand stand = new EntityArmorStand(c.world, pos.getX() + 1.5, pos.getY(), pos.getZ() + 0.5);
        c.world.spawnEntity(stand);
        behavior.dispense(source, stack);
        c.eq(1, stack.getCount(), "one head dispensed");
        c.eq(item, stand.getItemStackFromSlot(EntityEquipmentSlot.HEAD).getItem(), "onto the armor stand's head");
        stand.setDead();
    }
}
