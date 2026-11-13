package com.exiledradio.trailsandtalesbackport.selftest;

import com.exiledradio.trailsandtalesbackport.common.Content;
import com.exiledradio.trailsandtalesbackport.common.Owners;
import com.exiledradio.trailsandtalesbackport.wolf.ItemWolfArmor;
import com.exiledradio.trailsandtalesbackport.wolf.RecipeWolfArmorDye;
import com.exiledradio.trailsandtalesbackport.wolf.WolfArmor;
import com.exiledradio.trailsandtalesbackport.wolf.WolfContent;
import com.exiledradio.trailsandtalesbackport.wolf.WolfData;
import com.exiledradio.trailsandtalesbackport.wolf.WolfNetwork;
import com.exiledradio.trailsandtalesbackport.wolf.WolfSpawns;
import com.exiledradio.trailsandtalesbackport.wolf.WolfVariant;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.advancements.Advancement;
import net.minecraft.block.BlockCauldron;
import net.minecraft.block.BlockDirt;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntitySnowman;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntitySnowball;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.PlaySoundAtEntityEvent;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Stream W: wolf armor, wolf variants and the tamed-wolf changes. */
public final class WolfSelfTest implements SelfTest {

    private Item scute;

    @Override
    public String name() {
        return "wolf";
    }

    @Override
    public void run(Check c) {
        if (!c.ok(WolfContent.wolfArmor != null && WolfData.CAPABILITY != null, "wolves stream registered")) {
            return;
        }
        this.scute = Owners.ours("armadillo_scute");
        registration(c);
        recipes(c);
        variants(c);
        spawns(c);
        joining(c);
        taming(c);
        damage(c);
        armor(c);
        armorItemRules(c);
        sounds(c);
        breeding(c);
        persistence(c);
        packets(c);
        advancements(c);
        c.world.playerEntities.removeAll(OWNERS);
        OWNERS.clear();
    }

    // ------------------------------------------------------------------ registration and recipes

    private void registration(Check c) {
        Item armor = WolfContent.wolfArmor;
        c.eq(Content.id("wolf_armor"), armor.getRegistryName(), "wolf armor name");
        c.eq("item.trailsandtalesbackport.wolf_armor", armor.getTranslationKey(), "wolf armor key");
        ItemStack s = new ItemStack(armor);
        c.eq(1, s.getMaxStackSize(), "one per stack");
        c.eq(64, s.getMaxDamage(), "64 durability");
        c.ok(!armor.isEnchantable(s) && armor.getItemEnchantability() == 0, "not enchantable at the table");
        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        c.ok(!armor.isBookEnchantable(s, book), "nor with books");
        c.ok(this.scute != null && armor.getIsRepairable(s, new ItemStack(this.scute)), "repaired with scutes in an anvil");
        c.ok(!armor.getIsRepairable(s, new ItemStack(Items.LEATHER)), "not with leather");
        int sounds = 0;
        for (String e : new String[] {"item.armor.equip_wolf", "item.armor.unequip_wolf", "item.wolf_armor.break",
                "item.wolf_armor.crack", "item.wolf_armor.damage", "item.wolf_armor.repair"}) {
            sounds += ForgeRegistries.SOUND_EVENTS.containsKey(Content.id(e)) ? 1 : 0;
        }
        c.eq(6, sounds, "6 sound events");
    }

    private void recipes(Check c) {
        ItemStack x = new ItemStack(this.scute);
        ItemStack e = ItemStack.EMPTY;
        Crafting.expect(c, Crafting.grid(3, 3, x, e, e, x, x, x, x, e, x), new ItemStack(WolfContent.wolfArmor), "6 scutes make wolf armor");
        Crafting.expect(c, Crafting.grid(3, 3, e, e, x, x, x, x, x, e, x), new ItemStack(WolfContent.wolfArmor),
                "mirrored too, as 1.20's shaped recipes");

        ItemStack armor = new ItemStack(WolfContent.wolfArmor);
        ItemStack red = new ItemStack(Items.DYE, 1, EnumDyeColor.RED.getDyeDamage());
        ItemStack yellow = new ItemStack(Items.DYE, 1, EnumDyeColor.YELLOW.getDyeDamage());
        ItemStack dyed = net.minecraft.item.crafting.CraftingManager.findMatchingResult(Crafting.grid(2, 1, armor, red), c.world);
        c.ok(dyed.getItem() == WolfContent.wolfArmor, "armor + dye is a recipe");
        c.eq(0xB02E26, ItemWolfArmor.getColor(dyed), "red as 1.20's red");
        ItemStack mixed = net.minecraft.item.crafting.CraftingManager.findMatchingResult(Crafting.grid(3, 1, armor, red, yellow), c.world);
        c.eq(expectedMix(), ItemWolfArmor.getColor(mixed), "red and yellow mixed as 1.20.5 mixes");
        ItemStack again = net.minecraft.item.crafting.CraftingManager.findMatchingResult(Crafting.grid(2, 1, dyed, yellow), c.world);
        c.eq(expectedMix(), ItemWolfArmor.getColor(again), "a dyed armor's color counts as one dye");
        c.ok(net.minecraft.item.crafting.CraftingManager.findMatchingResult(Crafting.grid(2, 1, armor, new ItemStack(Items.STICK)),
                c.world).isEmpty(), "armor and a stick make nothing");

        // The cauldron washes it, one level.
        BlockPos area = c.area();
        BlockPos at = area.add(3, 0, 3);
        c.world.setBlockState(at, Blocks.CAULDRON.getDefaultState().withProperty(BlockCauldron.LEVEL, 3), 2);
        EntityPlayerMP player = Players.real(c.world, "wolfwasher");
        player.setPosition(at.getX() + 0.5, at.getY(), at.getZ() + 2.5);
        ItemStack held = dyed.copy();
        player.setHeldItem(EnumHand.MAIN_HAND, held);
        PlayerInteractEvent.RightClickBlock click = new PlayerInteractEvent.RightClickBlock(player, EnumHand.MAIN_HAND, at,
                EnumFacing.UP, new Vec3d(0.5, 1, 0.5));
        MinecraftForge.EVENT_BUS.post(click);
        c.ok(click.isCanceled(), "the cauldron takes it");
        c.ok(!ItemWolfArmor.hasColor(player.getHeldItemMainhand()), "washed clean");
        c.eq(2, c.world.getBlockState(at).getValue(BlockCauldron.LEVEL), "one level of water used");
        PlayerInteractEvent.RightClickBlock twice = new PlayerInteractEvent.RightClickBlock(player, EnumHand.MAIN_HAND, at,
                EnumFacing.UP, new Vec3d(0.5, 1, 0.5));
        MinecraftForge.EVENT_BUS.post(twice);
        c.ok(!twice.isCanceled(), "undyed armor is not washed");
    }

