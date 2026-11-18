package com.exiledradio.trailsandtalesbackport.selftest;

import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import com.exiledradio.trailsandtalesbackport.camel.EntityCamel;
import com.exiledradio.trailsandtalesbackport.mechanics.EnchantingTables;
import com.exiledradio.trailsandtalesbackport.mechanics.client.MechanicsClient;
import net.minecraft.block.BlockTallGrass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.passive.AbstractHorse;
import net.minecraft.entity.passive.EntityBat;
import net.minecraft.entity.passive.EntityDonkey;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.passive.EntityLlama;
import net.minecraft.entity.passive.EntityMule;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.ContainerHorseChest;
import net.minecraft.item.ItemStack;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.MovementInput;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.client.IModGuiFactory;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.config.GuiConfigEntries;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * The 1.0.0 release check's client scenes, after {@link ClientLeftovers} in {@code runClient -Pselftest=client}. Each
 * scene is built on the integrated server in code and photographed with {@code ScreenShotHelper}, with no input:
 * <ul>
 *   <li>riding: a saddled camel, horse, donkey and mule, a llama and a pig with a carrot on a stick, each ridden by the
 *       real client player with its forward input held, as a player's client moves its mount;</li>
 *   <li>the 1.20.3 bat, flying and resting;</li>
 *   <li>names under the crosshair (P20): a named boat, a named armor stand (1.20.3 shows it only when its name is
 *       set visible) and an item frame holding a named item;</li>
 *   <li>enchanting particles through tall grass and a snow layer (P13);</li>
 *   <li>the Mechanics settings page, in English and in German.</li>
 * </ul>
 * Photos go to {@code eclipse/lookfeel/release-*.png}.
 */
final class ClientReleaseChecks {

    private static final int Y = 120;

    private final Minecraft mc;
    private final File shots;
    private final BiConsumer<Boolean, String> ok;
    private final List<Step> steps = new ArrayList<Step>();
    private int step;
    private int wait;
    private BlockPos base;

    /** One scene step: runs once, then the given number of ticks pass before the next. */
    private interface Step {
        int run() throws Exception;
    }

    ClientReleaseChecks(Minecraft mc, File shots, BiConsumer<Boolean, String> ok) {
        this.mc = mc;
        this.shots = shots;
        this.ok = ok;
        this.steps.add(this::platform);
        mount("camel", w -> new EntityCamel(w), 2.0D, true);
        mount("horse", w -> new EntityHorse(w), 3.0D, true);
        mount("donkey", w -> new EntityDonkey(w), 2.0D, true);
        mount("mule", w -> new EntityMule(w), 2.0D, true);
        mount("llama", w -> new EntityLlama(w), 0.0D, false);
        mount("pig", w -> new EntityPig(w), 1.0D, true);
        this.steps.add(this::bats);
        this.steps.add(this::batsLater);
        this.steps.add(this::names);
        this.steps.add(() -> nameCheck("cart", "release-name-boat.png", true));
        this.steps.add(() -> look(this.base.add(4, 0, -3), 0.0F, 15.0F));
        this.steps.add(() -> nameCheck("stand", "release-name-armor-stand.png", false));
        this.steps.add(() -> look(this.base.add(-4, 0, -2), 0.0F, 5.0F));
        this.steps.add(() -> nameCheck("frame", "release-name-item-frame.png", true));
        this.steps.add(this::enchanting);
        this.steps.add(this::enchantingShot);
        this.steps.add(() -> settings("en_us"));
        this.steps.add(() -> settingsShot("en_us"));
        this.steps.add(() -> language("de_de"));
        this.steps.add(() -> settings("de_de"));
        this.steps.add(() -> settingsShot("de_de"));
        this.steps.add(() -> language("en_us"));
    }

    private void ok(boolean condition, String what) {
        this.ok.accept(condition, what);
    }

    private void shot(String name) {
        ScreenShotHelper.saveScreenshot(this.shots, name, this.mc.displayWidth, this.mc.displayHeight, this.mc.getFramebuffer());
        File f = new File(new File(this.shots, "screenshots"), name);
        ok(f.isFile() && f.length() > 1000, "saved " + name);
    }

    /** @return true when done */
    boolean tick() throws Exception {
        if (this.mc.currentScreen instanceof net.minecraft.client.gui.GuiIngameMenu) {
            this.mc.displayGuiScreen(null);
        }
        if (this.mc.isGamePaused() && this.mc.currentScreen == null) {
            return false;
        }
        if (this.wait > 0) {
            this.wait--;
            return false;
        }
        if (this.step >= this.steps.size()) {
            return true;
        }
        this.wait = this.steps.get(this.step++).run();
        return false;
    }

