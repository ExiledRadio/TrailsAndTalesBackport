package com.exiledradio.trailsandtalesbackport.wolf;

import com.exiledradio.trailsandtalesbackport.ModConfig;
import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import com.exiledradio.trailsandtalesbackport.common.Content;
import com.exiledradio.trailsandtalesbackport.common.Owners;

import net.minecraft.advancements.Advancement;
import net.minecraft.block.BlockCauldron;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemShears;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.server.MinecraftServer;
import net.minecraft.stats.StatList;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.PlaySoundAtEntityEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.AnimalTameEvent;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.oredict.ShapedOreRecipe;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Stream W: wolves (1.20.5's wolf armor, wolf variants and tamed-wolf changes).
 *
 * <p>1.12.2's own {@code EntityWolf} stays the wolf; everything 1.20.5 adds is kept in {@link WolfData} and done
 * from events, so nothing replaces or patches the vanilla class.
 *
 * <ul>
 *   <li>Item {@code wolf_armor} ({@link ItemWolfArmor}): six armadillo scutes, dyeable in the grid
 *       ({@link RecipeWolfArmorDye}) and washed in a cauldron.</li>
 *   <li>The owner of a tamed adult wolf puts it on, takes it off with shears (not while cursed with binding, unless
 *       in creative) and, with the wolf sitting, repairs it with a scute: an eighth of its durability each.</li>
 *   <li>Worn, it absorbs all damage but {@link WolfArmor#BYPASSES_WOLF_ARMOR}, cracking in three stages, and
 *       drops when the wolf dies.</li>
 *   <li>Variants ({@link WolfVariant}): a new wolf takes its biome's; a pup one parent's at random; a wolf saved
 *       before this mod stays pale.</li>
 *   <li>Tamed wolves (1.20.5): 40 health (a tamed wolf from an older world has its health doubled once, as 1.20.5's
 *       {@code FixWolfHealth} does), food heals twice its hunger value, whining below 20 health, and damage from
 *       mobs is no longer halved first - which also leaves a snow golem's snowball with only its knockback.</li>
 *   <li>Spawning ({@link WolfSpawns}).</li>
 *   <li>Advancements <i>Good as New</i>, <i>Shear Brilliance</i> and <i>The Whole Pack</i>, under husbandry.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = TrailsAndTalesBackport.MOD_ID)
public final class WolfContent {

    private static final List<SoundEvent> SOUNDS = new ArrayList<SoundEvent>();
    public static final SoundEvent ARMOR_EQUIP_WOLF = sound("item.armor.equip_wolf");
    public static final SoundEvent ARMOR_UNEQUIP_WOLF = sound("item.armor.unequip_wolf");
    public static final SoundEvent WOLF_ARMOR_BREAK = sound("item.wolf_armor.break");
    public static final SoundEvent WOLF_ARMOR_CRACK = sound("item.wolf_armor.crack");
    public static final SoundEvent WOLF_ARMOR_DAMAGE = sound("item.wolf_armor.damage");
    public static final SoundEvent WOLF_ARMOR_REPAIR = sound("item.wolf_armor.repair");

    public static final ResourceLocation REPAIR_WOLF_ARMOR = Content.id("husbandry/repair_wolf_armor");
    public static final ResourceLocation REMOVE_WOLF_ARMOR = Content.id("husbandry/remove_wolf_armor");
    public static final ResourceLocation WHOLE_PACK = Content.id("husbandry/whole_pack");

    /** 1.20.5's {@code TAME_HEALTH}. */
    public static final float TAME_HEALTH = 40.0F;

    private static final Field LAST_DAMAGE = ReflectionHelper.findField(EntityLivingBase.class, "lastDamage", "field_110153_bc");

    /** Null when the stream is off. */
    public static Item wolfArmor;

    /** The wolf whose armor just absorbed a hit: its hurt sound is 1.20.5's armor damage sound. Server thread. */
    @Nullable
    private static EntityWolf armorHurt;
    /** The living entity being updated, when it is a wolf: its pant may be 1.20.5's whine. Server thread. */
    @Nullable
    private static EntityWolf updating;

    private WolfContent() {
    }

    public static boolean enabled() {
        return ModConfig.wolves.enabled;
    }

    private static SoundEvent sound(String name) {
        SoundEvent e = new SoundEvent(Content.id(name)).setRegistryName(Content.id(name));
        SOUNDS.add(e);
        return e;
    }

    /** Pre-init: the wolf data capability. */
    public static void preInit() {
        if (enabled()) {
            WolfData.register();
        }
    }

    /** Init: spawns, which need every biome registered. */
    public static void init() {
        if (enabled()) {
            WolfSpawns.addAll();
        }
    }

    // ------------------------------------------------------------------ registry events

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        if (enabled()) {
            wolfArmor = Content.item(event.getRegistry(), "wolf_armor", new ItemWolfArmor());
        }
    }

    @SubscribeEvent
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        if (!enabled()) {
            return;
        }
        Item scute = Owners.ours("armadillo_scute");
        if (scute != null) {
            ResourceLocation id = Content.id("wolf_armor");
            event.getRegistry().register(new ShapedOreRecipe(id, new ItemStack(wolfArmor), "X  ", "XXX", "X X",
                    'X', scute).setRegistryName(id));
        }
        event.getRegistry().register(new RecipeWolfArmorDye().setRegistryName(Content.id("wolf_armor_dye")));
    }

    @SubscribeEvent
    public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        if (enabled()) {
            for (SoundEvent e : SOUNDS) {
                event.getRegistry().register(e);
            }
        }
    }

    @SubscribeEvent
    public static void attach(AttachCapabilitiesEvent<Entity> event) {
        if (enabled() && event.getObject() instanceof EntityWolf) {
            event.addCapability(WolfData.KEY, new WolfData.Provider());
        }
    }

    // ------------------------------------------------------------------ joining, tracking, updating

    @SubscribeEvent
    public static void join(EntityJoinWorldEvent event) {
        if (!(event.getEntity() instanceof EntityWolf)) {
            return;
        }
        EntityWolf wolf = (EntityWolf) event.getEntity();
        WolfData data = WolfData.of(wolf);
        if (data == null) {
            return;
        }
        if (event.getWorld().isRemote) {
            com.exiledradio.trailsandtalesbackport.wolf.client.WolfClient.joined(wolf);
            return;
        }
        if (data.variant == null) {
            // Spawned (not yet in a chunk): 1.20.5's finalizeSpawn picks by biome. Loaded from a chunk without a
            // variant: saved before this mod, so pale, as 1.20.5's default.
            data.variant = wolf.addedToChunk ? WolfVariant.PALE : WolfVariant.forBiome(wolf.world.getBiome(new BlockPos(wolf)));
        }
        WolfArmor.updateModifier(wolf, data);
        tamedHealth(wolf, data);
        // 1.20.6's WolfPanicGoal (priority 1): on fire or freezing, the wolf runs. Added as the entity joins, never
        // from a goal.
        wolf.tasks.addTask(1, new WolfHooks.Panic(wolf));
    }

    @SubscribeEvent
    public static void startTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof EntityWolf && event.getEntityPlayer() instanceof EntityPlayerMP) {
            WolfNetwork.sendState((EntityWolf) event.getTarget(), (EntityPlayerMP) event.getEntityPlayer());
        }
    }

    @SubscribeEvent
    public static void update(LivingEvent.LivingUpdateEvent event) {
        EntityLivingBase living = event.getEntityLiving();
        if (living.world.isRemote) {
            return;
        }
        updating = null;
        if (living instanceof EntityWolf) {
            EntityWolf wolf = (EntityWolf) living;
            WolfData data = WolfData.of(wolf);
            if (data != null) {
                updating = wolf;
                tamedHealth(wolf, data);
            }
        }
    }

    /**
     * 1.20.5's 40 tamed health. 1.12.2 sets a tamed wolf's base to 20 whenever it is tamed or loaded; it is set back
     * to 40 here, and the first time a wolf gets it, its health is doubled: 1.12.2's taming leaves it at 20 (1.20.5
     * sets 40) and a wolf from an older world keeps its share (1.20.5's {@code FixWolfHealth}).
     */
    static void tamedHealth(EntityWolf wolf, WolfData data) {
        if (!wolf.isTamed()) {
            return;
        }
        IAttributeInstance max = wolf.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH);
        if (max.getBaseValue() == 20.0D) {
            max.setBaseValue(TAME_HEALTH);
            if (!data.health40) {
                wolf.setHealth(wolf.getHealth() * 2.0F);
            }
            data.health40 = true;
        } else if (max.getBaseValue() == TAME_HEALTH) {
            data.health40 = true;
        }
    }

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            armorHurt = null;
            updating = null;
        }
    }

    // ------------------------------------------------------------------ damage

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void attacked(LivingAttackEvent event) {
        if (event.getEntityLiving() instanceof EntityWolf && !event.getEntityLiving().world.isRemote) {
            armorHurt = null;
        }
    }

    /**
     * 1.20.5's {@code Wolf.hurt} and {@code actuallyHurt}.
     *
     * <p>1.12.2's wolf first turns damage from any mob (not a player or an arrow) into {@code (d + 1) / 2}; 1.20.5
     * dropped that. This is the first point after it, so the amount is turned back: on a fresh hit 1.12.2 has
     * just stored the halved amount as {@code lastDamage} and reset the hurt timer, and the real damage is
     * {@code 2h - 1}; within the hurt cooldown only the excess over the last hit arrives, the difference of two
     * halved amounts, so it is doubled.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void hurt(LivingHurtEvent event) {
        if (!(event.getEntityLiving() instanceof EntityWolf) || event.getEntityLiving().world.isRemote) {
            return;
        }
        EntityWolf wolf = (EntityWolf) event.getEntityLiving();
        WolfData data = WolfData.of(wolf);
        if (data == null) {
            return;
        }
        DamageSource source = event.getSource();
        float amount = event.getAmount();
        Entity by = source.getTrueSource();
        if (by != null && !(by instanceof EntityPlayer) && !(by instanceof EntityArrow)) {
            boolean fresh = wolf.hurtResistantTime == wolf.maxHurtResistantTime && amount == lastDamage(wolf);
            amount = fresh ? amount * 2.0F - 1.0F : amount * 2.0F;
            event.setAmount(amount);
        }
        if (WolfArmor.canAbsorb(data, source)) {
            WolfArmor.absorb(wolf, data, amount);
            event.setCanceled(true);
            if (data.hasArmor()) {
                armorHurt = wolf;
            }
            return;
        }
        if (!source.isUnblockable()) {
            WolfArmor.hurtByUnabsorbed(wolf, data, amount);
        }
    }

    private static float lastDamage(EntityWolf wolf) {
        try {
            return LAST_DAMAGE.getFloat(wolf);
        } catch (IllegalAccessException e) {
            return Float.NaN;
        }
    }

    /** 1.20.5's sound choices that 1.12.2 makes inside the wolf: the armor's hurt sound, and whining below 20. */
    @SubscribeEvent
    public static void sound(PlaySoundAtEntityEvent event) {
        if (FMLCommonHandler.instance().getEffectiveSide() != Side.SERVER) {
            return;
        }
        if (event.getSound() == SoundEvents.ENTITY_WOLF_HURT && armorHurt != null) {
            armorHurt = null;
            event.setSound(WOLF_ARMOR_DAMAGE);
        } else if (event.getSound() == SoundEvents.ENTITY_WOLF_PANT && updating != null && updating.isTamed()
                && updating.getHealth() < 20.0F) {
            event.setSound(SoundEvents.ENTITY_WOLF_WHINE);
        }
    }

    /** The armor drops on death (1.20's body slot drops it for sure), with mob loot on. */
    @SubscribeEvent
    public static void drops(LivingDropsEvent event) {
        if (!(event.getEntityLiving() instanceof EntityWolf) || event.getEntityLiving().world.isRemote) {
            return;
        }
        EntityWolf wolf = (EntityWolf) event.getEntityLiving();
        WolfData data = WolfData.of(wolf);
        if (data == null || !data.hasArmor()) {
            return;
        }
        if (wolf.world.getGameRules().getBoolean("doMobLoot")) {
            EntityItem item = new EntityItem(wolf.world, wolf.posX, wolf.posY, wolf.posZ, data.armor.copy());
            item.setDefaultPickupDelay();
            event.getDrops().add(item);
        }
        data.armor = ItemStack.EMPTY;
    }

    // ------------------------------------------------------------------ interaction

    /** 1.20.5's {@code Wolf.mobInteract} for a tamed wolf: feeding, armor on, shears, scute repair. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void interact(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof EntityWolf)) {
            return;
        }
        EntityWolf wolf = (EntityWolf) event.getTarget();
        WolfData data = WolfData.of(wolf);
        ItemStack stack = event.getItemStack();
        if (data == null || !wolf.isTamed() || stack.isEmpty()) {
            return;
        }
        EntityPlayer player = event.getEntityPlayer();
        boolean server = !wolf.world.isRemote;

        if (stack.getItem() instanceof ItemFood && ((ItemFood) stack.getItem()).isWolfsFavoriteMeat()
                && wolf.getHealth() < wolf.getMaxHealth()) {
            if (server) {
                float heal = 2.0F * ((ItemFood) stack.getItem()).getHealAmount(stack);
                if (!player.capabilities.isCreativeMode) {
                    stack.shrink(1);
                }
                wolf.heal(heal);
            }
            done(event);
            return;
        }
        if (!wolf.isOwner(player)) {
            // 1.20.5's mobInteract: only the owner dyes the collar (1.12.2 let anyone). Nothing happens otherwise.
            if (net.minecraftforge.oredict.DyeUtils.isDye(stack)) {
                event.setCanceled(true);
                event.setCancellationResult(EnumActionResult.PASS);
            }
            return;
        }
        if (WolfArmor.isWolfArmor(stack) && !data.hasArmor() && !wolf.isChild()) {
            if (server) {
                ItemStack one = stack.copy();
                one.setCount(1);
                WolfArmor.set(wolf, data, one);
                if (!player.capabilities.isCreativeMode) {
                    stack.shrink(1);
                }
                wolf.playSound(ARMOR_EQUIP_WOLF, 1.0F, 1.0F);
            }
            done(event);
            return;
        }
        if (stack.getItem() instanceof ItemShears && data.hasArmor()
                && (!EnchantmentHelper.hasBindingCurse(data.armor) || player.capabilities.isCreativeMode)) {
            if (server) {
                stack.damageItem(1, player);
                wolf.playSound(ARMOR_UNEQUIP_WOLF, 1.0F, 1.0F);
                ItemStack armor = data.armor;
                WolfArmor.set(wolf, data, ItemStack.EMPTY);
                wolf.entityDropItem(armor, 0.0F);
                grant(player, REMOVE_WOLF_ARMOR, "remove_wolf_armor");
            }
            done(event);
            return;
        }
        Item scute = Owners.ours("armadillo_scute");
        if (scute != null && stack.getItem() == scute && wolf.isSitting() && data.hasArmor() && data.armor.isItemDamaged()) {
            if (server) {
                stack.shrink(1);
                WolfArmor.repair(wolf, data);
                if (data.armor.getItemDamage() == 0) {
                    grant(player, REPAIR_WOLF_ARMOR, "repair_wolf_armor");
                }
            }
            done(event);
        }
    }

    private static void done(PlayerInteractEvent.EntityInteract event) {
        event.setCanceled(true);
        event.setCancellationResult(EnumActionResult.SUCCESS);
    }

    /** 1.20.5's cauldron washes the dye off wolf armor, one level of water each. */
    @SubscribeEvent
    public static void cauldron(PlayerInteractEvent.RightClickBlock event) {
        ItemStack stack = event.getItemStack();
        if (!WolfArmor.isWolfArmor(stack) || !ItemWolfArmor.hasColor(stack) || event.getEntityPlayer().isSneaking()) {
            return;
        }
        IBlockState state = event.getWorld().getBlockState(event.getPos());
        if (!(state.getBlock() instanceof BlockCauldron) || state.getValue(BlockCauldron.LEVEL) <= 0) {
            return;
        }
        if (!event.getWorld().isRemote) {
            ItemWolfArmor.removeColor(stack);
            ((BlockCauldron) state.getBlock()).setWaterLevel(event.getWorld(), event.getPos(), state,
                    state.getValue(BlockCauldron.LEVEL) - 1);
            event.getEntityPlayer().addStat(StatList.ARMOR_CLEANED);
        }
        event.setCanceled(true);
        event.setCancellationResult(EnumActionResult.SUCCESS);
    }

    // ------------------------------------------------------------------ taming, breeding, spawning

    /** The Whole Pack: taming each variant once. Lowest priority: by now nothing has stopped the taming. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void tamed(AnimalTameEvent event) {
        if (event.getAnimal() instanceof EntityWolf && event.getTamer() instanceof EntityPlayerMP) {
            WolfData data = WolfData.of((EntityWolf) event.getAnimal());
            if (data != null) {
                grant(event.getTamer(), WHOLE_PACK, data.variantOrPale().id());
            }
        }
    }

    /**
     * 1.20.5's {@code Wolf.getBreedOffspring}: the pup takes one parent's variant at random, and a tamed pup one
     * parent's collar color and full tamed health.
     */
    @SubscribeEvent
    public static void baby(BabyEntitySpawnEvent event) {
        if (!(event.getChild() instanceof EntityWolf) || !(event.getParentA() instanceof EntityWolf)
                || !(event.getParentB() instanceof EntityWolf)) {
            return;
        }
        EntityWolf pup = (EntityWolf) event.getChild();
        EntityWolf a = (EntityWolf) event.getParentA();
        EntityWolf b = (EntityWolf) event.getParentB();
        WolfData data = WolfData.of(pup);
        WolfData da = WolfData.of(a);
        WolfData db = WolfData.of(b);
        if (data == null || da == null || db == null) {
            return;
        }
        data.variant = a.getRNG().nextBoolean() ? da.variantOrPale() : db.variantOrPale();
        if (pup.isTamed()) {
            pup.setCollarColor(a.getRNG().nextBoolean() ? a.getCollarColor() : b.getCollarColor());
            pup.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(TAME_HEALTH);
            pup.setHealth(TAME_HEALTH);
            data.health40 = true;
        }
    }

    /**
     * 1.20.5's spawn rule ({@link WolfSpawns#canSpawnAt}) in place of 1.12.2's grass-only one, for natural,
     * chunk-generation and spawner spawns alike; the rest of the check (its light-based path weight off grass,
     * the block's own rule, room to stand) is 1.12.2's, as 1.20's is the same.
     */
    @SubscribeEvent
    public static void checkSpawn(LivingSpawnEvent.CheckSpawn event) {
        if (!enabled() || !(event.getEntity() instanceof EntityWolf) || event.getResult() != Event.Result.DEFAULT) {
            return;
        }
        EntityWolf wolf = (EntityWolf) event.getEntity();
        BlockPos pos = new BlockPos(wolf.posX, wolf.getEntityBoundingBox().minY, wolf.posZ);
        boolean ok = WolfSpawns.canSpawnAt(wolf.world, pos) && wolf.getBlockPathWeight(pos) >= 0.0F
                && wolf.world.getBlockState(pos.down()).canEntitySpawn(wolf) && wolf.isNotColliding();
        event.setResult(ok ? Event.Result.ALLOW : Event.Result.DENY);
    }

    // ------------------------------------------------------------------ advancements

    static void grant(EntityPlayer player, ResourceLocation id, String criterion) {
        if (!(player instanceof EntityPlayerMP)) {
            return;
        }
        MinecraftServer server = ((EntityPlayerMP) player).getServer();
        Advancement a = server == null ? null : server.getAdvancementManager().getAdvancement(id);
        if (a != null) {
            ((EntityPlayerMP) player).getAdvancements().grantCriterion(a, criterion);
        }
    }

    /** For the self-test. */
    public static Capability<WolfData> capability() {
        return WolfData.CAPABILITY;
    }
}
