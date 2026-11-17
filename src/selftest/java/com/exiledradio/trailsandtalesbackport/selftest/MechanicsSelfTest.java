package com.exiledradio.trailsandtalesbackport.selftest;

import com.exiledradio.trailsandtalesbackport.ModConfig;
import com.exiledradio.trailsandtalesbackport.asm.Asm;
import com.exiledradio.trailsandtalesbackport.mechanics.MechanicsHooks;
import com.exiledradio.trailsandtalesbackport.mechanics.MeleeReach;
import com.exiledradio.trailsandtalesbackport.mechanics.SpawnChunks;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIAttackMelee;
import net.minecraft.entity.ai.EntityAIZombieAttack;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.monster.EntityIronGolem;
import net.minecraft.entity.monster.EntityMagmaCube;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/**
 * {@code -Pselftest=mechanics}: 1.20's mechanics changes. Melee reach (P8) through the patched
 * {@code checkAndPerformAttack} of real AIs: a wide golem no longer reaches 2.5 blocks, a zombie no longer hits through a
 * glass pane, an enderman now reaches a target standing 2 blocks up, a horse's rider is out of a baby zombie's reach;
 * shields facing (P9); spawn chunks (P10); slimes and Jump Boost (P11); chorus fruit and fall distance; named arrows
 * (P12); re-dyeing recipes. Each also with its setting off, where 1.12.2's answer differs.
 */
public final class MechanicsSelfTest implements SelfTest {

    @Override
    public String name() {
        return "mechanics";
    }

    @Override
    public void run(Check c) throws Exception {
        c.ok(applied("P8", "net.minecraft.entity.ai.EntityAIAttackMelee.checkAndPerformAttack"), "P8 is applied to EntityAIAttackMelee: " + Asm.APPLIED);
        melee(c);
        new net.minecraft.entity.monster.EntityPolarBear(c.world); // loads its melee AI class
        c.ok(applied("P8", "net.minecraft.entity.monster.EntityPolarBear$AIMeleeAttack.checkAndPerformAttack"), "P8 also reaches the polar bear's own checkAndPerformAttack");
        c.ok(applied("P9", "EntityLivingBase.canBlockDamageSource"), "P9 is applied");
        shields(c);
        c.ok(applied("P10", "World.isSpawnChunk"), "P10 is applied to World.isSpawnChunk");
        c.ok(applied("P10", "MinecraftServer.initialWorldChunkLoad"), "P10 is applied to initialWorldChunkLoad");
        spawnChunks(c);
        slimes(c);
        chorus(c);
        c.ok(applied("P12", "EntityArrow.onCollideWithPlayer"), "P12 is applied");
        arrows(c);
        redyeing(c);
    }

    private static boolean applied(String patch, String what) {
        return Asm.APPLIED.contains(patch + " " + what);
    }

    // ---- melee reach ------------------------------------------------------------------------------------------

    private static Method checkAndPerformAttack;
    private static Field attackTick;

    /** Runs the goal's own (patched) checkAndPerformAttack with 1.12.2's distance, as updateTask passes it. */
    private static boolean attacks(EntityAIAttackMelee goal, EntityCreature attacker, EntityLivingBase target) throws Exception {
        if (checkAndPerformAttack == null) {
            checkAndPerformAttack = ObfuscationReflectionHelper.findMethod(EntityAIAttackMelee.class, "func_190102_a", void.class,
                    EntityLivingBase.class, double.class);
        }
        if (attackTick == null) {
            attackTick = ObfuscationReflectionHelper.findField(EntityAIAttackMelee.class, "field_75439_d");
        }
        attackTick.setInt(goal, 0);
        attacker.getEntitySenses().clearSensingCache();
        float before = target.getHealth();
        double distSq = attacker.getDistanceSq(target.posX, target.getEntityBoundingBox().minY, target.posZ);
        checkAndPerformAttack.invoke(goal, target, distSq);
        return target.getHealth() < before;
    }

    private static EntityVillager villager(Check c, double x, double y, double z) {
        EntityVillager v = new EntityVillager(c.world);
        v.setPosition(x, y, z);
        return v;
    }

