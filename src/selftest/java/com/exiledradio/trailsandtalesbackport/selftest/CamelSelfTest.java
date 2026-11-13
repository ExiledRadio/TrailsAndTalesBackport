package com.exiledradio.trailsandtalesbackport.selftest;

import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import com.exiledradio.trailsandtalesbackport.camel.CamelAdvancements;
import com.exiledradio.trailsandtalesbackport.camel.CamelContent;
import com.exiledradio.trailsandtalesbackport.camel.CamelVillages;
import com.exiledradio.trailsandtalesbackport.camel.EntityCamel;

import net.minecraft.advancements.Advancement;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.gen.structure.MapGenStructureData;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

/** Stream Ca: camels - riding, the dash, sitting, breeding, village camels, Two by Two. */
public final class CamelSelfTest implements SelfTest {

    private static final double EPS = 1.0E-4D;

    @Override
    public String name() {
        return "camels";
    }

    @Override
    public void run(Check c) throws Exception {
        if (!c.ok(EntityList.isRegistered(new ResourceLocation(TrailsAndTalesBackport.MOD_ID, "camel")), "camel registered")) {
            return;
        }
        // A new camel counts as standing since 53 ticks ago (1.20); in a world younger than that it is
        // still getting up, so the clock is moved on first.
        c.world.getWorldInfo().setWorldTotalTime(c.world.getTotalWorldTime() + 200);
        registration(c);
        riding(c);
        dash(c);
        sitting(c);
        randomSitting(c);
        breeding(c);
        villages(c);
    }

