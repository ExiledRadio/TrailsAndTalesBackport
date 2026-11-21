package com.exiledradio.trailsandtalesbackport.selftest;

import com.exiledradio.trailsandtalesbackport.ModConfig;
import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import com.exiledradio.trailsandtalesbackport.common.Owners;
import com.exiledradio.trailsandtalesbackport.sniffer.BlockPitcherCrop;
import com.exiledradio.trailsandtalesbackport.sniffer.BlockSnifferEgg;
import com.exiledradio.trailsandtalesbackport.sniffer.BlockTorchflower;
import com.exiledradio.trailsandtalesbackport.sniffer.BlockTorchflowerCrop;
import com.exiledradio.trailsandtalesbackport.sniffer.EntitySniffer;
import com.exiledradio.trailsandtalesbackport.sniffer.Half;
import com.exiledradio.trailsandtalesbackport.sniffer.SnifferAdvancements;
import com.exiledradio.trailsandtalesbackport.sniffer.SnifferContent;
import com.exiledradio.trailsandtalesbackport.sniffer.SnifferState;

import net.minecraft.advancements.Advancement;
import net.minecraft.block.BlockDirt;
import net.minecraft.block.BlockFarmland;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.passive.EntityChicken;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** Stream N: the sniffer, sniffer eggs, torchflowers and pitcher plants. */
public final class SnifferSelfTest implements SelfTest {

    @Override
    public String name() {
        return "sniffer";
    }

    @Override
    public void run(Check c) {
        if (!c.ok(SnifferContent.snifferEgg != null && SnifferContent.torchflowerSeeds != null, "sniffer stream registered")) {
            return;
        }
        registration(c);
        recipes(c);
        torchflowerCrop(c);
        torchflowerLight(c);
        pitcherCrop(c);
        pitcherPlant(c);
        egg(c);
        sniffer(c);
        breeding(c);
        digging(c);
        advancements(c);
        family(c);
    }

    /** With family mods in eclipse/mods: Caves & Cliffs' moss, Buzzy Bees' flowers. */
    private void family(Check c) {
        net.minecraft.block.Block moss = Owners.theirBlock(Owners.CAVES_AND_CLIFFS, "moss_block");
        if (moss != null) {
            c.ok(SnifferContent.isDiggable(moss.getDefaultState()), "sniffers dig Caves & Cliffs' moss");
            c.ok(SnifferContent.isHatchBoost(moss.getDefaultState()), "moss speeds hatching");
            net.minecraft.block.Block rooted = Owners.theirBlock(Owners.CAVES_AND_CLIFFS, "rooted_dirt");
            c.ok(rooted == null || SnifferContent.isDiggable(rooted.getDefaultState()), "and rooted dirt");
            BlockPos area = c.area();
            BlockPos at = area.add(4, 0, 4);
            c.world.setBlockState(at.down(), moss.getDefaultState(), 2);
            c.world.setBlockState(at, SnifferContent.snifferEgg.getDefaultState(), 3);
            c.ok(BlockSnifferEgg.hatchBoost(c.world, at), "an egg on moss is boosted");
            c.world.setBlockToAir(at);
        } else {
            TrailsAndTalesBackport.LOGGER.info("SELFTEST sniffer: Caves & Cliffs absent, moss not tested");
        }
        if (Owners.present(Owners.BUZZY_BEES)) {
            try {
                Class<?> api = Class.forName("com.exiledradio.buzzybeesbackport.api.BeeFlowers");
                java.lang.reflect.Method isFlower = api.getMethod("isFlower", IBlockState.class);
                java.lang.reflect.Method isFlowerItem = api.getMethod("isFlowerItem", ItemStack.class);
                c.ok((Boolean) isFlower.invoke(null, SnifferContent.torchflower.getDefaultState()), "torchflowers are flowers to bees");
                c.ok((Boolean) isFlower.invoke(null, SnifferContent.pitcherPlant.getDefaultState()), "pitcher plants are flowers to bees");
                c.ok((Boolean) isFlowerItem.invoke(null, new ItemStack(SnifferContent.torchflowerItem)), "and their items breed bees");
            } catch (ReflectiveOperationException e) {
                c.fail("Buzzy Bees' BeeFlowers: " + e);
            }
        } else {
            TrailsAndTalesBackport.LOGGER.info("SELFTEST sniffer: Buzzy Bees absent, bee flowers not tested");
        }
    }