    /** 1.20.5's DyedItemColor.applyDyes for red then yellow, worked by hand. */
    private static int expectedMix() {
        int r = (176 + 254) / 2;
        int g = (46 + 216) / 2;
        int b = (38 + 61) / 2;
        float avgMax = (176 + 254) / 2.0F;
        float top = Math.max(r, Math.max(g, b));
        return ((int) (r * avgMax / top) << 16) + ((int) (g * avgMax / top) << 8) + (int) (b * avgMax / top);
    }

    // ------------------------------------------------------------------ variants and spawning

    private void variants(Check c) {
        c.eq(WolfVariant.PALE, WolfVariant.forBiome(Biomes.TAIGA), "taiga: pale");
        c.eq(WolfVariant.PALE, WolfVariant.forBiome(Biomes.TAIGA_HILLS), "taiga hills: pale");
        c.eq(WolfVariant.ASHEN, WolfVariant.forBiome(Biomes.COLD_TAIGA), "snowy taiga: ashen");
        c.eq(WolfVariant.BLACK, WolfVariant.forBiome(Biomes.REDWOOD_TAIGA), "mega taiga (old growth pine): black");
        c.eq(WolfVariant.CHESTNUT, WolfVariant.forBiome(Biomes.MUTATED_REDWOOD_TAIGA), "mega spruce taiga: chestnut");
        c.eq(WolfVariant.WOODS, WolfVariant.forBiome(Biomes.FOREST), "forest: woods");
        c.eq(WolfVariant.PALE, WolfVariant.forBiome(Biomes.BIRCH_FOREST), "birch forest: pale");
        c.eq(WolfVariant.SPOTTED, WolfVariant.forBiome(Biomes.SAVANNA), "savanna: spotted");
        c.eq(WolfVariant.SPOTTED, WolfVariant.forBiome(Biomes.SAVANNA_PLATEAU), "savanna plateau: spotted");
        c.eq(WolfVariant.RUSTY, WolfVariant.forBiome(Biomes.JUNGLE_EDGE), "jungle edge (sparse jungle): rusty");
        c.eq(WolfVariant.RUSTY, WolfVariant.forBiome(Biomes.JUNGLE), "jungle: rusty");
        c.eq(WolfVariant.STRIPED, WolfVariant.forBiome(Biomes.MESA_ROCK), "wooded badlands: striped");
        c.eq(WolfVariant.STRIPED, WolfVariant.forBiome(Biomes.MESA), "badlands: striped");
        c.eq(WolfVariant.PALE, WolfVariant.forBiome(Biomes.PLAINS), "plains: pale");
        c.eq(WolfVariant.SNOWY, WolfVariant.forBiomePath("grove"), "grove (Caves & Cliffs, by name): snowy");
        c.eq(WolfVariant.BLACK, WolfVariant.forBiomePath("old_growth_pine_taiga"), "1.18 names too");
        c.eq(WolfVariant.PALE, WolfVariant.forBiome(null), "no biome: pale");
        Biome grove = ForgeRegistries.BIOMES.getValue(new ResourceLocation(Owners.CAVES_AND_CLIFFS, "grove"));
        c.ok(grove == null || WolfVariant.forBiome(grove) == WolfVariant.SNOWY, "Caves & Cliffs' grove, when present, is snowy");
        c.eq("minecraft:ashen", WolfVariant.ASHEN.id(), "1.20's ids");
        c.eq(WolfVariant.WOODS, WolfVariant.byId("minecraft:woods"), "read back by id");
    }