    // ---- the server side --------------------------------------------------------------------------------------

    private IntegratedServer server() {
        return this.mc.getIntegratedServer();
    }

    private WorldServer serverWorld() {
        return server().getWorld(0);
    }

    private EntityPlayerMP serverPlayer() {
        return server().getPlayerList().getPlayerByUUID(this.mc.player.getUniqueID());
    }

    /** Runs on the server thread and waits for it. */
    private <T> T onServer(Callable<T> task) throws Exception {
        return server().callFromMainThread(task).get(30, TimeUnit.SECONDS);
    }

    private int look(BlockPos feet, float yaw, float pitch) throws Exception {
        onServer(() -> {
            EntityPlayerMP p = serverPlayer();
            p.dismountRidingEntity();
            p.connection.setPlayerLocation(feet.getX() + 0.5D, feet.getY(), feet.getZ() + 0.5D, yaw, pitch);
            return null;
        });
        return 15;
    }

    private int platform() throws Exception {
        this.mc.gameSettings.thirdPersonView = 0;
        this.mc.gameSettings.pauseOnLostFocus = false;
        onServer(() -> {
            WorldServer w = serverWorld();
            BlockPos spawn = w.getSpawnPoint();
            this.base = new BlockPos(spawn.getX(), Y, spawn.getZ());
            w.getGameRules().setOrCreateGameRule("doDaylightCycle", "false");
            w.getGameRules().setOrCreateGameRule("doMobSpawning", "false");
            w.setWorldTime(6000L);
            w.getWorldInfo().setRaining(false);
            w.getWorldInfo().setThundering(false);
            for (int x = -12; x <= 12; x++) {
                for (int z = -12; z <= 60; z++) {
                    w.setBlockState(this.base.add(x, -1, z), Blocks.GRASS.getDefaultState(), 2);
                    for (int y = 0; y < 6; y++) {
                        w.setBlockState(this.base.add(x, y, z), Blocks.AIR.getDefaultState(), 2);
                    }
                }
            }
            serverPlayer().capabilities.isFlying = false;
            serverPlayer().sendPlayerAbilities();
            return null;
        });
        look(this.base, 0.0F, 10.0F);
        return 60;
    }

    // ---- riding -----------------------------------------------------------------------------------------------

    private Entity mount;
    private double startX;
    private double startZ;
    private double serverStartZ;
    private MovementInput keys;

    /** Forward held, as a player's own client keys give it. */
    static final class Forward extends MovementInput {
        @Override
        public void updatePlayerMoveState() {
            this.moveStrafe = 0.0F;
            this.moveForward = 1.0F;
            this.forwardKeyDown = true;
            this.backKeyDown = false;
            this.leftKeyDown = false;
            this.rightKeyDown = false;
            this.jump = false;
            this.sneak = false;
        }
    }

