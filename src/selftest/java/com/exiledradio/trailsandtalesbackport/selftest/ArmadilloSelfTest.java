package com.exiledradio.trailsandtalesbackport.selftest;

import com.exiledradio.trailsandtalesbackport.armadillo.ArmadilloAdvancements;
import com.exiledradio.trailsandtalesbackport.armadillo.ArmadilloContent;
import com.exiledradio.trailsandtalesbackport.armadillo.ArmadilloSpawns;
import com.exiledradio.trailsandtalesbackport.armadillo.ArmadilloState;
import com.exiledradio.trailsandtalesbackport.armadillo.EntityArmadillo;
import com.exiledradio.trailsandtalesbackport.common.Content;
import com.exiledradio.trailsandtalesbackport.common.Owners;

import net.minecraft.advancements.Advancement;
import net.minecraft.block.BlockDirt;
import net.minecraft.block.BlockDispenser;
import net.minecraft.block.BlockSand;
import net.minecraft.block.BlockColored;
import net.minecraft.dispenser.IBehaviorDispenseItem;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIAvoidEntity;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntityCaveSpider;
import net.minecraft.entity.monster.EntityHusk;
import net.minecraft.entity.monster.EntitySpider;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.List;

/** Stream Ar: the armadillo and the armadillo scute. */
public final class ArmadilloSelfTest implements SelfTest {

    @Override
    public String name() {
        return "armadillo";
    }

    @Override
    public void run(Check c) {
        if (!c.ok(ArmadilloContent.scute != null, "armadillo stream registered")) {
            return;
        }
        registration(c);
        spawns(c);
        armadillo(c);
        feeding(c);
        scare(c);
        damage(c);
        shedding(c);
        brushing(c);
        dispenser(c);
        spiders(c);
        breeding(c);
        advancements(c);
    }

    private void registration(Check c) {
        c.eq(Content.id("armadillo_scute"), ArmadilloContent.scute.getRegistryName(), "scute name");
        c.eq("item.trailsandtalesbackport.armadillo_scute", ArmadilloContent.scute.getTranslationKey(), "scute key");
        c.eq(ArmadilloContent.scute, Owners.ours("armadillo_scute"), "the scute found by name");
        c.eq(64, ArmadilloContent.scute.getItemStackLimit(new ItemStack(ArmadilloContent.scute)), "scutes stack to 64");
        EntityEntry entry = ForgeRegistries.ENTITIES.getValue(Content.id("armadillo"));
        c.ok(entry != null && entry.getEntityClass() == EntityArmadillo.class, "entity registered");
        EntityRegistry.EntityRegistration reg = EntityRegistry.instance().lookupModSpawn(EntityArmadillo.class, false);
        c.ok(reg != null && reg.getModEntityId() == ArmadilloContent.ARMADILLO_ID, "entity id 15");
        EntityList.EntityEggInfo egg = entry == null ? null : entry.getEgg();
        c.ok(egg != null && egg.primaryColor == 11366765 && egg.secondaryColor == 8538184, "1.20.5's spawn egg colors");
        int sounds = 0;
        for (String e : new String[] {"ambient", "brush", "death", "eat", "hurt", "hurt_reduced", "land", "peek", "roll",
                "scute_drop", "step", "unroll_finish", "unroll_start"}) {
            sounds += ForgeRegistries.SOUND_EVENTS.containsKey(Content.id("entity.armadillo." + e)) ? 1 : 0;
        }
        c.eq(13, sounds, "13 sound events");
        for (ResourceLocation table : new ResourceLocation[] {EntityArmadillo.SHED_LOOT, EntityArmadillo.BRUSH_LOOT}) {
            List<ItemStack> loot = c.world.getLootTableManager().getLootTableFromLocation(table).generateLootForPools(c.world.rand,
                    new net.minecraft.world.storage.loot.LootContext.Builder(c.world).build());
            c.ok(loot.size() == 1 && loot.get(0).getItem() == ArmadilloContent.scute && loot.get(0).getCount() == 1,
                    table + " gives one scute");
        }
        c.ok(ArmadilloContent.isBrush(new ItemStack(ForgeRegistries.ITEMS.getValue(ArmadilloContent.BRUSH_ID))), "the brush is known by name");
        c.ok(!ArmadilloContent.isBrush(new ItemStack(Items.FEATHER)), "a feather is no brush");
    }