    private static void melee(Check c) throws Exception {
        BlockPos a = c.area().up();
        double x = a.getX() + 8.5D;
        double y = a.getY();
        double z = a.getZ() + 8.5D;
        boolean was = ModConfig.mechanics.meleeReach;
        try {
            for (boolean on : new boolean[]{true, false}) {
                ModConfig.mechanics.meleeReach = on;
                String mode = on ? "1.20.2: " : "off (1.12.2): ";

                EntityZombie zombie = new EntityZombie(c.world);
                zombie.setPosition(x, y, z);
                EntityAIZombieAttack zombieGoal = new EntityAIZombieAttack(zombie, 1.0D, false);
                c.ok(attacks(zombieGoal, zombie, villager(c, x + 1.0D, y, z)), mode + "a zombie hits a villager one block away");

                // A glass pane's post between them (0.8 apart): in reach either way, but out of sight.
                BlockPos pane = new BlockPos(x + 2.0D, y, z);
                c.fill(pane, pane.up(), Blocks.GLASS_PANE.getDefaultState());
                double px = pane.getX();
                zombie.setPosition(px + 0.1D, y, pane.getZ() + 0.5D);
                c.eq(!on, attacks(zombieGoal, zombie, villager(c, px + 0.9D, y, pane.getZ() + 0.5D)),
                        mode + "a zombie hits through a glass pane");
                c.fill(pane, pane.up(), Blocks.AIR.getDefaultState());

                EntityIronGolem golem = new EntityIronGolem(c.world);
                golem.setPosition(x, y, z + 6.0D);
                EntityAIAttackMelee golemGoal = new EntityAIAttackMelee(golem, 1.0D, true);
                c.eq(!on, attacks(golemGoal, golem, villager(c, x + 2.5D, y, z + 6.0D)),
                        mode + "an iron golem (1.4 wide) hits a villager 2.5 blocks away");
                c.ok(attacks(golemGoal, golem, villager(c, x + 1.7D, y, z + 6.0D)), mode + "and one 1.7 blocks away");

                EntityEnderman enderman = new EntityEnderman(c.world);
                enderman.setPosition(x, y, z + 12.0D);
                EntityAIAttackMelee endermanGoal = new EntityAIAttackMelee(enderman, 1.0D, false);
                c.eq(on, attacks(endermanGoal, enderman, villager(c, x + 0.5D, y + 2.0D, z + 12.0D)),
                        mode + "an enderman (2.9 tall) hits a villager standing 2 blocks up");
                c.ok(!attacks(endermanGoal, enderman, villager(c, x + 0.5D, y + 3.0D, z + 12.0D)),
                        mode + "but not one standing 3 blocks up");
            }
        } finally {
            ModConfig.mechanics.meleeReach = was;
        }

        // Riders: the hitbox starts at the seat.
        EntityHorse horse = new EntityHorse(c.world);
        horse.setPosition(x, y, z + 18.0D);
        EntityVillager rider = villager(c, x, y, z + 18.0D);
        rider.startRiding(horse, true);
        horse.updatePassenger(rider);
        double seat = horse.posY + horse.getMountedYOffset();
        c.ok(Math.abs(MeleeReach.hitbox(rider).minY - seat) < 1.0E-9D, "a rider's hitbox starts at the seat (" + seat + ")");
        EntityZombie baby = new EntityZombie(c.world);
        baby.setChild(true);
        baby.setPosition(x + 0.5D, y, z + 18.0D);
        c.ok(!MeleeReach.withinRange(baby, rider), "a baby zombie cannot reach a horse's rider");
        EntityZombie adult = new EntityZombie(c.world);
        adult.setPosition(x + 0.5D, y, z + 18.0D);
        c.ok(MeleeReach.withinRange(adult, rider), "an adult zombie still can");
        c.ok(MeleeReach.withinRange(adult, villager(c, x + 0.5D, y + 1.9D, z + 18.0D)) && !MeleeReach.withinRange(adult, villager(c, x + 0.5D, y + 1.96D, z + 18.0D)),
                "a zombie reaches exactly its own height up");
        c.ok(Math.abs(MeleeReach.REACH - 0.828285D) < 1.0E-5D, "the side reach is sqrt(2.04) - 0.6: " + MeleeReach.REACH);
        rider.dismountRidingEntity();
    }

    // ---- shields ----------------------------------------------------------------------------------------------