    private void spawns(Check c) {
        c.ok(spawn(Biomes.SAVANNA_PLATEAU, 8, 4, 8), "savanna plateau: 8, 4-8");
        c.ok(spawn(Biomes.JUNGLE_EDGE, 8, 2, 4), "jungle edge (sparse jungle): 8, 2-4");
        c.ok(spawn(Biomes.MUTATED_JUNGLE_EDGE, 8, 2, 4), "jungle edge M");
        c.ok(spawn(Biomes.MESA_ROCK, 2, 4, 8), "mesa plateau F (wooded badlands): 2, 4-8");
        c.ok(spawn(Biomes.TAIGA, 8, 4, 4) && wolves(Biomes.TAIGA) == 1, "taiga keeps its one 8, 4-4");
        c.ok(spawn(Biomes.FOREST, 5, 4, 4) && wolves(Biomes.FOREST) == 1, "forest keeps its one 5, 4-4");
        c.ok(spawn(Biomes.COLD_TAIGA, 8, 4, 4), "snowy taiga");
        c.eq(0, wolves(Biomes.PLAINS), "none in plains");
        c.eq(0, wolves(Biomes.SAVANNA), "none in the plain savanna");
        c.eq(0, wolves(Biomes.MESA), "none in the plain badlands");
        WolfSpawns.addSpawns(Biomes.SAVANNA_PLATEAU);
        c.eq(1, wolves(Biomes.SAVANNA_PLATEAU), "added once");
        c.ok(WolfSpawns.forBiomePath("grove") == WolfSpawns.GROVE && WolfSpawns.forBiomePath("sparse_jungle") == WolfSpawns.SPARSE_JUNGLE,
                "1.18 names");

        c.ok(WolfSpawns.isSpawnableOn(Blocks.GRASS.getDefaultState()), "spawns on grass");
        c.ok(WolfSpawns.isSpawnableOn(Blocks.SNOW_LAYER.getDefaultState()), "snow");
        c.ok(WolfSpawns.isSpawnableOn(Blocks.SNOW.getDefaultState()), "snow blocks");
        c.ok(WolfSpawns.isSpawnableOn(Blocks.DIRT.getDefaultState().withProperty(BlockDirt.VARIANT, BlockDirt.DirtType.COARSE_DIRT)), "coarse dirt");
        c.ok(WolfSpawns.isSpawnableOn(Blocks.DIRT.getDefaultState().withProperty(BlockDirt.VARIANT, BlockDirt.DirtType.PODZOL)), "podzol");
        c.ok(!WolfSpawns.isSpawnableOn(Blocks.DIRT.getDefaultState()), "not dirt");
        c.ok(!WolfSpawns.isSpawnableOn(Blocks.STONE.getDefaultState()), "not stone");
        c.ok(!WolfSpawns.isSpawnableOn(Blocks.SAND.getDefaultState()), "not sand");

        BlockPos area = c.area();
        BlockPos podzol = area.add(4, 0, 4);
        c.world.setBlockState(podzol.down(), Blocks.DIRT.getDefaultState().withProperty(BlockDirt.VARIANT, BlockDirt.DirtType.PODZOL), 2);
        EntityWolf w = new EntityWolf(c.world);
        w.setLocationAndAngles(podzol.getX() + 0.5, podzol.getY(), podzol.getZ() + 0.5, 0.0F, 0.0F);
        boolean bright = c.world.getLight(podzol) > 8 && w.getBlockPathWeight(podzol) >= 0.0F;
        Event.Result r = ForgeEventFactory.canEntitySpawn(w, c.world, (float) w.posX, (float) w.posY, (float) w.posZ, false);
        c.eq(bright ? Event.Result.ALLOW : Event.Result.DENY, r, "on lit podzol by 1.20.5's rule (light " + c.world.getLight(podzol) + ")");
        w.setLocationAndAngles(podzol.getX() + 6.5, podzol.getY(), podzol.getZ() + 0.5, 0.0F, 0.0F);
        r = ForgeEventFactory.canEntitySpawn(w, c.world, (float) w.posX, (float) w.posY, (float) w.posZ, false);
        c.eq(Event.Result.DENY, r, "never on stone, chunk generation included");
    }

    private static boolean spawn(Biome biome, int weight, int min, int max) {
        for (Biome.SpawnListEntry e : biome.getSpawnableList(EnumCreatureType.CREATURE)) {
            if (e.entityClass == EntityWolf.class && e.itemWeight == weight && e.minGroupCount == min && e.maxGroupCount == max) {
                return true;
            }
        }
        return false;
    }

    private static int wolves(Biome biome) {
        int n = 0;
        for (Biome.SpawnListEntry e : biome.getSpawnableList(EnumCreatureType.CREATURE)) {
            n += e.entityClass == EntityWolf.class ? 1 : 0;
        }
        return n;
    }

    // ------------------------------------------------------------------ joining the world