    private void mount(String name, Function<WorldServer, Entity> make, double minDistance, boolean steered) {
        int[] id = new int[1];
        this.steps.add(() -> {
            BlockPos at = this.base.add(0, 0, 2);
            id[0] = onServer(() -> {
                WorldServer w = serverWorld();
                Entity e = make.apply(w);
                e.setLocationAndAngles(at.getX() + 0.5D, at.getY(), at.getZ() + 0.5D, 0.0F, 0.0F);
                if (e instanceof AbstractHorse) {
                    AbstractHorse horse = (AbstractHorse) e;
                    horse.setHorseTamed(true);
                    horse.setGrowingAge(0);
                    if (!(e instanceof EntityLlama)) {
                        ContainerHorseChest chest = ReflectionHelper.getPrivateValue(AbstractHorse.class, horse, "horseChest", "field_110296_bG");
                        chest.setInventorySlotContents(0, new ItemStack(Items.SADDLE));
                    }
                }
                if (e instanceof EntityPig) {
                    ((EntityPig) e).setSaddled(true);
                }
                if (e instanceof EntityLlama) {
                    // A llama walks by itself; only its rider's input is tested.
                    ((EntityLlama) e).setNoAI(true);
                }
                w.spawnEntity(e);
                EntityPlayerMP p = serverPlayer();
                p.connection.setPlayerLocation(at.getX() + 0.5D, at.getY(), at.getZ() - 1.5D, 0.0F, 15.0F);
                p.setHeldItem(EnumHand.MAIN_HAND, e instanceof EntityPig ? new ItemStack(Items.CARROT_ON_A_STICK) : ItemStack.EMPTY);
                p.startRiding(e, true);
                return e.getEntityId();
            });
            return 30;
        });
        this.steps.add(() -> {
            this.mount = this.mc.world.getEntityByID(id[0]);
            ok(this.mount != null && this.mc.player.getRidingEntity() == this.mount,
                    name + ": the client player rides it (" + this.mc.player.getRidingEntity() + ")");
            if (this.mount instanceof AbstractHorse && !(this.mount instanceof EntityLlama)) {
                ok(((AbstractHorse) this.mount).isHorseSaddled(), name + ": the client sees the saddle");
            }
            if (this.mount != null) {
                if (steered) {
                    ok(this.mount.getControllingPassenger() == this.mc.player, name + ": the rider steers (" + this.mount.getControllingPassenger() + ")");
                    ok(this.mount.canPassengerSteer(), name + ": the rider's client moves it");
                }
                this.startX = this.mount.posX;
                this.startZ = this.mount.posZ;
            }
            this.serverStartZ = onServer(() -> serverWorld().getEntityByID(id[0]).posZ);
            this.mc.player.rotationYaw = 0.0F;
            this.mc.player.rotationPitch = 15.0F;
            this.keys = this.mc.player.movementInput;
            this.mc.player.movementInput = new Forward();
            this.mc.gameSettings.thirdPersonView = 1;
            return 40;
        });
        this.steps.add(() -> {
            shot("release-ride-" + name + ".png");
            this.mc.player.movementInput = this.keys;
            this.mc.gameSettings.thirdPersonView = 0;
            if (this.mount != null) {
                double moved = this.mount.posZ - this.startZ;
                double serverMoved = onServer(() -> {
                    Entity e = serverWorld().getEntityByID(id[0]);
                    return e == null ? Double.NaN : e.posZ;
                }) - this.serverStartZ;
                if (minDistance > 0.0D) {
                    ok(moved >= minDistance, name + ": forward moves it on the client: " + String.format("%.2f", moved) + " blocks in 2 s");
                    ok(serverMoved >= minDistance, name + ": and the server takes the move: " + String.format("%.2f", serverMoved));
                } else {
                    ok(Math.abs(moved) < 0.5D, name + ": forward does not steer it, as in 1.20: " + String.format("%.2f", moved));
                }
                double side = Math.abs(this.mount.posX - this.startX);
                ok(side < 0.5D + 0.1D * Math.abs(moved), name + ": it went the rider's way: " + String.format("%.2f", side) + " sideways");
            }
            onServer(() -> {
                EntityPlayerMP p = serverPlayer();
                p.dismountRidingEntity();
                p.setHeldItem(EnumHand.MAIN_HAND, ItemStack.EMPTY);
                Entity e = serverWorld().getEntityByID(id[0]);
                if (e != null) {
                    e.setDead();
                }
                return null;
            });
            return 10;
        });
    }

    // ---- the bat ----------------------------------------------------------------------------------------------

    private int bats() throws Exception {
        onServer(() -> {
            WorldServer w = serverWorld();
            BlockPos ceiling = this.base.add(1, 3, 4);
            w.setBlockState(ceiling, Blocks.STONE.getDefaultState(), 2);
            EntityBat hanging = new EntityBat(w);
            hanging.setLocationAndAngles(ceiling.getX() + 0.5D, ceiling.getY() - 0.9D, ceiling.getZ() + 0.5D, 180.0F, 0.0F);
            hanging.setIsBatHanging(true);
            hanging.setNoAI(true);
            hanging.setNoGravity(true);
            w.spawnEntity(hanging);
            EntityBat flying = new EntityBat(w);
            flying.setLocationAndAngles(this.base.getX() - 0.5D, this.base.getY() + 1.6D, this.base.getZ() + 4.5D, 150.0F, 0.0F);
            flying.setIsBatHanging(false);
            flying.setNoAI(true);
            flying.setNoGravity(true);
            w.spawnEntity(flying);
            return null;
        });
        look(this.base.add(0, 0, 1), 0.0F, -15.0F);
        return 40;
    }