    private void spawns(Check c) {
        c.ok(spawn(Biomes.SAVANNA, 10, 2, 3), "savanna: 10, 2-3");
        c.ok(spawn(Biomes.SAVANNA_PLATEAU, 10, 2, 3), "savanna plateau");
        c.ok(spawn(Biomes.MUTATED_SAVANNA, 10, 2, 3), "shattered savanna (windswept savanna)");
        c.ok(spawn(Biomes.MUTATED_SAVANNA_ROCK, 10, 2, 3), "shattered savanna plateau");
        c.ok(spawn(Biomes.MESA, 6, 1, 2), "badlands: 6, 1-2");
        c.ok(spawn(Biomes.MESA_ROCK, 6, 1, 2), "wooded badlands");
        c.ok(spawn(Biomes.MESA_CLEAR_ROCK, 6, 1, 2), "badlands plateau");
        c.ok(spawn(Biomes.MUTATED_MESA, 6, 1, 2), "eroded badlands");
        c.ok(!spawn(Biomes.PLAINS, 10, 2, 3) && !spawn(Biomes.DESERT, 6, 1, 2), "not in plains or desert");
        c.ok(ArmadilloSpawns.forBiomePath("windswept_savanna") == ArmadilloSpawns.SAVANNA
                && ArmadilloSpawns.forBiomePath("wooded_badlands") == ArmadilloSpawns.BADLANDS, "1.18 biome names");
        // Twice adds once.
        ArmadilloSpawns.addSpawns(Biomes.SAVANNA);
        int n = 0;
        for (Biome.SpawnListEntry e : Biomes.SAVANNA.getSpawnableList(EnumCreatureType.CREATURE)) {
            n += e.entityClass == EntityArmadillo.class ? 1 : 0;
        }
        c.eq(1, n, "one savanna entry");

        // armadillo_spawnable_on: grass, the badlands terracottas, red sand, coarse dirt.
        c.ok(EntityArmadillo.isSpawnableOn(Blocks.GRASS.getDefaultState()), "spawns on grass");
        c.ok(EntityArmadillo.isSpawnableOn(Blocks.SAND.getDefaultState().withProperty(BlockSand.VARIANT, BlockSand.EnumType.RED_SAND)), "red sand");
        c.ok(!EntityArmadillo.isSpawnableOn(Blocks.SAND.getDefaultState()), "not sand");
        c.ok(EntityArmadillo.isSpawnableOn(Blocks.DIRT.getDefaultState().withProperty(BlockDirt.VARIANT, BlockDirt.DirtType.COARSE_DIRT)), "coarse dirt");
        c.ok(!EntityArmadillo.isSpawnableOn(Blocks.DIRT.getDefaultState()), "not dirt");
        c.ok(EntityArmadillo.isSpawnableOn(Blocks.HARDENED_CLAY.getDefaultState()), "terracotta");
        c.ok(EntityArmadillo.isSpawnableOn(Blocks.STAINED_HARDENED_CLAY.getDefaultState().withProperty(BlockColored.COLOR, EnumDyeColor.ORANGE)), "orange terracotta");
        c.ok(!EntityArmadillo.isSpawnableOn(Blocks.STAINED_HARDENED_CLAY.getDefaultState().withProperty(BlockColored.COLOR, EnumDyeColor.BLUE)), "not blue terracotta");
        c.ok(!EntityArmadillo.isSpawnableOn(Blocks.STONE.getDefaultState()), "not stone");

        BlockPos area = c.area();
        BlockPos at = area.add(4, 0, 4);
        c.world.setBlockState(at.down(), Blocks.SAND.getDefaultState().withProperty(BlockSand.VARIANT, BlockSand.EnumType.RED_SAND), 2);
        EntityArmadillo a = new EntityArmadillo(c.world);
        a.setLocationAndAngles(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0.0F, 0.0F);
        boolean lit = c.world.getLight(at) > 8;
        c.ok(!lit || a.getCanSpawnHere(), "may spawn on lit red sand");
        a.setLocationAndAngles(at.getX() + 6.5, at.getY(), at.getZ() + 0.5, 0.0F, 0.0F);
        c.ok(!a.getCanSpawnHere(), "not on stone");
    }