    private void registration(Check c) {
        c.eq(SnifferContent.eggItem, Owners.ours("sniffer_egg"), "the egg item is found by name for the archaeology loot");
        c.eq(EnumRarity.UNCOMMON, SnifferContent.eggItem.getRarity(new ItemStack(SnifferContent.eggItem)), "the egg is uncommon");
        c.ok(EntityList.isRegistered(new ResourceLocation("trailsandtalesbackport:sniffer")), "sniffer entity registered");
        c.ok(EntityList.ENTITY_EGGS.containsKey(new ResourceLocation("trailsandtalesbackport:sniffer")), "sniffer spawn egg");
        c.eq(8855049, EntityList.ENTITY_EGGS.get(new ResourceLocation("trailsandtalesbackport:sniffer")).primaryColor, "spawn egg color");
        for (String name : new String[] {"torchflower", "potted_torchflower", "torchflower_crop", "pitcher_crop", "pitcher_plant", "sniffer_egg"}) {
            c.ok(Owners.ourBlock(name) != null, "block " + name);
        }
        EntityChicken chicken = new EntityChicken(c.world);
        c.ok(chicken.isBreedingItem(new ItemStack(SnifferContent.torchflowerSeeds)), "chickens eat torchflower seeds (1.20)");
        c.ok(chicken.isBreedingItem(new ItemStack(SnifferContent.pitcherPod)), "chickens eat pitcher pods (1.20)");
        Set<Item> parrot = ObfuscationReflectionHelper.getPrivateValue(net.minecraft.entity.passive.EntityParrot.class, null, "field_192016_bJ");
        c.ok(parrot.contains(SnifferContent.torchflowerSeeds) && parrot.contains(SnifferContent.pitcherPod), "parrots are tamed with both (1.20)");
    }

    private void recipes(Check c) {
        Crafting.expect(c, Crafting.grid(1, 1, new ItemStack(SnifferContent.torchflowerItem)),
                new ItemStack(Items.DYE, 1, EnumDyeColor.ORANGE.getDyeDamage()), "torchflower -> orange dye");
        Crafting.expect(c, Crafting.grid(1, 1, new ItemStack(SnifferContent.pitcherPlantItem)),
                new ItemStack(Items.DYE, 2, EnumDyeColor.CYAN.getDyeDamage()), "pitcher plant -> 2 cyan dye");
    }

    private BlockPos farmland(Check c, BlockPos at) {
        c.world.setBlockState(at, Blocks.FARMLAND.getDefaultState().withProperty(BlockFarmland.MOISTURE, 7), 2);
        return at;
    }