    private static void shields(Check c) throws Exception {
        BlockPos a = c.area().up();
        EntityPlayerMP player = Players.real(c.world, "shield");
        player.setPosition(a.getX() + 8.5D, a.getY(), a.getZ() + 8.5D);
        player.setHeldItem(EnumHand.OFF_HAND, new ItemStack(Items.SHIELD));
        player.setActiveHand(EnumHand.OFF_HAND);
        Field count = ObfuscationReflectionHelper.findField(EntityLivingBase.class, "field_184628_bn");
        count.setInt(player, player.getActiveItemStack().getMaxItemUseDuration() - 10);
        c.ok(player.isActiveItemStackBlocking(), "the test player has its shield raised");
        Method canBlock = ObfuscationReflectionHelper.findMethod(EntityLivingBase.class, "func_184583_d", boolean.class, DamageSource.class);

        EntityZombie front = new EntityZombie(c.world);
        front.setPosition(player.posX, player.posY, player.posZ + 2.0D);
        EntityZombie behind = new EntityZombie(c.world);
        behind.setPosition(player.posX, player.posY, player.posZ - 2.0D);
        boolean was = ModConfig.mechanics.shieldPitch;
        try {
            ModConfig.mechanics.shieldPitch = true;
            for (float pitch : new float[]{-90.0F, 0.0F, 90.0F}) {
                player.rotationYaw = player.rotationYawHead = 0.0F; // facing +z
                player.rotationPitch = pitch;
                c.ok((Boolean) canBlock.invoke(player, DamageSource.causeMobDamage(front)), "1.20.3: pitch " + pitch + " blocks a zombie in front");
                c.ok(!(Boolean) canBlock.invoke(player, DamageSource.causeMobDamage(behind)), "1.20.3: pitch " + pitch + " does not block one behind");
            }
            ModConfig.mechanics.shieldPitch = false;
            player.rotationPitch = 0.0F;
            c.ok((Boolean) canBlock.invoke(player, DamageSource.causeMobDamage(front)), "off: looking ahead still blocks");
            c.eq(player.getLook(1.0F), MechanicsHooks.shieldLook(player, 1.0F), "off: the hook gives 1.12.2's look vector");
        } finally {
            ModConfig.mechanics.shieldPitch = was;
            player.resetActiveHand();
        }
    }

    // ---- spawn chunks -----------------------------------------------------------------------------------------

    private static void spawnChunks(Check c) {
        BlockPos spawn = c.world.getSpawnPoint();
        int sx = spawn.getX() >> 4;
        int sz = spawn.getZ() >> 4;
        boolean was = ModConfig.mechanics.spawnChunks;
        int radius = SpawnChunks.radius;
        try {
            ModConfig.mechanics.spawnChunks = true;
            c.eq(2, SpawnChunks.radius, "the spawn chunk radius is 2");
            c.ok(c.world.isSpawnChunk(sx, sz) && c.world.isSpawnChunk(sx + 2, sz - 2), "1.20.5: the spawn chunk and one 2 away are kept");
            c.ok(!c.world.isSpawnChunk(sx + 3, sz) && !c.world.isSpawnChunk(sx, sz - 3), "1.20.5: chunks 3 away are not");
            c.ok(!c.world.provider.canDropChunk(sx, sz) && c.world.provider.canDropChunk(sx + 5, sz), "the overworld drops chunks 5 away from spawn");
            c.eq(32, SpawnChunks.prepareBlocks(), "the start area reaches 2 chunks");
            c.eq(25, SpawnChunks.prepareCount(), "and is 25 chunks");
            SpawnChunks.radius = 0;
            c.ok(!c.world.isSpawnChunk(sx, sz), "radius 0 (1.21.9, for Tricky Trials) keeps none");
            c.ok(SpawnChunks.prepareStart() > SpawnChunks.prepareBlocks(), "and prepares none");
            SpawnChunks.radius = radius;
            ModConfig.mechanics.spawnChunks = false;
            c.ok(c.world.isSpawnChunk(sx + 7, sz - 7) && !c.world.isSpawnChunk(sx + 10, sz), "off: 1.12.2's 128-block area");
            c.eq(192, SpawnChunks.prepareBlocks(), "off: 1.12.2's start area");
        } finally {
            ModConfig.mechanics.spawnChunks = was;
            SpawnChunks.radius = radius;
        }
    }