    private static boolean spawn(Biome biome, int weight, int min, int max) {
        for (Biome.SpawnListEntry e : biome.getSpawnableList(EnumCreatureType.CREATURE)) {
            if (e.entityClass == EntityArmadillo.class) {
                return e.itemWeight == weight && e.minGroupCount == min && e.maxGroupCount == max;
            }
        }
        return false;
    }

    private void armadillo(Check c) {
        BlockPos area = c.area();
        EntityArmadillo a = spawn(c, area.add(5, 0, 5));
        c.eq(12.0D, a.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).getBaseValue(), "12 health");
        c.ok(Math.abs(a.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getBaseValue() - 0.14D) < 1e-6, "speed 0.14");
        c.ok(Math.abs(a.width - 0.7F) < 1e-4 && Math.abs(a.height - 0.65F) < 1e-4, "0.7 x 0.65");
        c.ok(Math.abs(a.getEyeHeight() - 0.26F) < 1e-4, "eyes at 0.26");
        c.eq(ArmadilloState.IDLE, a.getState(), "starts idle");
        c.eq("ArmadilloLook", a.getLookHelper().getClass().getSimpleName(), "1.20's look control in place");
        c.eq(32, a.getMaxHeadYRot(), "head turns 32 degrees from the body");
        c.ok(a.scuteTime() >= 6000 && a.scuteTime() < 12000, "first scute in 5-10 minutes (" + a.scuteTime() + ")");
        c.ok(a.isBreedingItem(new ItemStack(Items.SPIDER_EYE)), "bred with spider eyes");
        c.ok(!a.isBreedingItem(new ItemStack(Items.WHEAT)), "not wheat");

        EntityArmadillo baby = spawn(c, area.add(10, 0, 5));
        baby.setGrowingAge(-24000);
        c.ok(Math.abs(baby.width - 0.42F) < 1e-4 && Math.abs(baby.height - 0.39F) < 1e-4, "a baby is 0.6 the size");
        c.ok(Math.abs(baby.getEyeHeight() - 0.21875F) < 1e-5, "a baby's eyes at 0.21875");