    private void registration(Check c) {
        ResourceLocation id = new ResourceLocation(TrailsAndTalesBackport.MOD_ID, "camel");
        c.ok(EntityList.ENTITY_EGGS.containsKey(id), "camel spawn egg");
        c.eq(CamelContent.EGG_PRIMARY, EntityList.ENTITY_EGGS.get(id).primaryColor, "spawn egg color");
        EntityCamel camel = new EntityCamel(c.world);
        c.eq(32.0D, camel.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).getBaseValue(), "32 health");
        c.ok(Math.abs(camel.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getBaseValue() - 0.09D) < EPS, "speed 0.09");
        c.ok(Math.abs(camel.getHorseJumpStrength() - 0.42D) < EPS, "jump strength 0.42");
        c.eq(1.5F, camel.stepHeight, "step height 1.5 (1.20)");
        c.eq(1.7F, camel.width, "1.7 wide");
        c.eq(2.375F, camel.height, "2.375 tall");
        c.ok(Math.abs(camel.getEyeHeight() - 2.275F) < EPS, "eye height 2.275");
        c.ok(camel.isTame(), "always tame");
        c.ok(camel.isBreedingItem(new ItemStack(Blocks.CACTUS)), "cactus is its food");
        c.ok(!camel.isBreedingItem(new ItemStack(Items.WHEAT)), "wheat is not");
        camel.setGrowingAge(-24000);
        c.ok(Math.abs(camel.width - 1.7F * 0.45F) < EPS && Math.abs(camel.height - 2.375F * 0.45F) < EPS, "a baby is 0.45 the size");
        // 1.20 spawns camels grown (1.12.2's horse makes a foal one time in five).
        int babies = 0;
        for (int i = 0; i < 40; i++) {
            EntityCamel spawned = new EntityCamel(c.world);
            spawned.onInitialSpawn(c.world.getDifficultyForLocation(BlockPos.ORIGIN), null);
            babies += spawned.isChild() ? 1 : 0;
        }
        c.eq(0, babies, "spawned camels are grown");
    }

    private EntityCamel spawn(Check c, BlockPos area, double x, double z, float yaw) {
        EntityCamel camel = new EntityCamel(c.world);
        camel.setLocationAndAngles(area.getX() + x, area.getY(), area.getZ() + z, yaw, 0.0F);
        camel.onInitialSpawn(c.world.getDifficultyForLocation(new BlockPos(camel)), null);
        c.world.spawnEntity(camel);
        return camel;
    }

    private void riding(Check c) throws ReflectiveOperationException {
        BlockPos area = c.area();
        EntityCamel camel = spawn(c, area, 16.5, 16.5, 0.0F);
        run(c, 3, camel);
        EntityPlayerMP a = Players.real(c.world, "riderA");
        EntityPlayerMP b = Players.real(c.world, "riderB");
        EntityPlayerMP d = Players.real(c.world, "riderC");
        for (EntityPlayerMP p : new EntityPlayerMP[] {a, b, d}) {
            p.setPosition(camel.posX + 2, camel.posY, camel.posZ);
        }
        c.ok(camel.getControllingPassenger() == null, "nobody steers an empty camel");

        // A saddle is put on by right-clicking, and used up.
        a.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.SADDLE));
        c.ok(camel.processInteract(a, EnumHand.MAIN_HAND), "a saddle is accepted");
        c.ok(camel.isHorseSaddled(), "saddled");
        c.ok(a.getHeldItemMainhand().isEmpty(), "the saddle is used up");

        // Two riders.
        c.ok(camel.processInteract(a, EnumHand.MAIN_HAND) && a.getRidingEntity() == camel, "the first player gets on");
        c.ok(camel.processInteract(b, EnumHand.MAIN_HAND) && b.getRidingEntity() == camel, "a second player gets on");
        camel.processInteract(d, EnumHand.MAIN_HAND);
        c.ok(d.getRidingEntity() == null, "a third does not");
        c.eq(2, camel.getPassengers().size(), "two riders");
        c.eq(a, camel.getControllingPassenger(), "the first rider steers");

        // Seats: 1.20's attachment points, standing: 2.0 up; the driver 0.5 forward, the other 0.7 back.
        Vec3d front = camel.getPassengerAttachmentPoint(a, 1.0F);
        Vec3d back = camel.getPassengerAttachmentPoint(b, 1.0F);
        c.ok(Math.abs(front.y - 2.0D) < EPS, "seat height 2.0 standing (" + front.y + ")");
        c.ok(seat(camel, front, 0.5D), "the driver sits 0.5 forward (" + front + ", yaw " + camel.rotationYaw + ")");
        c.ok(seat(camel, back, -0.7D), "the second rider 0.7 back (" + back + ")");
        camel.updatePassenger(a);
        c.ok(Math.abs(a.posY - (camel.posY + 2.0D - 0.6D)) < EPS, "a player's feet 0.6 below the seat (1.20's player attachment)");

        // Sitting lowers the seats; the rider's forward key stands it back up once it has sat down.
        quiet(camel);
        camel.sitDown();
        run(c, 41, camel);
        Vec3d sat = camel.getPassengerAttachmentPoint(a, 1.0F);
        c.ok(Math.abs(sat.y - (0.945D - 0.375D + (1.43D - (1.43D - 0.2D)))) < EPS, "seat height sitting (" + sat.y + ", height " + camel.height + ", sitting " + camel.isCamelSitting() + ", pose time " + camel.getPoseTime() + ")");
        c.ok(!camel.canJump(), "no dash while sitting");
        a.setEntityActionState(0.0F, 1.0F, false, false);
        run(c, 1, camel);
        c.ok(!camel.isCamelSitting(), "the driver's forward key stands it up");
        a.setEntityActionState(0.0F, 0.0F, false, false);

        // Without a saddle nobody steers (1.20; 1.12.2's horse lets its first rider).
        camel.removePassengers();
        EntityCamel bare = spawn(c, area, 8.5, 8.5, 0.0F);
        bare.processInteract(d, EnumHand.MAIN_HAND);
        c.ok(d.getRidingEntity() == bare, "anyone can get on an unsaddled camel");
        c.ok(bare.getControllingPassenger() == null, "but nobody steers it");
        c.ok(!bare.canJump(), "and it has no dash");
        bare.removePassengers();

        // A baby carries nobody and takes no saddle.
        EntityCamel baby = spawn(c, area, 24.5, 24.5, 0.0F);
        baby.setGrowingAge(-24000);
        d.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.SADDLE));
        baby.processInteract(d, EnumHand.MAIN_HAND);
        c.ok(!baby.isHorseSaddled(), "a baby takes no saddle");
        d.setHeldItem(EnumHand.MAIN_HAND, ItemStack.EMPTY);
        baby.processInteract(d, EnumHand.MAIN_HAND);
        c.ok(d.getRidingEntity() == null, "nor riders");

        // A pig as second passenger sits 0.2 further forward (1.20's animal seat).
        EntityPig pig = new EntityPig(c.world);
        pig.setPosition(camel.posX, camel.posY, camel.posZ);
        a.startRiding(camel);
        pig.startRiding(camel, true);
        c.ok(seat(camel, camel.getPassengerAttachmentPoint(pig, 1.0F), -0.5D), "an animal's back seat is at -0.5 (" + camel.getPassengerAttachmentPoint(pig, 1.0F) + ", riders " + camel.getPassengers() + ", yaw " + camel.rotationYaw + ")");
        camel.removePassengers();

        // Its inventory opens to a sneaking player, and the saddle is kept when saved.
        NBTTagCompound tag = new NBTTagCompound();
        camel.writeToNBT(tag);
        EntityCamel copy = new EntityCamel(c.world);
        copy.readFromNBT(tag);
        c.ok(copy.isHorseSaddled(), "the saddle is saved");
        for (EntityCamel e : new EntityCamel[] {camel, bare, baby, copy}) {
            e.setDead();
        }
    }

    private void dash(Check c) {
        BlockPos area = c.area();
        EntityCamel camel = spawn(c, area, 16.5, 2.5, 0.0F);
        EntityPlayerMP p = Players.real(c.world, "dasher");
        p.setPosition(camel.posX, camel.posY, camel.posZ);
        p.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.SADDLE));
        camel.processInteract(p, EnumHand.MAIN_HAND);
        run(c, 3, camel);
        c.ok(camel.onGround, "standing on the floor");
        c.ok(camel.canJump(), "a saddled standing camel can dash");

        // Speed: +0.1 while the rider sprints and the dash is ready.
        p.setSprinting(true);
        c.ok(Math.abs(camel.getRiddenSpeed(p) - 0.19F) < EPS, "sprinting rider: speed 0.19");
        p.setSprinting(false);
        c.ok(Math.abs(camel.getRiddenSpeed(p) - 0.09F) < EPS, "walking rider: speed 0.09");

        // The push: 22.2222 x speed forward and 1.4285 x jump strength up, at a full bar.
        camel.motionX = camel.motionY = camel.motionZ = 0.0D;
        camel.executeRidersJump(1.0F);
        c.ok(Math.abs(camel.motionZ - 22.2222D * 0.09D) < 1.0E-3D && Math.abs(camel.motionX) < EPS,
                "forward push 2.0 (facing south: " + camel.motionX + ", " + camel.motionZ + ")");
        c.ok(Math.abs(camel.motionY - 1.4285D * 0.42D) < 1.0E-3D, "up 0.6 (" + camel.motionY + ")");
        c.eq(EntityCamel.DASH_COOLDOWN_TICKS, camel.getJumpCooldown(), "55-tick cooldown");
        c.ok(camel.isDashing(), "dashing");

        // During the cooldown: no new dash (the jump bar's power is refused, the server ignores the key).
        camel.setJumpPower(100);
        int before = camel.getJumpCooldown();
        camel.handleStartJump(100);
        c.eq(before, camel.getJumpCooldown(), "a dash key during the cooldown is ignored");
        run(c, 30, camel);
        c.ok(!camel.isDashing(), "the dash ends once on the ground after 5 ticks (" + camel.getJumpCooldown() + " left)");
        run(c, 30, camel);
        c.eq(0, camel.getJumpCooldown(), "the cooldown runs out");

        // The server's side of a dash: the sound and the dash, which starts the cooldown there too.
        camel.handleStartJump(100);
        c.ok(camel.isDashing(), "the dash key dashes when ready");
        c.eq(EntityCamel.DASH_COOLDOWN_TICKS, camel.getJumpCooldown(), "and starts the cooldown (synced flag)");
        run(c, 60, camel);
        camel.setDead();
    }

    private void sitting(Check c) throws ReflectiveOperationException {
        BlockPos area = c.area();
        EntityCamel camel = spawn(c, area, 16.5, 16.5, 0.0F);
        run(c, 3, camel);
        c.ok(!camel.isCamelSitting() && !camel.refuseToMove(), "a new camel stands, ready");
        quiet(camel);
        camel.sitDown();
        c.ok(camel.isCamelSitting(), "sits down");
        c.ok(Math.abs(camel.height - 0.945F) < EPS, "0.945 tall sitting");
        c.ok(Math.abs(camel.getEyeHeight() - 0.845F) < EPS, "eye height 0.845 sitting");
        c.ok(camel.refuseToMove() && camel.isInPoseTransition(), "refuses to move while sitting down");
        run(c, 39, camel);
        c.ok(camel.isInPoseTransition(), "sitting down takes 40 ticks");
        run(c, 1, camel);
        c.ok(!camel.isInPoseTransition() && camel.refuseToMove(), "then sits, still refusing to move");
        double x = camel.posX;
        camel.motionX = 0.5D;
        run(c, 3, camel);
        c.ok(Math.abs(camel.posX - x) < EPS, "a sitting camel does not slide");

        camel.standUp();
        c.ok(!camel.isCamelSitting() && camel.isInPoseTransition(), "stands up");
        c.ok(Math.abs(camel.height - 2.375F) < EPS, "2.375 tall again");
        run(c, 51, camel);
        c.ok(camel.isInPoseTransition(), "standing up takes 52 ticks");
        run(c, 1, camel);
        c.ok(!camel.isInPoseTransition(), "then stands");

        quiet(camel);
        camel.sitDown();
        run(c, 50, camel);
        camel.attackEntityFrom(DamageSource.GENERIC, 1.0F);
        c.ok(!camel.isCamelSitting() && !camel.refuseToMove(), "hurt, it stands up at once");

        camel.sitDown();
        NBTTagCompound tag = new NBTTagCompound();
        camel.writeToNBT(tag);
        EntityCamel copy = new EntityCamel(c.world);
        copy.readFromNBT(tag);
        c.ok(copy.isCamelSitting() && tag.getLong("LastPoseTick") < 0L, "sitting is saved (LastPoseTick)");

        c.fill(area.add(14, 0, 14), area.add(19, 0, 19), Blocks.WATER.getDefaultState());
        run(c, 2, camel);
        c.ok(!camel.isCamelSitting(), "it stands up in water");
        camel.setDead();
    }

    /** 1.20's RandomSitting: after 20 seconds in a pose, an idle camel sometimes sits down and later stands up. */
    private void randomSitting(Check c) {
        BlockPos area = c.area();
        // A 4x4 pen, so it cannot wander off.
        c.fill(area.add(13, 0, 13), area.add(20, 2, 20), Blocks.STONE.getDefaultState());
        c.fill(area.add(14, 0, 14), area.add(19, 2, 19), Blocks.AIR.getDefaultState());
        EntityCamel camel = spawn(c, area, 17.0, 17.0, 0.0F);
        boolean sat = false;
        boolean stoodAgain = false;
        int t = 0;
        int satAt = -1;
        while (t < 6000 && !stoodAgain) {
            run(c, 1, camel);
            t++;
            if (!sat && camel.isCamelSitting()) {
                sat = true;
                satAt = t;
            } else if (sat && !camel.isCamelSitting()) {
                stoodAgain = true;
            }
        }
        c.ok(sat, "an idle camel sits down by itself (after " + satAt + " ticks)");
        c.ok(satAt < 0 || satAt >= 400 - 60, "not before 20 seconds standing (" + satAt + ")");
        c.ok(stoodAgain, "and stands up again (by " + t + ")");
        camel.setDead();
    }

    private void breeding(Check c) throws Exception {
        BlockPos area = c.area();
        EntityCamel a = spawn(c, area, 12.5, 16.5, 90.0F);
        EntityCamel b = spawn(c, area, 18.5, 16.5, -90.0F);
        EntityPlayerMP player = Players.real(c.world, "camelbreeder");
        player.setPosition(area.getX() + 15.5, area.getY(), area.getZ() + 2.5);
        Item cactus = Item.getItemFromBlock(Blocks.CACTUS);
        for (EntityCamel camel : new EntityCamel[] {a, b}) {
            player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(cactus, 2));
            c.ok(camel.processInteract(player, EnumHand.MAIN_HAND), "a grown camel takes cactus");
            c.ok(camel.isInLove(), "and is in love");
            c.eq(1, player.getHeldItemMainhand().getCount(), "one cactus eaten");
        }
        player.setPosition(area.getX() + 15.5, area.getY() + 40, area.getZ() + 2.5);
        int t = 0;
        List<EntityCamel> babies = null;
        while (t < 600) {
            run(c, 10, a, b);
            t += 10;
            babies = c.world.getEntitiesWithinAABB(EntityCamel.class, new AxisAlignedBB(area, area.add(32, 4, 32)),
                    e -> e != null && e.isChild());
            if (!babies.isEmpty()) {
                break;
            }
        }
        c.eq(1, babies == null ? 0 : babies.size(), "two camels in love have one baby (" + t + " ticks)");
        c.ok(a.getGrowingAge() >= 5989 && a.getGrowingAge() <= 6000, "parent cooldown 6000 (" + a.getGrowingAge() + ")");
        c.ok(!a.isInLove() && !b.isInLove(), "no longer in love");
        if (babies != null && !babies.isEmpty()) {
            EntityCamel baby = babies.get(0);
            c.eq(-24000, baby.getGrowingAge(), "a baby camel takes 20 minutes to grow");
            c.ok(Math.abs(baby.width - 1.7F * 0.45F) < EPS, "at 0.45 size");
            int age = baby.getGrowingAge();
            player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(cactus, 1));
            baby.processInteract(player, EnumHand.MAIN_HAND);
            c.eq(age + 200, baby.getGrowingAge(), "cactus grows a baby by 10 seconds");
            baby.setDead();
        }
        // Healing: 2 a cactus.
        a.setHealth(20.0F);
        a.setGrowingAge(6000);
        player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(cactus, 1));
        a.processInteract(player, EnumHand.MAIN_HAND);
        c.ok(Math.abs(a.getHealth() - 22.0F) < EPS, "cactus heals 2 (" + a.getHealth() + ")");

        Advancement two = c.world.getMinecraftServer().getAdvancementManager().getAdvancement(CamelAdvancements.TWO_BY_TWO);
        c.ok(two != null && two.getCriteria().containsKey(CamelAdvancements.CAMEL_CRITERION), "Two by Two needs camels");
        a.setDead();
        b.setDead();
    }

    // ------------------------------------------------------------------ villages

    @SuppressWarnings("unchecked")
    private void villages(Check c) throws Exception {
        // Village & Pillage's own pool file, read by name: its first three desert town centers are 1.20's three.
        List<String> real = CamelVillages.townCenters();
        if (net.minecraftforge.fml.common.Loader.isModLoaded("villagerbackport")) {
            c.ok(real.size() >= 3 && real.get(0).endsWith("desert_meeting_point_1") && real.get(1).endsWith("desert_meeting_point_2")
                    && real.get(2).endsWith("desert_meeting_point_3"), "Village & Pillage's desert town centers are read (" + real + ")");
        } else {
            c.ok(real.isEmpty(), "without Village & Pillage there are no modern town centers");
        }
        Field field = ReflectionHelper.findField(CamelVillages.class, "townCenters");
        List<String> saved = (List<String>) field.get(null);
        List<String> pool = Arrays.asList(
                "village/desert/town_centers/desert_meeting_point_1",
                "village/desert/town_centers/desert_meeting_point_2",
                "village/desert/town_centers/desert_meeting_point_3",
                "village/desert/zombie/town_centers/desert_meeting_point_1");
        field.set(null, pool);
        try {
            // 1.20's camel jigsaws, unturned: point 1 at (8,0,4) top south -> camel piece turned 180, the camel
            // at its block's far corner; 2 at (6,0,1) top west; 3 at (7,0,8) top north.
            spotEq(c, piece(0, new BlockPos(100, 64, 200), Rotation.NONE), 109.0, 65, 205.0, 180.0F, "meeting point 1");
            spotEq(c, piece(1, new BlockPos(100, 64, 200), Rotation.NONE), 106.0, 65, 202.0, 270.0F, "meeting point 2");
            spotEq(c, piece(2, new BlockPos(100, 64, 200), Rotation.NONE), 107.0, 65, 208.0, 0.0F, "meeting point 3");
            // Turned clockwise: (8,0,4) -> (-4,0,8), top south -> west.
            spotEq(c, piece(0, new BlockPos(100, 64, 200), Rotation.CLOCKWISE_90), 96.0, 65, 209.0, 270.0F, "meeting point 1 turned");
            c.ok(CamelVillages.modernSpot(piece(3, BlockPos.ORIGIN, Rotation.NONE)) == null, "no camel in a zombie village");

            BlockPos area = c.area();
            // A Village & Pillage village whose desert town center has its camel spot in this test area.
            BlockPos origin = area.add(4, -1, 4);
            NBTTagCompound start = new NBTTagCompound();
            start.setString("id", "VBP_ModernVillage");
            NBTTagList children = new NBTTagList();
            NBTTagCompound center = piece(2, origin, Rotation.NONE);
            center.setString("id", "VBP_ModernPiece");
            children.appendTag(center);
            start.setTag("Children", children);
            int spawned = spawnFromFakeStart(c, start, origin, "Village");
            c.eq(1, spawned, "a desert village's town center gets one camel");

            // Vanilla 1.12.2's desert village: one camel at a corner of the well.
            BlockPos well = area.add(14, -12, 14);
            NBTTagCompound vanilla = new NBTTagCompound();
            vanilla.setString("id", "Village");
            vanilla.setBoolean("Valid", true);
            NBTTagList vChildren = new NBTTagList();
            NBTTagCompound vStart = new NBTTagCompound();
            vStart.setString("id", "ViStart");
            vStart.setTag("BB", new StructureBoundingBox(well.getX(), well.getY(), well.getZ(), well.getX() + 5, well.getY() + 14, well.getZ() + 5).toNBTTagIntArray());
            vStart.setInteger("HPos", area.getY() - 1);
            vStart.setByte("Type", (byte) 1);
            vStart.setBoolean("Zombie", false);
            vChildren.appendTag(vStart);
            vanilla.setTag("Children", vChildren);
            c.eq(1, spawnFromFakeStart(c, vanilla, well, "Village"), "a vanilla desert village gets one camel by its well");
            vStart.setBoolean("Zombie", true);
            c.eq(0, spawnFromFakeStart(c, vanilla, well, "Village"), "a zombie village none");
            vStart.setBoolean("Zombie", false);
            vStart.setByte("Type", (byte) 0);
            c.eq(0, spawnFromFakeStart(c, vanilla, well, "Village"), "a plains village none");
        } finally {
            field.set(null, saved);
        }
    }

    private static NBTTagCompound piece(int element, BlockPos pos, Rotation rotation) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("Pool", "village/desert/town_centers");
        tag.setInteger("Element", element);
        tag.setInteger("PosX", pos.getX());
        tag.setInteger("PosY", pos.getY());
        tag.setInteger("PosZ", pos.getZ());
        tag.setString("Rotation", rotation.name());
        return tag;
    }

    private static void spotEq(Check c, NBTTagCompound piece, double x, double y, double z, float yaw, String what) {
        CamelVillages.Spot spot = CamelVillages.modernSpot(piece);
        boolean ok = spot != null && Math.abs(spot.pos.x - x) < EPS && Math.abs(spot.pos.y - y) < EPS
                && Math.abs(spot.pos.z - z) < EPS && Math.abs(net.minecraft.util.math.MathHelper.wrapDegrees(spot.yaw - yaw)) < EPS;
        c.ok(ok, what + ": expected " + x + "," + y + "," + z + " yaw " + yaw + ", got "
                + (spot == null ? "none" : spot.pos + " yaw " + spot.yaw));
    }

    /**
     * Puts a village start into the world's saved structure data, populates the chunk that holds its camel (and
     * its neighbors, which must place none), and counts the camels; the start is taken out again.
     */
    private static int spawnFromFakeStart(Check c, NBTTagCompound start, BlockPos near, String name) {
        MapGenStructureData data = (MapGenStructureData) c.world.getPerWorldStorage().getOrLoadData(MapGenStructureData.class, name);
        boolean created = data == null;
        if (created) {
            data = new MapGenStructureData(name);
            c.world.getPerWorldStorage().setData(name, data);
        }
        int cx = near.getX() >> 4;
        int cz = near.getZ() >> 4;
        String key = MapGenStructureData.formatChunkCoords(cx, cz);
        data.getTagCompound().setTag(key, start);
        AxisAlignedBB box = new AxisAlignedBB(near.add(-40, -20, -40), near.add(40, 20, 40));
        for (EntityCamel old : c.world.getEntitiesWithinAABB(EntityCamel.class, box)) {
            old.setDead();
        }
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                CamelVillages.spawnInChunk(c.world, cx + dx, cz + dz);
            }
        }
        List<EntityCamel> camels = c.world.getEntitiesWithinAABB(EntityCamel.class, box, e -> e != null && !e.isDead);
        data.getTagCompound().removeTag(key);
        for (EntityCamel camel : camels) {
            camel.setDead();
        }
        return camels.size();
    }

    /** @return true if a seat is this far forward (negative: back) along the camel's facing, on its center line */
    private static boolean seat(EntityCamel camel, Vec3d at, double forward) {
        double yaw = Math.toRadians(camel.rotationYaw);
        return Math.abs(at.x - -Math.sin(yaw) * forward) < EPS && Math.abs(at.z - Math.cos(yaw) * forward) < EPS;
    }

    /**
     * Stops what the brain is walking to, as a camel told to sit by a test has nowhere to go. (A walk order
     * stands a sitting camel up again - 1.20's camel move control - which a test that sits it must rule out.)
     */
    private static void quiet(EntityCamel camel) throws ReflectiveOperationException {
        Object brain = ReflectionHelper.findField(EntityCamel.class, "brain").get(camel);
        ReflectionHelper.findField(brain.getClass(), "walkTarget").set(brain, null);
        camel.getNavigator().clearPath();
        camel.getMoveHelper().action = net.minecraft.entity.ai.EntityMoveHelper.Action.WAIT;
    }

    /** Runs camels for a number of ticks with the world clock advancing, as the server would. */
    private static void run(Check c, int ticks, EntityCamel... camels) {
        for (int i = 0; i < ticks; i++) {
            c.world.getWorldInfo().setWorldTotalTime(c.world.getTotalWorldTime() + 1);
            for (EntityCamel camel : camels) {
                if (!camel.isDead) {
                    // As World.updateEntityWithOptionalForce does around each entity's update.
                    camel.ticksExisted++;
                    camel.onUpdate();
                    for (net.minecraft.entity.Entity rider : camel.getPassengers()) {
                        camel.updatePassenger(rider);
                    }
                }
            }
        }
    }

}