    // ---- slimes -----------------------------------------------------------------------------------------------

    private static void slimes(Check c) throws Exception {
        c.ok(applied("P11", "EntitySlime.jump") && applied("P11", "EntityMagmaCube.jump"), "P11 is applied to both");
        Method jump = ObfuscationReflectionHelper.findMethod(EntityLivingBase.class, "func_70664_aZ", void.class);
        EntitySlime slime = new EntitySlime(c.world);
        jump.invoke(slime);
        c.ok(Math.abs(slime.motionY - 0.41999998688697815D) < 1.0E-9D, "a slime without Jump Boost jumps at 0.42: " + slime.motionY);
        slime.addPotionEffect(new PotionEffect(MobEffects.JUMP_BOOST, 200, 1));
        jump.invoke(slime);
        c.ok(Math.abs(slime.motionY - (0.41999998688697815D + 0.2F)) < 1.0E-6D, "with Jump Boost II at 0.62: " + slime.motionY);
        EntityMagmaCube cube = new EntityMagmaCube(c.world);
        cube.addPotionEffect(new PotionEffect(MobEffects.JUMP_BOOST, 200, 0));
        double base = (double) (0.42F + (float) cube.getSlimeSize() * 0.1F);
        jump.invoke(cube);
        c.ok(Math.abs(cube.motionY - (base + 0.1F)) < 1.0E-6D, "a magma cube with Jump Boost I: " + cube.motionY + " (base " + base + ")");
        boolean was = ModConfig.mechanics.slimeJumpBoost;
        try {
            ModConfig.mechanics.slimeJumpBoost = false;
            jump.invoke(slime);
            c.ok(Math.abs(slime.motionY - 0.41999998688697815D) < 1.0E-9D, "off: Jump Boost does nothing to slimes");
        } finally {
            ModConfig.mechanics.slimeJumpBoost = was;
        }
    }

    // ---- chorus fruit -----------------------------------------------------------------------------------------

    private static void chorus(Check c) {
        BlockPos a = c.area().up();
        for (boolean on : new boolean[]{true, false}) {
            boolean was = ModConfig.mechanics.chorusFallDistance;
            ModConfig.mechanics.chorusFallDistance = on;
            try {
                EntityPlayerMP player = Players.real(c.world, "chorus" + on);
                player.setPosition(a.getX() + 16.5D, a.getY() + 3, a.getZ() + 16.5D);
                player.fallDistance = 10.0F;
                ItemStack fruit = new ItemStack(Items.CHORUS_FRUIT);
                double x0 = player.posX;
                double y0 = player.posY;
                double z0 = player.posZ;
                boolean moved = false;
                for (int i = 0; i < 10 && !moved; i++) {
                    MinecraftForge.EVENT_BUS.post(new LivingEntityUseItemEvent.Tick(player, fruit, 1));
                    ItemStack result = fruit.copy().onItemUseFinish(c.world, player);
                    MinecraftForge.EVENT_BUS.post(new LivingEntityUseItemEvent.Finish(player, fruit, 0, result));
                    moved = player.getDistanceSq(x0, y0, z0) > 1.0E-7D;
                }
                c.ok(moved, "chorus fruit teleported the test player");
                c.eq(on ? 0.0F : 10.0F, player.fallDistance, (on ? "1.20" : "off") + ": fall distance after the teleport");
            } finally {
                ModConfig.mechanics.chorusFallDistance = was;
            }
        }
    }

    // ---- named arrows -----------------------------------------------------------------------------------------