    private int batsLater() {
        List<EntityBat> bats = this.mc.world.getEntities(EntityBat.class, b -> b != null && b.getDistanceSq(this.mc.player) < 64.0D);
        ok(bats.size() == 2, "two bats in view: " + bats.size());
        boolean resting = false;
        boolean flying = false;
        for (EntityBat bat : bats) {
            Render<Entity> r = this.mc.getRenderManager().getEntityRenderObject(bat);
            ok((Object) r instanceof MechanicsClient.BatRender, "the bat is drawn by 1.20.3's renderer: " + r);
            resting |= bat.getIsBatHanging();
            flying |= !bat.getIsBatHanging();
        }
        ok(resting && flying, "one resting, one flying");
        shot("release-bat-1.png");
        this.steps.add(this.step, () -> {
            shot("release-bat-2.png");
            return 1;
        });
        return 7;
    }

    // ---- names ------------------------------------------------------------------------------------------------

    private final int[] named = new int[3];

    private int names() throws Exception {
        onServer(() -> {
            WorldServer w = serverWorld();
            BlockPos c = this.base.add(0, 0, 0);
            net.minecraft.entity.item.EntityBoat cart = new net.minecraft.entity.item.EntityBoat(w, c.getX() + 0.5D, c.getY(), c.getZ() + 3.5D);
            cart.setCustomNameTag("Named Boat");
            w.spawnEntity(cart);
            EntityArmorStand stand = new EntityArmorStand(w, c.getX() + 4.5D, c.getY(), c.getZ() + 0.5D);
            stand.setCustomNameTag("Named Armor Stand");
            w.spawnEntity(stand);
            BlockPos wall = c.add(-4, 1, 1);
            w.setBlockState(wall, Blocks.PLANKS.getDefaultState(), 2);
            EntityItemFrame frame = new EntityItemFrame(w, wall.north(), EnumFacing.NORTH);
            frame.setDisplayedItem(new ItemStack(Items.DIAMOND).setStackDisplayName("Named Diamond"));
            w.spawnEntity(frame);
            this.named[0] = cart.getEntityId();
            this.named[1] = stand.getEntityId();
            this.named[2] = frame.getEntityId();
            return null;
        });
        return look(this.base, 0.0F, 22.0F);
    }

    private int nameCheck(String which, String photo, boolean shows) throws Exception {
        int id = this.named["cart".equals(which) ? 0 : "stand".equals(which) ? 1 : 2];
        Entity e = this.mc.world.getEntityByID(id);
        Entity hit = this.mc.objectMouseOver != null ? this.mc.objectMouseOver.entityHit : null;
        ok(e != null && hit == e, which + " is under the crosshair: " + hit);
        if (e != null) {
            Render<Entity> r = this.mc.getRenderManager().getEntityRenderObject(e);
            // The renderer reads the pointed entity cached for the frame being drawn.
            this.mc.getRenderManager().pointedEntity = this.mc.pointedEntity;
            if ("cart".equals(which)) {
                ok(this.mc.pointedEntity == null, "1.12.2 sets no pointedEntity for a boat; P20 reads the crosshair's hit");
            }
            Method m = canRenderName(r.getClass());
            boolean drawn = (Boolean) m.invoke(r, e);
            if ("frame".equals(which)) {
                // The frame's renderer checks the frame being drawn; the photo shows the item's name.
                TrailsAndTalesBackport.LOGGER.info("CLIENT SELFTEST frame: renderer answers {} at tick time (judged from the photo)", drawn);
            } else {
                ok(drawn == shows, which + ": its name " + (shows ? "shows" : "stays hidden, as 1.20.3's armor stand renderer has it") + " under the crosshair");
            }
            if ("cart".equals(which)) {
                ok(MechanicsClient.canRenderName(e), "P20: Render.canRenderName answers through MechanicsClient");
            }
        }
        shot(photo);
        return 1;
    }

    private static Method canRenderName(Class<?> c) {
        for (Class<?> k = c; k != null; k = k.getSuperclass()) {
            for (Method m : k.getDeclaredMethods()) {
                if (("canRenderName".equals(m.getName()) || "func_177070_b".equals(m.getName()))
                        && m.getParameterCount() == 1 && m.getParameterTypes()[0] == Entity.class) {
                    m.setAccessible(true);
                    return m;
                }
            }
        }
        throw new IllegalStateException("no canRenderName in " + c);
    }

    // ---- enchanting -------------------------------------------------------------------------------------------

    private BlockPos table;