    private void joining(Check c) {
        BlockPos area = c.area();
        BlockPos at = area.add(8, 0, 8);
        EntityWolf fresh = wolf(c, at);
        WolfData data = WolfData.of(fresh);
        c.ok(data != null, "every wolf has the data");
        if (data == null) {
            return;
        }
        c.eq(WolfVariant.forBiome(c.world.getBiome(at)), data.variant, "a new wolf takes its biome's variant ("
                + c.world.getBiome(at).getRegistryName() + ")");

        // An old wolf: saved without our data, loaded from its chunk.
        EntityWolf old = new EntityWolf(c.world);
        old.setLocationAndAngles(at.getX() + 3.5, at.getY(), at.getZ() + 0.5, 0, 0);
        NBTTagCompound tag = new NBTTagCompound();
        old.writeToNBTOptional(tag);
        tag.removeTag("ForgeCaps");
        EntityWolf loaded = load(c, tag);
        WolfData ld = loaded == null ? null : WolfData.of(loaded);
        c.ok(ld != null && ld.variant == WolfVariant.PALE, "a wolf saved before the mod stays pale");

        // An old tamed wolf: 20 max health, 15 health -> 40 and 30 (FixWolfHealth).
        EntityWolf tame = new EntityWolf(c.world);
        tame.setLocationAndAngles(at.getX() + 5.5, at.getY(), at.getZ() + 0.5, 0, 0);
        tame.setTamed(true);
        tame.setOwnerId(java.util.UUID.randomUUID());
        tame.setHealth(15.0F);
        tag = new NBTTagCompound();
        tame.writeToNBTOptional(tag);
        tag.removeTag("ForgeCaps");
        EntityWolf tl = load(c, tag);
        if (c.ok(tl != null, "old tamed wolf loads")) {
            c.eq(40.0D, tl.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).getBaseValue(), "old tamed wolf: 40 max health");
            c.eq(30.0F, tl.getHealth(), "and its health doubled");
            // Saved and loaded again: never doubled twice.
            tag = new NBTTagCompound();
            tl.writeToNBTOptional(tag);
            tl.setDead();
            c.world.removeEntityDangerously(tl);
            EntityWolf again = load(c, tag);
            c.ok(again != null && again.getHealth() == 30.0F
                    && again.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).getBaseValue() == 40.0D, "reloaded: 30 of 40 still");
            kill(c, again);
        }
        kill(c, fresh);
        kill(c, loaded);
    }

    // ------------------------------------------------------------------ taming and feeding

    private void taming(Check c) {
        BlockPos area = c.area();
        EntityWolf w = wolf(c, area.add(8, 0, 8));
        EntityPlayerMP player = Players.real(c.world, "wolftamer");
        player.setPosition(area.getX() + 8.5, area.getY(), area.getZ() + 6.5);
        for (int i = 0; i < 60 && !w.isTamed(); i++) {
            player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.BONE));
            player.interactOn(w, EnumHand.MAIN_HAND);
        }
        if (!c.ok(w.isTamed(), "tamed with bones")) {
            return;
        }
        tick(w);
        c.eq(40.0F, w.getMaxHealth(), "a tamed wolf has 40 health");
        c.eq(40.0F, w.getHealth(), "and starts full");

        Advancement pack = c.world.getMinecraftServer().getAdvancementManager().getAdvancement(WolfContent.WHOLE_PACK);
        WolfData data = WolfData.of(w);
        c.ok(pack != null && data != null && player.getAdvancements().getProgress(pack).getCriterionProgress(data.variantOrPale().id())
                .isObtained(), "The Whole Pack counts its variant");

        // Food heals twice its hunger: cooked beef (8) heals 16.
        w.setHealth(10.0F);
        player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.COOKED_BEEF, 3));
        player.interactOn(w, EnumHand.MAIN_HAND);
        c.eq(26.0F, w.getHealth(), "cooked beef heals 16");
        c.eq(2, player.getHeldItemMainhand().getCount(), "one eaten");
        w.setHealth(22.0F);
        player.interactOn(w, EnumHand.MAIN_HAND);
        c.eq(38.0F, w.getHealth(), "fed above 20 health too (1.12.2 stopped at 20)");
        player.interactOn(w, EnumHand.MAIN_HAND);
        c.eq(40.0F, w.getHealth(), "up to full");
        w.setHealth(40.0F);
        player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.ROTTEN_FLESH));
        player.interactOn(w, EnumHand.MAIN_HAND);
        c.eq(40.0F, w.getHealth(), "full: not fed");
        kill(c, w);
    }

    // ------------------------------------------------------------------ damage

    private void damage(Check c) {
        BlockPos area = c.area();
        EntityWolf w = tamed(c, area.add(8, 0, 8), "wolfhurt");
        EntityZombie zombie = new EntityZombie(c.world);
        zombie.setPosition(area.getX() + 2.5, area.getY(), area.getZ() + 2.5);
        w.attackEntityFrom(DamageSource.causeMobDamage(zombie), 6.0F);
        c.eq(34.0F, w.getHealth(), "a mob's 6 is 6 (1.12.2 halved it to 3.5)");
        w.hurtResistantTime = 15;
        w.attackEntityFrom(DamageSource.causeMobDamage(zombie), 9.0F);
        c.eq(31.0F, w.getHealth(), "within the hurt cooldown only the excess, unhalved: 9 - 6");
        w.hurtResistantTime = 0;
        EntityPlayerMP player = Players.real(c.world, "wolfhitter");
        w.attackEntityFrom(DamageSource.causePlayerDamage(player), 5.0F);
        c.eq(26.0F, w.getHealth(), "a player's 5 is 5");
        w.hurtResistantTime = 0;
        w.attackEntityFrom(DamageSource.FALL, 4.0F);
        c.eq(22.0F, w.getHealth(), "fall damage unchanged");
        w.hurtResistantTime = 0;
        EntitySnowman golem = new EntitySnowman(c.world);
        golem.setPosition(area.getX() + 2.5, area.getY(), area.getZ() + 2.5);
        EntitySnowball ball = new EntitySnowball(c.world, golem);
        w.attackEntityFrom(DamageSource.causeThrownDamage(ball, golem), 0.0F);
        c.eq(22.0F, w.getHealth(), "a snow golem's snowball does no damage (1.12.2: half a heart)");
        kill(c, w);
    }

    // ------------------------------------------------------------------ armor

    private void armor(Check c) {
        BlockPos area = c.area();
        EntityWolf w = tamed(c, area.add(8, 0, 8), "wolfarmorer");
        EntityPlayerMP owner = (EntityPlayerMP) w.getOwner();
        WolfData data = WolfData.of(w);
        EntityPlayerMP other = Players.real(c.world, "notowner");
        other.setPosition(area.getX() + 9.5, area.getY(), area.getZ() + 6.5);
        other.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(WolfContent.wolfArmor));
        other.interactOn(w, EnumHand.MAIN_HAND);
        c.ok(!data.hasArmor(), "only the owner puts it on");

        owner.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(WolfContent.wolfArmor, 1));
        owner.interactOn(w, EnumHand.MAIN_HAND);
        c.ok(data.hasArmor() && owner.getHeldItemMainhand().isEmpty(), "the owner puts it on");
        c.eq(11.0D, w.getEntityAttribute(SharedMonsterAttributes.ARMOR).getAttributeValue(), "11 armor points");
        owner.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(WolfContent.wolfArmor, 1));
        owner.interactOn(w, EnumHand.MAIN_HAND);
        c.ok(!owner.getHeldItemMainhand().isEmpty(), "not a second one");

        EntityZombie zombie = new EntityZombie(c.world);
        zombie.setPosition(area.getX() + 2.5, area.getY(), area.getZ() + 2.5);
        w.hurtResistantTime = 0;
        w.attackEntityFrom(DamageSource.causeMobDamage(zombie), 3.5F);
        c.eq(40.0F, w.getHealth(), "the armor takes the hit");
        c.eq(4, data.armor.getItemDamage(), "ceil(3.5) = 4 durability");
        c.eq(WolfArmor.Crack.LOW, WolfArmor.Crack.of(data.armor), "below 95% left: low cracks");
        w.hurtResistantTime = 0;
        w.attackEntityFrom(DamageSource.FALL, 3.0F);
        c.ok(w.getHealth() == 40.0F && data.armor.getItemDamage() == 7, "falls too");
        w.hurtResistantTime = 0;
        w.attackEntityFrom(DamageSource.MAGIC, 3.0F);
        c.ok(w.getHealth() == 37.0F && data.armor.getItemDamage() == 7, "magic goes past it");
        w.hurtResistantTime = 0;
        w.attackEntityFrom(DamageSource.DROWN, 2.0F);
        c.ok(w.getHealth() == 35.0F && data.armor.getItemDamage() == 7, "so does drowning");
        w.hurtResistantTime = 0;
        w.attackEntityFrom(DamageSource.causeThornsDamage(zombie), 8.0F);
        c.eq(9, data.armor.getItemDamage(), "thorns goes past it but wears it by 8 / 4 = 2");
        c.ok(w.getHealth() < 35.0F, "thorns hurts the wolf");

        // Repair: sitting, by the owner, an eighth (8) a scute.
        ItemStack worn = data.armor.copy();
        worn.setItemDamage(20);
        WolfArmor.set(w, data, worn);
        owner.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(this.scute, 5));
        w.setSitting(false);
        owner.interactOn(w, EnumHand.MAIN_HAND);
        c.eq(20, data.armor.getItemDamage(), "not repaired standing");
        w.setSitting(true);
        owner.interactOn(w, EnumHand.MAIN_HAND);
        c.eq(12, data.armor.getItemDamage(), "a scute repairs 8");
        owner.interactOn(w, EnumHand.MAIN_HAND);
        owner.interactOn(w, EnumHand.MAIN_HAND);
        c.eq(0, data.armor.getItemDamage(), "repaired fully");
        Advancement good = c.world.getMinecraftServer().getAdvancementManager().getAdvancement(WolfContent.REPAIR_WOLF_ARMOR);
        c.ok(good != null && owner.getAdvancements().getProgress(good).isDone(), "Good as New");
        int left = owner.getHeldItemMainhand().getCount();
        owner.interactOn(w, EnumHand.MAIN_HAND);
        c.eq(left, owner.getHeldItemMainhand().getCount(), "an undamaged armor takes no scute");

        // Shears take it off.
        w.setSitting(false);
        owner.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.SHEARS));
        owner.interactOn(w, EnumHand.MAIN_HAND);
        c.ok(!data.hasArmor(), "sheared off");
        c.eq(1, owner.getHeldItemMainhand().getItemDamage(), "the shears wear by 1");
        c.eq(0.0D, w.getEntityAttribute(SharedMonsterAttributes.ARMOR).getAttributeValue(), "armor points gone");
        c.eq(1, armorItems(c, area), "dropped beside the wolf");
        Advancement shear = c.world.getMinecraftServer().getAdvancementManager().getAdvancement(WolfContent.REMOVE_WOLF_ARMOR);
        c.ok(shear != null && owner.getAdvancements().getProgress(shear).isDone(), "Shear Brilliance");

        // Binding: shears can't, outside creative.
        ItemStack bound = new ItemStack(WolfContent.wolfArmor);
        bound.addEnchantment(Enchantments.BINDING_CURSE, 1);
        WolfArmor.set(w, data, bound);
        owner.interactOn(w, EnumHand.MAIN_HAND);
        c.ok(data.hasArmor(), "cursed with binding: stays on");
        WolfArmor.set(w, data, ItemStack.EMPTY);

        // Breaking: 63 damage, then a hit.
        ItemStack nearly = new ItemStack(WolfContent.wolfArmor);
        nearly.setItemDamage(62);
        WolfArmor.set(w, data, nearly);
        w.hurtResistantTime = 0;
        float before = w.getHealth();
        w.attackEntityFrom(DamageSource.causeMobDamage(zombie), 5.0F);
        c.ok(!data.hasArmor(), "breaks when used up");
        c.eq(before, w.getHealth(), "the breaking hit is still absorbed");
        c.eq(0.0D, w.getEntityAttribute(SharedMonsterAttributes.ARMOR).getAttributeValue(), "no armor points after");

        // Drops on death.
        WolfArmor.set(w, data, new ItemStack(WolfContent.wolfArmor));
        w.onDeath(DamageSource.GENERIC);
        c.eq(1, armorItems(c, area), "dropped when the wolf dies");
        kill(c, w);

        // A baby can't wear it.
        EntityWolf pup = tamed(c, area.add(20, 0, 20), "wolfpupowner");
        pup.setGrowingAge(-24000);
        EntityPlayerMP po = (EntityPlayerMP) pup.getOwner();
        po.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(WolfContent.wolfArmor));
        po.interactOn(pup, EnumHand.MAIN_HAND);
        c.ok(!WolfData.of(pup).hasArmor(), "a pup can't wear armor");
        kill(c, pup);

        // A wild wolf can't either.
        EntityWolf wild = wolf(c, area.add(24, 0, 8));
        po.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(WolfContent.wolfArmor));
        po.interactOn(wild, EnumHand.MAIN_HAND);
        c.ok(!WolfData.of(wild).hasArmor(), "an untamed wolf can't wear armor");
        kill(c, wild);
    }

    private void armorItemRules(Check c) {
        c.eq(WolfArmor.Crack.NONE, WolfArmor.Crack.byDamage(3, 64), "95.3% left: no cracks");
        c.eq(WolfArmor.Crack.LOW, WolfArmor.Crack.byDamage(4, 64), "93.8%: low");
        c.eq(WolfArmor.Crack.MEDIUM, WolfArmor.Crack.byDamage(20, 64), "68.8%: medium");
        c.eq(WolfArmor.Crack.LOW, WolfArmor.Crack.byDamage(19, 64), "70.3%: low");
        c.eq(WolfArmor.Crack.HIGH, WolfArmor.Crack.byDamage(44, 64), "31.3%: high");
        c.eq(WolfArmor.Crack.MEDIUM, WolfArmor.Crack.byDamage(43, 64), "32.8%: medium");
        c.ok(WolfArmor.bypasses(DamageSource.OUT_OF_WORLD) && WolfArmor.bypasses(DamageSource.STARVE)
                && WolfArmor.bypasses(DamageSource.IN_WALL) && WolfArmor.bypasses(DamageSource.WITHER)
                && WolfArmor.bypasses(DamageSource.CRAMMING), "the void, starving, suffocation, wither, cramming go past");
        c.ok(!WolfArmor.bypasses(DamageSource.LAVA) && !WolfArmor.bypasses(DamageSource.IN_FIRE)
                && !WolfArmor.bypasses(DamageSource.CACTUS) && !WolfArmor.bypasses(DamageSource.LIGHTNING_BOLT),
                "lava, fire, cacti, lightning are absorbed");
    }

    // ------------------------------------------------------------------ sounds

    private final List<SoundEvent> heard = new ArrayList<SoundEvent>();

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void listen(PlaySoundAtEntityEvent event) {
        this.heard.add(event.getSound());
    }

    private void sounds(Check c) {
        BlockPos area = c.area();
        EntityWolf w = tamed(c, area.add(8, 0, 8), "wolfsounds");
        WolfData data = WolfData.of(w);
        EntityZombie zombie = new EntityZombie(c.world);
        zombie.setPosition(area.getX() + 2.5, area.getY(), area.getZ() + 2.5);
        MinecraftForge.EVENT_BUS.register(this);
        try {
            this.heard.clear();
            w.hurtResistantTime = 0;
            w.attackEntityFrom(DamageSource.causeMobDamage(zombie), 2.0F);
            c.ok(this.heard.contains(SoundEvents.ENTITY_WOLF_HURT), "without armor: the wolf's hurt sound");

            WolfArmor.set(w, data, new ItemStack(WolfContent.wolfArmor));
            this.heard.clear();
            w.hurtResistantTime = 0;
            w.attackEntityFrom(DamageSource.causeMobDamage(zombie), 2.0F);
            c.ok(this.heard.contains(WolfContent.WOLF_ARMOR_DAMAGE) && !this.heard.contains(SoundEvents.ENTITY_WOLF_HURT),
                    "with armor: the armor's damage sound " + this.heard);
            this.heard.clear();
            w.hurtResistantTime = 0;
            w.attackEntityFrom(DamageSource.causeMobDamage(zombie), 2.0F);
            c.ok(this.heard.contains(WolfContent.WOLF_ARMOR_CRACK), "crossing into low cracks: the crack sound " + this.heard);
            this.heard.clear();
            w.hurtResistantTime = 0;
            w.attackEntityFrom(DamageSource.MAGIC, 1.0F);
            c.ok(this.heard.contains(SoundEvents.ENTITY_WOLF_HURT), "magic past the armor: the wolf's own hurt sound " + this.heard);

            // Whining below 20 (1.12.2: below 10): the pant played while the wolf updates becomes a whine.
            w.setHealth(15.0F);
            MinecraftForge.EVENT_BUS.post(new LivingEvent.LivingUpdateEvent(w));
            PlaySoundAtEntityEvent pant = ForgeEventFactory.onPlaySoundAtEntity(null, SoundEvents.ENTITY_WOLF_PANT, SoundCategory.NEUTRAL, 1, 1);
            c.eq(SoundEvents.ENTITY_WOLF_WHINE, pant.getSound(), "pant at 15 health is a whine");
            w.setHealth(25.0F);
            MinecraftForge.EVENT_BUS.post(new LivingEvent.LivingUpdateEvent(w));
            pant = ForgeEventFactory.onPlaySoundAtEntity(null, SoundEvents.ENTITY_WOLF_PANT, SoundCategory.NEUTRAL, 1, 1);
            c.eq(SoundEvents.ENTITY_WOLF_PANT, pant.getSound(), "pant at 25 stays a pant");
        } finally {
            MinecraftForge.EVENT_BUS.unregister(this);
        }
        kill(c, w);
    }

    // ------------------------------------------------------------------ breeding

    private void breeding(Check c) {
        BlockPos area = c.area();
        EntityWolf a = tamed(c, area.add(8, 0, 8), "wolfbreeder");
        EntityWolf b = tamed(c, area.add(10, 0, 8), "wolfbreeder");
        WolfData.of(a).variant = WolfVariant.ASHEN;
        WolfData.of(b).variant = WolfVariant.RUSTY;
        a.setCollarColor(EnumDyeColor.BLUE);
        b.setCollarColor(EnumDyeColor.GREEN);
        int ashen = 0;
        int rusty = 0;
        boolean healthy = true;
        boolean collars = true;
        for (int i = 0; i < 40; i++) {
            EntityWolf pup = a.createChild(b);
            BabyEntitySpawnEvent e = new BabyEntitySpawnEvent(a, b, pup);
            MinecraftForge.EVENT_BUS.post(e);
            WolfVariant v = WolfData.of(pup).variant;
            ashen += v == WolfVariant.ASHEN ? 1 : 0;
            rusty += v == WolfVariant.RUSTY ? 1 : 0;
            healthy &= pup.getMaxHealth() == 40.0F && pup.getHealth() == 40.0F;
            collars &= pup.getCollarColor() == EnumDyeColor.BLUE || pup.getCollarColor() == EnumDyeColor.GREEN;
        }
        c.ok(ashen + rusty == 40 && ashen > 0 && rusty > 0, "pups take either parent's variant (" + ashen + "/" + rusty + ")");
        c.ok(healthy, "a tamed pup has 40 of 40 health");
        c.ok(collars, "and one parent's collar");
        kill(c, a);
        kill(c, b);
    }

    // ------------------------------------------------------------------ saving and packets

    private void persistence(Check c) {
        BlockPos area = c.area();
        EntityWolf w = tamed(c, area.add(8, 0, 8), "wolfsaver");
        WolfData data = WolfData.of(w);
        data.variant = WolfVariant.CHESTNUT;
        ItemStack armor = new ItemStack(WolfContent.wolfArmor);
        armor.setItemDamage(9);
        ItemWolfArmor.setColor(armor, 0x123456);
        WolfArmor.set(w, data, armor);
        NBTTagCompound tag = new NBTTagCompound();
        w.writeToNBTOptional(tag);
        kill(c, w);
        EntityWolf back = load(c, tag);
        WolfData bd = back == null ? null : WolfData.of(back);
        c.ok(bd != null && bd.variant == WolfVariant.CHESTNUT, "the variant is saved");
        c.ok(bd != null && bd.armor.getItem() == WolfContent.wolfArmor && bd.armor.getItemDamage() == 9
                && ItemWolfArmor.getColor(bd.armor) == 0x123456, "the armor is saved, worn and dyed");
        c.ok(back != null && back.getEntityAttribute(SharedMonsterAttributes.ARMOR).getAttributeValue() == 11.0D, "with its armor points");
        NBTTagCompound caps = tag.getCompoundTag("ForgeCaps").getCompoundTag(WolfData.KEY.toString());
        c.eq("minecraft:chestnut", caps.getString("variant"), "saved as 1.20's id");
        kill(c, back);
    }

    private void packets(Check c) {
        ItemStack armor = new ItemStack(WolfContent.wolfArmor);
        armor.setItemDamage(5);
        WolfNetwork.State out = new WolfNetwork.State();
        out.entity = 1234;
        out.variant = WolfVariant.STRIPED.ordinal();
        out.armor = armor;
        ByteBuf buf = Unpooled.buffer();
        out.toBytes(buf);
        WolfNetwork.State in = new WolfNetwork.State();
        in.fromBytes(buf);
        c.ok(in.entity == 1234 && WolfVariant.byOrdinal(in.variant) == WolfVariant.STRIPED && in.armor.getItemDamage() == 5,
                "the state packet round-trips");
        c.eq(WolfVariant.PALE, WolfVariant.byOrdinal(99), "an unknown variant is pale");
    }

    private void advancements(Check c) {
        for (ResourceLocation id : new ResourceLocation[] {WolfContent.REPAIR_WOLF_ARMOR, WolfContent.REMOVE_WOLF_ARMOR, WolfContent.WHOLE_PACK}) {
            Advancement a = c.world.getMinecraftServer().getAdvancementManager().getAdvancement(id);
            c.ok(a != null && a.getParent() != null && new ResourceLocation("minecraft", "husbandry/tame_an_animal").equals(a.getParent().getId()),
                    id + " under Best Friends Forever");
        }
        Advancement pack = c.world.getMinecraftServer().getAdvancementManager().getAdvancement(WolfContent.WHOLE_PACK);
        c.ok(pack != null && pack.getCriteria().size() == 9 && pack.getRequirements().length == 9, "The Whole Pack: nine variants, each required");
        c.ok(pack != null && pack.getRewards() != null, "with a reward");
    }

    // ------------------------------------------------------------------ helpers

    private static EntityWolf wolf(Check c, BlockPos at) {
        EntityWolf w = new EntityWolf(c.world);
        w.setLocationAndAngles(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0.0F, 0.0F);
        c.world.spawnEntity(w);
        return w;
    }

    private static final List<EntityPlayerMP> OWNERS = new ArrayList<EntityPlayerMP>();

    private static EntityWolf tamed(Check c, BlockPos at, String owner) {
        EntityWolf w = wolf(c, at);
        EntityPlayerMP player = Players.real(c.world, owner);
        player.setPosition(at.getX() + 0.5, at.getY(), at.getZ() - 1.5);
        w.setTamedBy(player);
        w.setHealth(20.0F);
        tick(w);
        // getOwner() and isOwner() look the owner up among the world's players: list the test player there
        // until the test ends.
        c.world.playerEntities.add(player);
        OWNERS.add(player);
        return w;
    }

    /** What a server tick does for the stream: the update event (tamed health, the updating wolf). */
    private static void tick(EntityWolf w) {
        MinecraftForge.EVENT_BUS.post(new LivingEvent.LivingUpdateEvent(w));
    }

    /** Loads a saved wolf as a chunk does: into its chunk first, then into the world. */
    private static EntityWolf load(Check c, NBTTagCompound tag) {
        net.minecraft.entity.Entity e = EntityList.createEntityFromNBT(tag, c.world);
        if (!(e instanceof EntityWolf)) {
            return null;
        }
        c.world.getChunk(new BlockPos(e)).addEntity(e);
        c.world.loadEntities(Collections.singletonList(e));
        return (EntityWolf) e;
    }

    private static int armorItems(Check c, BlockPos area) {
        int n = 0;
        for (EntityItem item : c.world.getEntitiesWithinAABB(EntityItem.class, new AxisAlignedBB(area.add(-2, -2, -2), area.add(34, 8, 34)))) {
            if (item.getItem().getItem() == WolfContent.wolfArmor) {
                n += item.getItem().getCount();
            }
            item.setDead();
            c.world.removeEntityDangerously(item);
        }
        return n;
    }

    private static void kill(Check c, EntityWolf w) {
        if (w != null) {
            w.setDead();
            c.world.removeEntityDangerously(w);
        }
    }
}