    private static void arrows(Check c) throws Exception {
        BlockPos a = c.area().up();
        EntityPlayerMP player = Players.real(c.world, "archer");
        player.setPosition(a.getX() + 8.5D, a.getY(), a.getZ() + 8.5D);
        ItemStack named = new ItemStack(Items.ARROW, 4);
        named.setStackDisplayName("Pointy");
        player.inventory.setInventorySlotContents(9, named);
        ItemStack bow = new ItemStack(Items.BOW);
        player.setHeldItem(EnumHand.MAIN_HAND, bow);
        player.setActiveHand(EnumHand.MAIN_HAND);
        c.ok(player.isHandActive(), "the archer draws the bow");
        // Released as if drawn for a second: stopActiveHand hands the time left to onPlayerStoppedUsing.
        ObfuscationReflectionHelper.findField(EntityLivingBase.class, "field_184628_bn").setInt(player, Items.BOW.getMaxItemUseDuration(bow) - 20);
        player.stopActiveHand();
        List<EntityArrow> shot = c.world.getEntitiesWithinAABB(EntityArrow.class, player.getEntityBoundingBox().grow(8.0D));
        EntityArrow arrow = shot.isEmpty() ? null : shot.get(0);
        if (!c.ok(arrow != null, "the bow shot an arrow")) {
            return;
        }
        c.eq("Pointy", arrow.getCustomNameTag(), "1.20.3: the arrow takes the item's name");
        c.eq(3, player.inventory.getStackInSlot(9).getCount(), "one arrow was used");

        Field inGround = ObfuscationReflectionHelper.findField(EntityArrow.class, "field_70254_i");
        inGround.setBoolean(arrow, true);
        arrow.arrowShake = 0;
        arrow.pickupStatus = EntityArrow.PickupStatus.ALLOWED;
        player.inventory.setInventorySlotContents(9, ItemStack.EMPTY);
        arrow.onCollideWithPlayer(player);
        ItemStack back = ItemStack.EMPTY;
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            if (player.inventory.getStackInSlot(i).getItem() == Items.ARROW) {
                back = player.inventory.getStackInSlot(i);
            }
        }
        c.ok(!back.isEmpty() && "Pointy".equals(back.getDisplayName()), "picking it up gives the named arrow back: " + back);
        c.ok(arrow.isDead, "and the arrow is gone");
        for (EntityArrow e : shot) {
            e.setDead();
        }

        EntityArrow plain = new net.minecraft.entity.projectile.EntityTippedArrow(c.world, player);
        c.ok(MechanicsHooks.arrowPickup(new ItemStack(Items.ARROW), plain).getItem() == Items.ARROW
                && !MechanicsHooks.arrowPickup(new ItemStack(Items.ARROW), plain).hasDisplayName(), "an arrow of a plain item gives a plain arrow");
    }

    // ---- re-dyeing --------------------------------------------------------------------------------------------

    private static void redyeing(Check c) {
        Item wool = Item.getItemFromBlock(Blocks.WOOL);
        Item carpet = Item.getItemFromBlock(Blocks.CARPET);
        ItemStack redDye = new ItemStack(Items.DYE, 1, EnumDyeColor.RED.getDyeDamage());
        ItemStack blueWool = new ItemStack(wool, 1, EnumDyeColor.BLUE.getMetadata());
        Crafting.expect(c, Crafting.grid(2, 1, redDye, blueWool), new ItemStack(wool, 1, EnumDyeColor.RED.getMetadata()),
                "red dye + blue wool gives red wool");
        Crafting.expect(c, Crafting.grid(2, 1, redDye, new ItemStack(Items.BED, 1, EnumDyeColor.LIME.getMetadata())),
                new ItemStack(Items.BED, 1, EnumDyeColor.RED.getMetadata()), "red dye + lime bed gives a red bed");
        ItemStack g = new ItemStack(carpet, 1, EnumDyeColor.GREEN.getMetadata());
        ItemStack y = new ItemStack(carpet, 1, EnumDyeColor.YELLOW.getMetadata());
        ItemStack black = new ItemStack(Items.DYE, 1, EnumDyeColor.BLACK.getDyeDamage());
        Crafting.expect(c, Crafting.grid(3, 3, g, y, g, y, black, y, g, y, g),
                new ItemStack(carpet, 8, EnumDyeColor.BLACK.getMetadata()), "black dye in eight mixed carpets gives eight black carpets");
        ItemStack redWool = new ItemStack(wool, 1, EnumDyeColor.RED.getMetadata());
        ItemStack none = net.minecraft.item.crafting.CraftingManager.findMatchingResult(Crafting.grid(2, 1, redDye, redWool), c.world);
        c.ok(none.isEmpty(), "red dye + red wool is no recipe (1.20 leaves the color out): " + none);
        Crafting.expect(c, Crafting.grid(2, 1, redDye, new ItemStack(wool, 1, EnumDyeColor.WHITE.getMetadata())), redWool,
                "white wool still dyes");
    }
}