    private int enchanting() throws Exception {
        this.table = this.base.add(6, 0, 12);
        onServer(() -> {
            WorldServer w = serverWorld();
            BlockPos t = this.table;
            w.setBlockState(t, Blocks.ENCHANTING_TABLE.getDefaultState(), 2);
            for (int y = 0; y < 2; y++) {
                w.setBlockState(t.add(2, y, 0), Blocks.BOOKSHELF.getDefaultState(), 2);
                w.setBlockState(t.add(-2, y, 0), Blocks.BOOKSHELF.getDefaultState(), 2);
                w.setBlockState(t.add(0, y, 2), Blocks.BOOKSHELF.getDefaultState(), 2);
            }
            w.setBlockState(t.add(1, 0, 0), Blocks.TALLGRASS.getDefaultState().withProperty(BlockTallGrass.TYPE, BlockTallGrass.EnumType.GRASS), 2);
            w.setBlockState(t.add(-1, 0, 0), Blocks.SNOW_LAYER.getDefaultState(), 2);
            w.setBlockState(t.add(0, 0, 1), Blocks.STONE.getDefaultState(), 2);
            return null;
        });
        return look(this.table.add(0, 0, -3), 0.0F, 20.0F) + 60;
    }

    private int enchantingShot() {
        ok(EnchantingTables.particlesPass(this.mc.world, this.table.add(1, 0, 0)), "enchanting particles pass through tall grass");
        ok(EnchantingTables.particlesPass(this.mc.world, this.table.add(-1, 0, 0)), "and through a snow layer");
        ok(!EnchantingTables.particlesPass(this.mc.world, this.table.add(0, 0, 1)), "but not through stone");
        shot("release-enchanting-gaps.png");
        return 1;
    }

    // ---- the settings screen ----------------------------------------------------------------------------------

    private GuiConfig mechanicsPage;

    private int settings(String lang) throws Exception {
        IModGuiFactory factory = FMLClientHandler.instance().getGuiFactoryFor(Loader.instance().getIndexedModList().get(TrailsAndTalesBackport.MOD_ID));
        ok(factory != null && factory.hasConfigGui(), lang + ": the mod has a settings screen");
        GuiScreen root = factory.createConfigGui(null);
        this.mc.displayGuiScreen(root);
        this.mechanicsPage = null;
        if (root instanceof GuiConfig) {
            for (GuiConfigEntries.IConfigEntry entry : ((GuiConfig) root).entryList.listEntries) {
                if (entry instanceof GuiConfigEntries.CategoryEntry && "mechanics".equals(entry.getConfigElement().getName())) {
                    GuiScreen child = ReflectionHelper.getPrivateValue(GuiConfigEntries.CategoryEntry.class, (GuiConfigEntries.CategoryEntry) entry, "childScreen");
                    this.mechanicsPage = (GuiConfig) child;
                }
            }
        }
        ok(this.mechanicsPage != null, lang + ": the settings list a Mechanics page");
        if (this.mechanicsPage != null) {
            this.mc.displayGuiScreen(this.mechanicsPage);
        }
        return 20;
    }

    private int settingsShot(String lang) {
        if (this.mechanicsPage != null) {
            ok(lang.equals(this.mc.gameSettings.language), "the game's language is " + lang + ": " + this.mc.gameSettings.language);
            List<String> raw = new ArrayList<String>();
            List<String> names = new ArrayList<String>();
            for (GuiConfigEntries.IConfigEntry entry : this.mechanicsPage.entryList.listEntries) {
                String key = entry.getConfigElement().getLanguageKey();
                String name = I18n.format(key);
                names.add(name);
                if (name.equals(key) || !I18n.hasKey(key + ".tooltip")) {
                    raw.add(key);
                }
            }
            ok(names.size() >= 16, lang + ": the Mechanics page lists " + names.size() + " settings");
            ok(raw.isEmpty(), lang + ": every Mechanics setting has its name and tooltip" + (raw.isEmpty() ? "" : ", raw keys " + raw));
            TrailsAndTalesBackport.LOGGER.info("CLIENT SELFTEST {} Mechanics page: {}", lang, names);
        }
        shot("release-settings-mechanics-" + lang + ".png");
        this.mc.displayGuiScreen(null);
        return 1;
    }

    private int language(String code) {
        // The dev client lists no languages (no vanilla pack metadata), so the code is set as GuiLanguage would.
        ReflectionHelper.setPrivateValue(net.minecraft.client.resources.LanguageManager.class, this.mc.getLanguageManager(), code,
                "currentLanguage", "field_135048_c");
        this.mc.gameSettings.language = code;
        this.mc.refreshResources();
        // 1.20.5's reload overlay fades out first.
        return 80;
    }
}