    /** Not vanilla (sniffer.torchflowerLight): torchflowers and their pot give off light, none with the setting off. */
    private void torchflowerLight(Check c) {
        BlockPos area = c.area();
        BlockPos at = area.add(4, 0, 4);
        // World.checkLight works only with every chunk 17 blocks round loaded.
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                c.world.getChunkProvider().provideChunk((at.getX() >> 4) + dx, (at.getZ() >> 4) + dz);
            }
        }
        c.world.setBlockState(at.down(), Blocks.GRASS.getDefaultState());
        c.world.setBlockState(at, SnifferContent.torchflower.getDefaultState());
        c.eq(BlockTorchflower.LIGHT, c.world.getLightFor(EnumSkyBlock.BLOCK, at), "light: a torchflower glows");
        c.eq(BlockTorchflower.LIGHT - 1, c.world.getLightFor(EnumSkyBlock.BLOCK, at.north()), "light: it spreads to the next block");
        c.world.setBlockToAir(at);
        c.eq(0, c.world.getLightFor(EnumSkyBlock.BLOCK, at), "light: gone with the flower");
        c.world.setBlockState(at, SnifferContent.pottedTorchflower.getDefaultState());
        c.eq(BlockTorchflower.LIGHT, c.world.getLightFor(EnumSkyBlock.BLOCK, at), "light: a potted torchflower glows");
        c.world.setBlockToAir(at);
        ModConfig.sniffer.torchflowerLight = false;
        try {
            c.world.setBlockState(at, SnifferContent.torchflower.getDefaultState());
            c.eq(0, c.world.getLightFor(EnumSkyBlock.BLOCK, at), "light: setting off, a torchflower gives none");
        } finally {
            ModConfig.sniffer.torchflowerLight = true;
        }
        c.world.setBlockToAir(at);
    }

    private void torchflowerCrop(Check c) {
        BlockPos area = c.area();
        BlockPos soil = farmland(c, area.add(5, -1, 5));
        EntityPlayerMP player = Players.real(c.world, "planter");
        player.setPosition(soil.getX() + 0.5, soil.getY() + 1, soil.getZ() - 2.5);
        ItemStack seeds = new ItemStack(SnifferContent.torchflowerSeeds, 4);
        player.setHeldItem(EnumHand.MAIN_HAND, seeds);
        c.world.setBlockState(area.add(8, -1, 8), Blocks.GRASS.getDefaultState());
        seeds.onItemUse(player, c.world, area.add(8, -1, 8), EnumHand.MAIN_HAND, EnumFacing.UP, 0.5F, 1.0F, 0.5F);
        c.ok(c.world.isAirBlock(area.add(8, 0, 8)), "torchflower seeds do not plant on grass");
        seeds.onItemUse(player, c.world, soil, EnumHand.MAIN_HAND, EnumFacing.UP, 0.5F, 1.0F, 0.5F);
        BlockPos at = soil.up();
        IBlockState s = c.world.getBlockState(at);
        c.eq(SnifferContent.torchflowerCrop, s.getBlock(), "seeds plant a torchflower crop on farmland");
        c.eq(3, seeds.getCount(), "one seed used");
        c.eq(0, s.getValue(BlockTorchflowerCrop.AGE), "at age 0");
        c.eq(new AxisAlignedBB(5 / 16.0, 0, 5 / 16.0, 11 / 16.0, 6 / 16.0, 11 / 16.0), s.getBoundingBox(c.world, at), "age 0 shape");
        Advancement past = c.world.getMinecraftServer().getAdvancementManager().getAdvancement(new ResourceLocation("trailsandtalesbackport:husbandry/plant_any_sniffer_seed"));
        c.ok(past != null && player.getAdvancements().getProgress(past).getCriterionProgress("torchflower").isObtained(),
                "planting it counts for Planting the Past");

        Random rand = new Random(1);
        SnifferContent.torchflowerCrop.grow(c.world, rand, at, c.world.getBlockState(at));
        c.eq(1, c.world.getBlockState(at).getValue(BlockTorchflowerCrop.AGE), "bone meal: one age");
        SnifferContent.torchflowerCrop.grow(c.world, rand, at, c.world.getBlockState(at));
        c.eq(SnifferContent.torchflower, c.world.getBlockState(at).getBlock(), "bone meal at age 1: a torchflower");
        c.world.destroyBlock(at, true);
        c.eq(1, Drops.count(c.world, at, SnifferContent.torchflowerItem), "the torchflower drops itself");

        c.world.setBlockState(at, SnifferContent.torchflowerCrop.getDefaultState().withProperty(BlockTorchflowerCrop.AGE, 1), 2);
        c.world.destroyBlock(at, true);
        c.eq(1, Drops.count(c.world, at, SnifferContent.torchflowerSeeds), "a crop drops one seed");

        // Random ticks: two in three do nothing; at light 15 on wet farmland it still grows within a few hundred.
        c.world.setBlockState(at, SnifferContent.torchflowerCrop.getDefaultState(), 2);
        int ticks = 0;
        while (c.world.getBlockState(at).getBlock() == SnifferContent.torchflowerCrop && ticks < 2000) {
            SnifferContent.torchflowerCrop.updateTick(c.world, at, c.world.getBlockState(at), rand);
            ticks++;
        }
        c.eq(SnifferContent.torchflower, c.world.getBlockState(at).getBlock(), "random ticks grow it into a torchflower");
        c.ok(ticks > 4 && ticks < 2000, "in a plausible number of random ticks (" + ticks + ")");

        // A torchflower is a small flower: it is placed on grass or dirt.
        BlockPos grass = area.add(12, -1, 12);
        c.world.setBlockState(grass, Blocks.GRASS.getDefaultState());
        c.ok(SnifferContent.torchflower.canPlaceBlockAt(c.world, grass.up()), "torchflowers are planted on grass");
        c.ok(!SnifferContent.torchflowerCrop.canPlaceBlockAt(c.world, grass.up()), "the crop is not");
    }

    private void pitcherCrop(Check c) {
        BlockPos area = c.area();
        BlockPos soil = farmland(c, area.add(5, -1, 5));
        EntityPlayerMP player = Players.real(c.world, "potter");
        player.setPosition(soil.getX() + 0.5, soil.getY() + 1, soil.getZ() - 2.5);
        ItemStack pods = new ItemStack(SnifferContent.pitcherPod, 4);
        player.setHeldItem(EnumHand.MAIN_HAND, pods);
        pods.onItemUse(player, c.world, soil, EnumHand.MAIN_HAND, EnumFacing.UP, 0.5F, 1.0F, 0.5F);
        BlockPos at = soil.up();
        BlockPitcherCrop crop = SnifferContent.pitcherCrop;
        IBlockState s = c.world.getBlockState(at);
        c.eq(crop, s.getBlock(), "a pod plants a pitcher crop");
        c.eq(Half.LOWER, s.getValue(Half.PROPERTY), "its lower half");
        c.ok(s.getCollisionBoundingBox(c.world, at) != null, "age 0 has the bulb's collision");
        Advancement past = c.world.getMinecraftServer().getAdvancementManager().getAdvancement(new ResourceLocation("trailsandtalesbackport:husbandry/plant_any_sniffer_seed"));
        c.ok(past != null && player.getAdvancements().getProgress(past).isDone(), "a pod alone earns Planting the Past");

        Random rand = new Random(2);
        for (int age = 1; age <= 4; age++) {
            c.ok(crop.canGrow(c.world, at, c.world.getBlockState(at), false), "bone meal can grow age " + (age - 1));
            crop.grow(c.world, rand, at, c.world.getBlockState(at));
            c.eq(age, c.world.getBlockState(at).getValue(BlockPitcherCrop.AGE), "bone meal: age " + age);
            IBlockState up = c.world.getBlockState(at.up());
            if (age >= 3) {
                c.ok(up.getBlock() == crop && up.getValue(Half.PROPERTY) == Half.UPPER && up.getValue(BlockPitcherCrop.AGE) == age,
                        "age " + age + " has its upper half");
            } else {
                c.ok(c.world.isAirBlock(at.up()), "age " + age + " is one block tall");
            }
        }
        c.ok(!crop.canGrow(c.world, at.up(), c.world.getBlockState(at.up()), false), "age 4 grows no more (asked from the top)");
        c.ok(c.world.getBlockState(at.up()).getCollisionBoundingBox(c.world, at.up()) == null, "the upper half has no collision");

        // Breaking the upper half breaks the lower, which drops the plant once.
        EntityPlayerMP breaker = Players.real(c.world, "harvester");
        breaker.setPosition(at.getX() + 0.5, at.getY(), at.getZ() - 1.5);
        breaker.interactionManager.tryHarvestBlock(at.up());
        c.ok(c.world.isAirBlock(at) && c.world.isAirBlock(at.up()), "breaking the top takes the whole crop");
        c.eq(1, Drops.count(c.world, at, SnifferContent.pitcherPlantItem), "a grown crop drops one pitcher plant");

        // Young crops drop their pod; an age-3 crop without room above stays at 2.
        c.world.setBlockState(at, crop.getDefaultState().withProperty(BlockPitcherCrop.AGE, 2), 2);
        c.world.setBlockState(at.up(), Blocks.STONE.getDefaultState(), 2);
        c.ok(!crop.canGrow(c.world, at, c.world.getBlockState(at), false), "no room above: it cannot grow to 3");
        c.world.setBlockToAir(at.up());
        c.world.destroyBlock(at, true);
        c.eq(1, Drops.count(c.world, at, SnifferContent.pitcherPod), "an age-2 crop drops one pod");

        // Random ticks grow it all the way, no light minimum beyond 8.
        farmland(c, soil);
        c.world.setBlockState(at, crop.getDefaultState(), 2);
        int ticks = 0;
        while (c.world.getBlockState(at).getValue(BlockPitcherCrop.AGE) < 4 && ticks < 5000) {
            crop.updateTick(c.world, at, c.world.getBlockState(at), rand);
            ticks++;
        }
        c.eq(4, c.world.getBlockState(at).getValue(BlockPitcherCrop.AGE), "random ticks grow it to age 4 (" + ticks + ")");
        c.world.setBlockToAir(at.up());
        c.world.setBlockToAir(at);
        Drops.count(c.world, at, SnifferContent.pitcherPod);
    }

    private void pitcherPlant(Check c) {
        BlockPos area = c.area();
        BlockPos ground = area.add(5, -1, 5);
        c.world.setBlockState(ground, Blocks.DIRT.getDefaultState().withProperty(BlockDirt.VARIANT, BlockDirt.DirtType.PODZOL));
        EntityPlayerMP player = Players.real(c.world, "florist");
        player.setPosition(ground.getX() + 0.5, ground.getY() + 1, ground.getZ() - 2.5);
        ItemStack plant = new ItemStack(SnifferContent.pitcherPlantItem, 2);
        player.setHeldItem(EnumHand.MAIN_HAND, plant);
        plant.onItemUse(player, c.world, ground, EnumHand.MAIN_HAND, EnumFacing.UP, 0.5F, 1.0F, 0.5F);
        BlockPos at = ground.up();
        c.eq(SnifferContent.pitcherPlant, c.world.getBlockState(at).getBlock(), "pitcher plant placed on podzol");
        c.eq(Half.UPPER, c.world.getBlockState(at.up()).getValue(Half.PROPERTY), "with its upper half");
        player.interactionManager.tryHarvestBlock(at.up());
        c.ok(c.world.isAirBlock(at), "breaking the top breaks the bottom");
        c.eq(1, Drops.count(c.world, at, SnifferContent.pitcherPlantItem), "one pitcher plant dropped");
        c.world.setBlockState(at.up(2), Blocks.STONE.getDefaultState());
        c.world.setBlockState(at.up(), Blocks.STONE.getDefaultState());
        c.ok(!SnifferContent.pitcherPlant.canPlaceBlockAt(c.world, at), "no room above: not placed");
    }

    private void egg(Check c) {
        BlockPos area = c.area();
        BlockPos at = area.add(5, 0, 5);
        BlockSnifferEgg egg = SnifferContent.snifferEgg;
        c.world.setBlockState(at, egg.getDefaultState(), 3);
        c.ok(c.world.isUpdateScheduled(at, egg), "a placed egg schedules its first crack");
        c.ok(!BlockSnifferEgg.hatchBoost(c.world, at), "stone is no hatch boost");
        Random rand = new Random(3);
        egg.updateTick(c.world, at, c.world.getBlockState(at), rand);
        c.eq(1, c.world.getBlockState(at).getValue(BlockSnifferEgg.HATCH), "first crack");
        egg.updateTick(c.world, at, c.world.getBlockState(at), rand);
        c.eq(2, c.world.getBlockState(at).getValue(BlockSnifferEgg.HATCH), "second crack");
        egg.updateTick(c.world, at, c.world.getBlockState(at), rand);
        c.ok(c.world.isAirBlock(at), "the third tick hatches it");
        List<EntitySniffer> hatched = c.world.getEntitiesWithinAABB(EntitySniffer.class, new AxisAlignedBB(at).grow(1));
        c.eq(1, hatched.size(), "one snifflet");
        if (!hatched.isEmpty()) {
            EntitySniffer baby = hatched.get(0);
            c.ok(baby.isChild(), "it is a baby");
            c.eq(-EntitySniffer.BABY_AGE_TICKS, baby.getGrowingAge(), "48000 ticks to grow up");
            c.ok(Math.abs(baby.width - 0.95F) < 1e-4, "half size: " + baby.width);
            baby.setDead();
        }
        c.eq(0, Drops.count(c.world, at, SnifferContent.eggItem), "hatching drops no egg");
        c.world.setBlockState(at, egg.getDefaultState(), 3);
        c.world.destroyBlock(at, true);
        c.eq(1, Drops.count(c.world, at, SnifferContent.eggItem), "a broken egg drops itself");
        c.eq(0.5F, egg.getBlockHardness(egg.getDefaultState(), c.world, at), "hardness 0.5");
    }

    private EntitySniffer spawn(Check c, double x, double y, double z, float yaw) {
        EntitySniffer s = new EntitySniffer(c.world);
        s.setLocationAndAngles(x, y, z, yaw, 0.0F);
        s.rotationYawHead = yaw;
        s.renderYawOffset = yaw;
        c.world.spawnEntity(s);
        return s;
    }

    private void sniffer(Check c) {
        BlockPos area = c.area();
        EntitySniffer s = spawn(c, area.getX() + 5.5, area.getY(), area.getZ() + 5.5, 0.0F);
        c.eq(14.0D, s.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).getBaseValue(), "14 health");
        c.ok(Math.abs(s.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getBaseValue() - 0.1D) < 1e-6, "speed 0.1");
        c.ok(Math.abs(s.width - 1.9F) < 1e-4 && Math.abs(s.height - 1.75F) < 1e-4, "1.9 x 1.75");
        c.ok(Math.abs(s.getEyeHeight() - 1.05F) < 1e-4, "eyes at 0.6 of the height");
        c.ok(s.isBreedingItem(new ItemStack(SnifferContent.torchflowerSeeds)), "bred with torchflower seeds");
        c.ok(!s.isBreedingItem(new ItemStack(Items.WHEAT_SEEDS)), "not wheat seeds");
        c.ok(SnifferContent.isDiggable(Blocks.GRASS.getDefaultState()), "digs grass");
        c.ok(SnifferContent.isDiggable(Blocks.DIRT.getDefaultState().withProperty(BlockDirt.VARIANT, BlockDirt.DirtType.COARSE_DIRT)), "digs coarse dirt");
        c.ok(SnifferContent.isDiggable(Blocks.DIRT.getDefaultState().withProperty(BlockDirt.VARIANT, BlockDirt.DirtType.PODZOL)), "digs podzol");
        c.ok(!SnifferContent.isDiggable(Blocks.SAND.getDefaultState()), "not sand");
        c.ok(!SnifferContent.isDiggable(Blocks.FARMLAND.getDefaultState()), "not farmland");
        c.eq(1.8D, s.getMountedYOffset(), "riders 1.8 up");

        // Feeding a snifflet ages it and earns Little Sniffs.
        EntitySniffer baby = spawn(c, area.getX() + 12.5, area.getY(), area.getZ() + 12.5, 0.0F);
        baby.setGrowingAge(-EntitySniffer.BABY_AGE_TICKS);
        EntityPlayerMP player = Players.real(c.world, "feeder");
        player.setPosition(baby.posX, baby.posY, baby.posZ - 2);
        player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(SnifferContent.torchflowerSeeds, 3));
        c.ok(baby.processInteract(player, EnumHand.MAIN_HAND), "a snifflet takes seeds");
        c.eq(-EntitySniffer.BABY_AGE_TICKS + 4800, baby.getGrowingAge(), "a tenth of its remaining age");
        Advancement little = c.world.getMinecraftServer().getAdvancementManager().getAdvancement(SnifferAdvancements.FEED_SNIFFLET);
        c.ok(little != null && player.getAdvancements().getProgress(little).isDone(), "Little Sniffs");
        c.eq(2, player.getHeldItemMainhand().getCount(), "one seed eaten");
        // 26.1's age lock (Core's AgeLockable, held by Game Drops Backport): a snifflet starts again at -48000.
        com.exiledradio.backportcore.mechanics.AgeLockable lockable = baby;
        c.eq(-48000, lockable.startAge(), "a snifflet's start age is 26.3's -48000");
        c.ok(lockable.isBaby() && lockable.getAge() == baby.getGrowingAge(), "its age is the growing age");
        lockable.setAge(-100);
        c.eq(-100, baby.getGrowingAge(), "and set through it");
        baby.setGrowingAge(-EntitySniffer.BABY_AGE_TICKS + 4800);

        // Saved: state survives as IDLING, the cooldown and explored spots are kept.
        net.minecraft.nbt.NBTTagCompound tag = new net.minecraft.nbt.NBTTagCompound();
        s.writeToNBT(tag);
        c.ok(tag.hasKey("SniffCooldown") && tag.hasKey("ExploredPositions"), "brain memories saved");
        s.setDead();
        baby.setDead();
    }

    /** Runs entities for a number of ticks with the world clock advancing, as the server would. */
    private static void run(Check c, int ticks, EntitySniffer... sniffers) {
        for (int i = 0; i < ticks; i++) {
            c.world.getWorldInfo().setWorldTotalTime(c.world.getTotalWorldTime() + 1);
            for (EntitySniffer s : sniffers) {
                if (!s.isDead) {
                    // As World.updateEntityWithOptionalForce does around each entity's update.
                    s.ticksExisted++;
                    s.onUpdate();
                }
            }
        }
    }

    private void grassFloor(Check c, BlockPos area) {
        c.fill(area.add(0, -1, 0), area.add(31, -1, 31), Blocks.GRASS.getDefaultState());
    }

    private void breeding(Check c) {
        BlockPos area = c.area();
        grassFloor(c, area);
        EntitySniffer a = spawn(c, area.getX() + 12.5, area.getY(), area.getZ() + 16.5, 90.0F);
        EntitySniffer b = spawn(c, area.getX() + 18.5, area.getY(), area.getZ() + 16.5, -90.0F);
        EntityPlayerMP player = Players.real(c.world, "breeder");
        player.setPosition(area.getX() + 15.5, area.getY(), area.getZ() + 2.5);
        for (EntitySniffer s : new EntitySniffer[] {a, b}) {
            player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(SnifferContent.torchflowerSeeds));
            c.ok(s.processInteract(player, EnumHand.MAIN_HAND), "an adult takes seeds");
            c.ok(s.isInLove(), "and is in love");
        }
        player.setPosition(area.getX() + 15.5, area.getY() + 40, area.getZ() + 2.5);
        int t = 0;
        int eggs = 0;
        while (t < 600 && eggs == 0) {
            run(c, 10, a, b);
            t += 10;
            for (EntityItem item : c.world.getEntitiesWithinAABB(EntityItem.class, new AxisAlignedBB(area, area.add(32, 4, 32)))) {
                if (item.getItem().getItem() == SnifferContent.eggItem) {
                    eggs += item.getItem().getCount();
                    item.setDead();
                }
            }
        }
        c.eq(1, eggs, "two sniffers in love lay one egg (" + t + " ticks)");
        c.ok(a.getGrowingAge() >= 5989 && a.getGrowingAge() <= 6000, "parent cooldown 6000 (" + a.getGrowingAge() + " after the last checks)");
        c.ok(b.getGrowingAge() >= 5989 && b.getGrowingAge() <= 6000, "both parents (" + b.getGrowingAge() + ")");
        c.ok(!a.isInLove() && !b.isInLove(), "no longer in love");
        c.eq(0, c.world.getEntitiesWithinAABB(EntitySniffer.class, new AxisAlignedBB(area, area.add(32, 4, 32)),
                s -> s != null && s.isChild()).size(), "no baby is born");
        a.setDead();
        b.setDead();
    }

    private void digging(Check c) {
        BlockPos area = c.area();
        grassFloor(c, area);
        // A wall round the floor, so a wandering sniffer always has grass 10-18 blocks away to pick.
        net.minecraft.block.state.IBlockState wall = Blocks.STONE.getDefaultState();
        c.fill(area.add(0, 0, 0), area.add(31, 1, 0), wall);
        c.fill(area.add(0, 0, 31), area.add(31, 1, 31), wall);
        c.fill(area.add(0, 0, 0), area.add(0, 1, 31), wall);
        c.fill(area.add(31, 0, 0), area.add(31, 1, 31), wall);
        EntitySniffer s = spawn(c, area.getX() + 16.5, area.getY(), area.getZ() + 16.5, 0.0F);
        run(c, 5, s);
        c.ok(s.onGround, "standing on the grass");
        c.ok(s.canDig(), "it may dig the grass ahead of its head");
        Set<SnifferState> seen = new HashSet<SnifferState>();
        Set<Item> dug = new HashSet<Item>();
        int t = 0;
        while (t < 12000 && dug.isEmpty()) {
            run(c, 1, s);
            t++;
            seen.add(s.getState());
            if (t % 20 == 0) {
                for (EntityItem item : c.world.getEntitiesWithinAABB(EntityItem.class, new AxisAlignedBB(area, area.add(32, 4, 32)))) {
                    dug.add(item.getItem().getItem());
                    item.setDead();
                }
            }
        }
        TrailsAndTalesBackport.LOGGER.info("SELFTEST sniffer: first dig after {} ticks", t);
        c.ok(seen.contains(SnifferState.SNIFFING), "it sniffed (saw " + seen + ", at " + s.getPosition() + ", ground " + s.onGround + ", water " + s.isInWater() + ", love " + s.isInLove() + ", cooldown " + s.sniffCooldown() + ", child " + s.isChild() + ")");
        c.ok(seen.contains(SnifferState.SEARCHING), "it searched");
        c.ok(seen.contains(SnifferState.DIGGING), "it dug");
        c.ok(dug.contains(SnifferContent.torchflowerSeeds) || dug.contains(SnifferContent.pitcherPod),
                "digging gave a torchflower seed or a pitcher pod (" + t + " ticks, got " + dug + ")");
        // The rest of the dig: stands up, remembers the spot, is happy, then waits out the cooldown.
        int more = 0;
        while (more < 400 && s.exploredCount() == 0) {
            run(c, 1, s);
            more++;
            seen.add(s.getState());
        }
        c.ok(seen.contains(SnifferState.RISING), "it stood up");
        c.eq(1, s.exploredCount(), "it remembers the dug spot");
        c.ok(s.sniffCooldown() > 9000 && s.sniffCooldown() <= 9600, "and waits 9600 ticks to sniff again (" + s.sniffCooldown() + ")");
        Set<SnifferState> after = new HashSet<SnifferState>();
        for (int i = 0; i < 120; i++) {
            run(c, 1, s);
            after.add(s.getState());
        }
        c.ok(after.contains(SnifferState.FEELING_HAPPY), "then it is happy (" + after + ")");
        c.ok(!after.contains(SnifferState.SNIFFING) && !after.contains(SnifferState.DIGGING), "and does not sniff or dig in its cooldown");
        s.setDead();
    }

    private void advancements(Check c) {
        SnifferAdvancements.check(c.world.getMinecraftServer());
        Advancement two = c.world.getMinecraftServer().getAdvancementManager().getAdvancement(SnifferAdvancements.TWO_BY_TWO);
        Advancement seedy = c.world.getMinecraftServer().getAdvancementManager().getAdvancement(SnifferAdvancements.SEEDY_PLACE);
        c.ok(two != null && two.getCriteria().containsKey(SnifferAdvancements.SNIFFER_CRITERION), "Two by Two needs a sniffer");
        // Other streams (camel, armadillo) append their own requirements too, so the sniffer's need not be last.
        boolean required = false;
        if (two != null) {
            for (String[] req : two.getRequirements()) {
                required |= req.length == 1 && SnifferAdvancements.SNIFFER_CRITERION.equals(req[0]);
            }
        }
        c.ok(required, "as its own requirement");
        c.ok(seedy != null && seedy.getCriteria().containsKey("torchflower") && seedy.getCriteria().containsKey("pitcher_pod"),
                "A Seedy Place takes both seeds");
        c.ok(seedy != null && seedy.getRequirements().length == 1 && seedy.getRequirements()[0].length == 7,
                "in its one any-of requirement");
        Advancement egg = c.world.getMinecraftServer().getAdvancementManager().getAdvancement(new ResourceLocation("trailsandtalesbackport:husbandry/obtain_sniffer_egg"));
        c.ok(egg != null && egg.getParent() != null && "minecraft:husbandry/root".equals(egg.getParent().getId().toString()),
                "Smells Interesting is under Husbandry");
    }
}