        // Saved and loaded: 1.20's "state" and "scute_time".
        a.switchToState(ArmadilloState.SCARED);
        a.setScuteTime(1234);
        NBTTagCompound tag = new NBTTagCompound();
        a.writeToNBT(tag);
        c.eq("scared", tag.getString("state"), "state saved by name");
        c.eq(1234, tag.getInteger("scute_time"), "scute time saved");
        EntityArmadillo copy = new EntityArmadillo(c.world);
        copy.readFromNBT(tag);
        c.eq(ArmadilloState.SCARED, copy.getState(), "state loaded");
        c.eq(1234, copy.scuteTime(), "scute time loaded");
        tag.setString("state", "nonsense");
        copy.readFromNBT(tag);
        c.eq(ArmadilloState.IDLE, copy.getState(), "an unknown state loads as idle");
        a.setDead();
        baby.setDead();
    }

    private void feeding(Check c) {
        BlockPos area = c.area();
        EntityArmadillo a = spawn(c, area.add(5, 0, 5));
        EntityPlayerMP player = Players.real(c.world, "armadillofeeder");
        player.setPosition(a.posX, a.posY, a.posZ - 2);
        player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.SPIDER_EYE, 3));
        c.ok(a.processInteract(player, EnumHand.MAIN_HAND), "an adult takes a spider eye");
        c.ok(a.isInLove(), "and is in love");
        c.eq(2, player.getHeldItemMainhand().getCount(), "one eye eaten");

        EntityArmadillo b = spawn(c, area.add(10, 0, 5));
        b.switchToState(ArmadilloState.SCARED);
        c.ok(!b.processInteract(player, EnumHand.MAIN_HAND), "rolled up, it takes nothing");
        c.ok(!b.isInLove(), "and is not in love");
        c.eq(2, player.getHeldItemMainhand().getCount(), "no eye eaten");

        // Rolling up forgets love.
        a.rollUp();
        c.ok(!a.isInLove(), "rolling up forgets love");
        c.eq(ArmadilloState.ROLLING, a.getState(), "rolling");
        a.setDead();
        b.setDead();
    }

    /** An undead mob nearby: rolls up, rolled up after 10 ticks, unrolls once the danger memory runs down. */
    private void scare(Check c) {
        BlockPos area = c.area();
        EntityArmadillo a = spawn(c, area.add(16, 0, 16));
        run(c, 5, a);
        c.ok(a.onGround, "on the ground");
        EntityHusk husk = new EntityHusk(c.world);
        husk.setNoAI(true);
        husk.setLocationAndAngles(a.posX + 3, a.posY, a.posZ, 0.0F, 0.0F);
        c.world.spawnEntity(husk);
        c.ok(a.isScaredBy(husk), "a husk 3 blocks away scares it");

        int t = 0;
        while (t < 60 && a.getState() == ArmadilloState.IDLE) {
            run(c, 1, a);
            t++;
        }
        c.eq(ArmadilloState.ROLLING, a.getState(), "rolls up within the scans (" + t + " ticks)");
        c.ok(a.dangerTicks() > 70, "danger remembered (" + a.dangerTicks() + ")");
        int r = 0;
        while (r < 30 && a.getState() == ArmadilloState.ROLLING) {
            run(c, 1, a);
            r++;
        }
        c.eq(ArmadilloState.SCARED, a.getState(), "rolled up");
        c.eq(11, r, "rolled up on the 11th tick after rolling up (in state more than 10)");
        run(c, 100, a);
        c.eq(ArmadilloState.SCARED, a.getState(), "stays rolled up while the husk is there");
        c.ok(a.shouldHideInShell(), "hidden in its shell");

        husk.setDead();
        c.world.loadedEntityList.remove(husk);
        int u = 0;
        while (u < 120 && a.getState() == ArmadilloState.SCARED) {
            run(c, 1, a);
            u++;
        }
        c.eq(ArmadilloState.UNROLLING, a.getState(), "starts unrolling with under 30 ticks of danger left (" + u + " ticks)");
        c.ok(a.dangerTicks() < 30 && a.dangerTicks() > 0, "danger left " + a.dangerTicks());
        int v = 0;
        while (v < 60 && a.getState() == ArmadilloState.UNROLLING) {
            run(c, 1, a);
            v++;
        }
        c.eq(ArmadilloState.IDLE, a.getState(), "unrolled when the danger is forgotten (" + v + " ticks)");
        c.ok(u + v >= 70 && u + v <= 90, "about 80 ticks after the husk went (" + (u + v) + ")");

        // Players: sprinting or riding scare it, walking does not; spectators never.
        EntityPlayerMP player = Players.real(c.world, "armadilloscarer");
        player.setPosition(a.posX + 4, a.posY, a.posZ);
        c.ok(!a.isScaredBy(player), "a walking player does not scare it");
        player.setSprinting(true);
        c.ok(a.isScaredBy(player), "a sprinting player does");
        player.setPosition(a.posX + 9, a.posY, a.posZ);
        c.ok(!a.isScaredBy(player), "not from 9 blocks");
        player.setPosition(a.posX + 4, a.posY + 4, a.posZ);
        c.ok(!a.isScaredBy(player), "not from 4 blocks above");
        player.setPosition(a.posX + 4, a.posY, a.posZ);
        player.setGameType(net.minecraft.world.GameType.SPECTATOR);
        c.ok(!a.isScaredBy(player), "never a spectator");
        a.setDead();
    }

    private void damage(Check c) {
        BlockPos area = c.area();
        EntityArmadillo a = spawn(c, area.add(16, 0, 16));
        run(c, 3, a);
        EntityHusk husk = new EntityHusk(c.world);
        husk.setLocationAndAngles(a.posX + 20, a.posY, a.posZ, 0.0F, 0.0F);
        a.attackEntityFrom(DamageSource.causeMobDamage(husk), 5.0F);
        c.ok(Math.abs(a.getHealth() - 7.0F) < 1e-4, "idle, takes the full 5 (" + a.getHealth() + ")");
        c.eq(ArmadilloState.ROLLING, a.getState(), "hurt by a mob it rolls up");
        c.ok(a.dangerTicks() == 80, "and remembers danger for 80 ticks");
        run(c, 15, a);
        a.hurtResistantTime = 0;
        a.attackEntityFrom(DamageSource.causeMobDamage(husk), 5.0F);
        c.ok(Math.abs(a.getHealth() - 5.0F) < 1e-4, "rolled up, takes (5 - 1) / 2 = 2 (" + a.getHealth() + ")");
        a.hurtResistantTime = 0;
        a.attackEntityFrom(DamageSource.IN_FIRE, 1.0F);
        c.eq(ArmadilloState.IDLE, a.getState(), "fire unrolls it");
        run(c, 25, a);
        c.ok(a.isPanicking(), "and it panics");
        c.ok(!a.canStayRolledUp(), "panicking, it cannot stay rolled up");
        a.setDead();
    }

    /** Current Java's shedding: an adult every 5-10 minutes; a baby's timer runs on but it sheds nothing. */
    private void shedding(Check c) {
        BlockPos area = c.area();
        EntityArmadillo a = spawn(c, area.add(5, 0, 5));
        a.setScuteTime(2);
        run(c, 1, a);
        c.eq(0, scutes(c, area), "nothing before its time");
        run(c, 1, a);
        c.eq(1, scutes(c, area), "one scute when the timer runs out");
        c.ok(a.scuteTime() >= 6000 && a.scuteTime() < 12000, "next in 5-10 minutes (" + a.scuteTime() + ")");

        EntityArmadillo baby = spawn(c, area.add(15, 0, 5));
        baby.setGrowingAge(-24000);
        baby.setScuteTime(1);
        run(c, 3, baby);
        c.eq(0, scutes(c, area), "a baby sheds none");
        c.ok(baby.scuteTime() < 0, "its timer runs on (" + baby.scuteTime() + ")");
        baby.setGrowingAge(0);
        run(c, 1, baby);
        c.eq(1, scutes(c, area), "grown up, it sheds at once");

        c.world.getGameRules().setOrCreateGameRule("doMobLoot", "false");
        a.setScuteTime(1);
        run(c, 2, a);
        c.eq(0, scutes(c, area), "no shedding without mob loot");
        c.world.getGameRules().setOrCreateGameRule("doMobLoot", "true");
        a.setDead();
        baby.setDead();
    }

    private void brushing(Check c) {
        Item brush = ForgeRegistries.ITEMS.getValue(ArmadilloContent.BRUSH_ID);
        if (!c.ok(brush != null, "a brush exists (the archaeology stream's or the test stand-in)")) {
            return;
        }
        BlockPos area = c.area();
        EntityArmadillo a = spawn(c, area.add(5, 0, 5));
        EntityPlayerMP player = Players.real(c.world, "armadillobrusher");
        player.setPosition(a.posX, a.posY, a.posZ - 2);
        player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(brush));
        c.ok(a.processInteract(player, EnumHand.MAIN_HAND), "brushing an adult works");
        c.eq(1, scutes(c, area), "one scute brushed off");
        c.eq(16, player.getHeldItemMainhand().getItemDamage(), "16 durability");
        Advancement scute = c.world.getMinecraftServer().getAdvancementManager().getAdvancement(ArmadilloAdvancements.BRUSH_ARMADILLO);
        c.ok(scute != null && player.getAdvancements().getProgress(scute).isDone(), "Isn't It Scute?");
        a.switchToState(ArmadilloState.SCARED);
        c.ok(a.processInteract(player, EnumHand.MAIN_HAND), "a rolled-up adult is brushed too");
        c.eq(1, scutes(c, area), "another scute");

        EntityArmadillo baby = spawn(c, area.add(12, 0, 5));
        baby.setGrowingAge(-24000);
        int damage = player.getHeldItemMainhand().getItemDamage();
        c.ok(!baby.processInteract(player, EnumHand.MAIN_HAND), "a baby cannot be brushed");
        c.eq(0, scutes(c, area), "no scute from a baby");
        c.eq(damage, player.getHeldItemMainhand().getItemDamage(), "and no wear");
        a.setDead();
        baby.setDead();
    }

    private void dispenser(Check c) {
        Item brush = ForgeRegistries.ITEMS.getValue(ArmadilloContent.BRUSH_ID);
        if (brush == null) {
            return;
        }
        BlockPos area = c.area();
        BlockPos at = area.add(5, 0, 5);
        c.world.setBlockState(at, Blocks.DISPENSER.getDefaultState().withProperty(BlockDispenser.FACING, EnumFacing.EAST), 2);
        IBehaviorDispenseItem behavior = BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.getObject(brush);
        net.minecraft.block.BlockSourceImpl source = new net.minecraft.block.BlockSourceImpl(c.world, at);
        ItemStack stack = new ItemStack(brush);
        ItemStack out = behavior.dispense(source, stack.copy());
        c.ok(out.getItem() == brush && out.getCount() == 1 && out.getItemDamage() == 0, "no armadillo: the brush stays, unused");
        c.eq(0, c.world.getEntitiesWithinAABB(EntityItem.class, new AxisAlignedBB(area, area.add(32, 4, 32))).size(), "nothing dropped");

        EntityArmadillo a = spawn(c, at.east());
        out = behavior.dispense(source, out);
        c.eq(1, scutes(c, area), "a dispenser brushes off a scute");
        c.eq(16, out.getItemDamage(), "16 durability");
        out.setItemDamage(brush.getMaxDamage() - 10);
        out = behavior.dispense(source, out);
        c.ok(out.isEmpty(), "a worn-out brush breaks");
        c.eq(1, scutes(c, area), "after brushing once more");
        a.setDead();
        c.world.setBlockToAir(at);
    }

    private void spiders(Check c) {
        BlockPos area = c.area();
        for (EntitySpider spider : new EntitySpider[] {new EntitySpider(c.world), new EntityCaveSpider(c.world)}) {
            spider.setLocationAndAngles(area.getX() + 5.5, area.getY(), area.getZ() + 5.5, 0.0F, 0.0F);
            c.world.spawnEntity(spider);
            boolean avoids = false;
            for (EntityAITasks.EntityAITaskEntry e : spider.tasks.taskEntries) {
                avoids |= e.action instanceof EntityAIAvoidEntity && e.priority == 2;
            }
            c.ok(avoids, spider.getClass().getSimpleName() + " avoids armadillos (priority 2)");
            spider.setDead();
        }
    }

    private void breeding(Check c) {
        BlockPos area = c.area();
        c.fill(area.add(0, -1, 0), area.add(31, -1, 31), Blocks.GRASS.getDefaultState());
        EntityArmadillo a = spawn(c, area.add(13, 0, 16));
        EntityArmadillo b = spawn(c, area.add(17, 0, 16));
        EntityPlayerMP player = Players.real(c.world, "armadillobreeder");
        player.setPosition(area.getX() + 15.5, area.getY(), area.getZ() + 2.5);
        for (EntityArmadillo x : new EntityArmadillo[] {a, b}) {
            player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.SPIDER_EYE));
            x.processInteract(player, EnumHand.MAIN_HAND);
        }
        c.ok(a.isInLove() && b.isInLove(), "both in love");
        int t = 0;
        List<EntityArmadillo> babies = null;
        while (t < 600) {
            run(c, 10, a, b);
            t += 10;
            babies = c.world.getEntitiesWithinAABB(EntityArmadillo.class, new AxisAlignedBB(area, area.add(32, 4, 32)),
                    x -> x != null && x.isChild() && !x.isDead);
            if (!babies.isEmpty()) {
                break;
            }
        }
        c.ok(babies != null && babies.size() == 1, "two armadillos in love have one baby (" + t + " ticks)");
        if (babies != null && babies.size() == 1) {
            c.ok(babies.get(0).getGrowingAge() <= -23990, "a baby for 24000 ticks");
            babies.get(0).setDead();
        }
        c.ok(a.getGrowingAge() > 5900 && b.getGrowingAge() > 5900, "parents wait 6000 ticks");
        c.ok(!a.isInLove() && !b.isInLove(), "no longer in love");
        a.setDead();
        b.setDead();
    }

    private void advancements(Check c) {
        ArmadilloAdvancements.check(c.world.getMinecraftServer());
        Advancement two = c.world.getMinecraftServer().getAdvancementManager().getAdvancement(ArmadilloAdvancements.TWO_BY_TWO);
        c.ok(two != null && two.getCriteria().containsKey(ArmadilloAdvancements.ARMADILLO_CRITERION), "Two by Two needs an armadillo");
        boolean required = false;
        if (two != null) {
            for (String[] req : two.getRequirements()) {
                required |= req.length == 1 && ArmadilloAdvancements.ARMADILLO_CRITERION.equals(req[0]);
            }
        }
        c.ok(required, "as a requirement of its own");
        Advancement scute = c.world.getMinecraftServer().getAdvancementManager().getAdvancement(ArmadilloAdvancements.BRUSH_ARMADILLO);
        c.ok(scute != null && scute.getParent() != null
                && new ResourceLocation("minecraft", "adventure/root").equals(scute.getParent().getId()), "Isn't It Scute? under Adventure");
    }

    // ------------------------------------------------------------------ helpers

    private static EntityArmadillo spawn(Check c, BlockPos at) {
        EntityArmadillo a = new EntityArmadillo(c.world);
        a.setLocationAndAngles(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0.0F, 0.0F);
        c.world.spawnEntity(a);
        return a;
    }

    /** @return scutes lying in the area, removing them */
    private static int scutes(Check c, BlockPos area) {
        int n = 0;
        for (EntityItem item : c.world.getEntitiesWithinAABB(EntityItem.class, new AxisAlignedBB(area.add(-2, -2, -2), area.add(34, 6, 34)))) {
            if (item.getItem().getItem() == ArmadilloContent.scute) {
                n += item.getItem().getCount();
            }
            item.setDead();
            c.world.removeEntityDangerously(item);
        }
        return n;
    }

    /** Runs entities for a number of ticks with the world clock advancing, as the server would. */
    private static void run(Check c, int ticks, EntityArmadillo... armadillos) {
        for (int i = 0; i < ticks; i++) {
            c.world.getWorldInfo().setWorldTotalTime(c.world.getTotalWorldTime() + 1);
            for (EntityArmadillo a : armadillos) {
                if (!a.isDead) {
                    // As World.updateEntityWithOptionalForce does around each entity's update.
                    a.lastTickPosX = a.posX;
                    a.lastTickPosY = a.posY;
                    a.lastTickPosZ = a.posZ;
                    a.ticksExisted++;
                    a.onUpdate();
                }
            }
        }
    }
}
