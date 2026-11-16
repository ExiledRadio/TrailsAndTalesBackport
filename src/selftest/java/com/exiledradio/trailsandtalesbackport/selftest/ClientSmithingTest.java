package com.exiledradio.trailsandtalesbackport.selftest;

import com.exiledradio.backportcore.smithing.ArmorTrims;
import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import com.exiledradio.trailsandtalesbackport.common.Content;
import com.exiledradio.trailsandtalesbackport.smithing.ContainerSmithing;
import com.exiledradio.trailsandtalesbackport.smithing.SmithingContent;
import com.exiledradio.trailsandtalesbackport.smithing.Templates;
import com.exiledradio.trailsandtalesbackport.smithing.client.GuiSmithing;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.List;

/**
 * {@code runClient -Pselftest=trimrender}: armor trims on a real client. In a superflat world it stands armor
 * stands in trimmed armor in front of the player (worn trims, glint over trims, darker palettes, leather, a custom
 * name), saves {@code eclipse/smithing/worn.png}; draws trimmed armor icons with a trimmed piece's tooltip
 * ({@code icons.png}); opens the 1.20 smithing screen with a trim in its result ({@code screen.png}); checks the
 * tooltip's lines; then quits. Logged as "CLIENT SELFTEST DONE pass= fail=".
 */
public final class ClientSmithingTest {

    private static final String WORLD = "smithing-client-test";

    private int ticks;
    private int pass;
    private int fail;
    private int phase;
    private File shots;

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new ClientSmithingTest());
        TrailsAndTalesBackport.LOGGER.info("CLIENT SELFTEST registered (smithing)");
    }

    private void ok(boolean condition, String what) {
        if (condition) {
            this.pass++;
            TrailsAndTalesBackport.LOGGER.info("CLIENT SELFTEST ok {}", what);
        } else {
            this.fail++;
            TrailsAndTalesBackport.LOGGER.error("CLIENT SELFTEST FAIL {}", what);
        }
    }

    static ItemStack trimmed(Item armor, Templates pattern, String material) {
        return ArmorTrims.apply(new ItemStack(armor), ArmorTrims.pattern(Content.id(pattern.pattern)),
                ArmorTrims.material(Content.id(material)));
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        try {
            this.ticks++;
            if (this.phase == 0 && mc.currentScreen instanceof GuiMainMenu && this.ticks >= 100) {
                this.phase = 1;
                this.ticks = 0;
                this.shots = new File(mc.gameDir, "smithing");
                this.shots.mkdirs();
                FileUtils.deleteQuietly(new File(new File(mc.gameDir, "saves"), WORLD));
                System.clearProperty(TrailsAndTalesBackport.MOD_ID + ".selftest");
                mc.gameSettings.tutorialStep = net.minecraft.client.tutorial.TutorialSteps.NONE;
                mc.gameSettings.pauseOnLostFocus = false;
                WorldSettings settings = new WorldSettings(1L, GameType.CREATIVE, false, false, WorldType.FLAT);
                mc.launchIntegratedServer(WORLD, WORLD, settings);
            } else if (this.phase == 1 && this.ticks > 2400) {
                ok(false, "the test world never opened");
                finish(mc);
            } else if (this.phase == 1 && mc.world != null && mc.player != null && this.ticks >= 40) {
                this.phase = 2;
                this.ticks = 0;
                mc.getIntegratedServer().addScheduledTask(this::stage);
            } else if (this.phase == 2 && this.ticks == 90) {
                mc.displayGuiScreen(null); // a pause menu, should the window have lost focus
            } else if (this.phase == 2 && this.ticks == 100) {
                ScreenShotHelper.saveScreenshot(this.shots, "worn.png", mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
                ok(true, "worn trims drawn (worn.png)");
                tooltip(mc);
                mc.displayGuiScreen(new Icons());
            } else if (this.phase == 2 && this.ticks == 130) {
                ScreenShotHelper.saveScreenshot(this.shots, "icons.png", mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
                ok(true, "icons and tooltip drawn (icons.png)");
                mc.displayGuiScreen(null);
                mc.getIntegratedServer().addScheduledTask(this::openTable);
            } else if (this.phase == 2 && this.ticks == 190) {
                ok(mc.currentScreen instanceof GuiSmithing, "the 1.20 smithing screen opened: " + mc.currentScreen);
                ScreenShotHelper.saveScreenshot(this.shots, "screen.png", mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
                mc.player.closeScreen();
                finish(mc);
            }
        } catch (Throwable t) {
            TrailsAndTalesBackport.LOGGER.error("CLIENT SELFTEST threw", t);
            this.fail++;
            finish(mc);
        }
    }

    /** Server thread: noon, the player facing five armor stands in trimmed armor. */
    private void stage() {
        EntityPlayerMP player = Minecraft.getMinecraft().getIntegratedServer().getPlayerList().getPlayers().get(0);
        WorldServer world = player.getServerWorld();
        world.setWorldTime(6000);
        world.getGameRules().setOrCreateGameRule("doDaylightCycle", "false");
        // Around wherever the player spawned (its chunks are the loaded ones).
        BlockPos base = new BlockPos(player.posX, 4, player.posZ);
        player.connection.setPlayerLocation(base.getX() + 0.5, 4, base.getZ() - 4.5, 0.0F, 15.0F);
        Object[][] sets = {
                {Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS, Templates.SENTRY, "iron"},
                {Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS, Templates.SILENCE, "gold"},
                {Items.GOLDEN_HELMET, Items.GOLDEN_CHESTPLATE, Items.GOLDEN_LEGGINGS, Items.GOLDEN_BOOTS, Templates.WILD, "redstone"},
                {Items.LEATHER_HELMET, Items.LEATHER_CHESTPLATE, Items.LEATHER_LEGGINGS, Items.LEATHER_BOOTS, Templates.COAST, "amethyst"},
                {Items.CHAINMAIL_HELMET, Items.CHAINMAIL_CHESTPLATE, Items.CHAINMAIL_LEGGINGS, Items.CHAINMAIL_BOOTS, Templates.VEX, "copper"},
        };
        for (int i = 0; i < sets.length; i++) {
            EntityArmorStand stand = new EntityArmorStand(world, base.getX() - 3.5 + i * 2, 4, base.getZ() + 0.5);
            stand.rotationYaw = 180.0F;
            stand.setRotationYawHead(180.0F);
            stand.renderYawOffset = 180.0F;
            EntityEquipmentSlot[] slots = {EntityEquipmentSlot.HEAD, EntityEquipmentSlot.CHEST, EntityEquipmentSlot.LEGS, EntityEquipmentSlot.FEET};
            for (int s = 0; s < 4; s++) {
                ItemStack piece = trimmed((Item) sets[i][s], (Templates) sets[i][4], (String) sets[i][5]);
                if (i == 1 && s == 1) {
                    piece.addEnchantment(Enchantments.PROTECTION, 1); // glint over the trim
                }
                stand.setItemStackToSlot(slots[s], piece);
            }
            world.spawnEntity(stand);
        }
        ok(true, "stands placed at " + base);
    }

    /** Server thread: a smithing table at the player's feet, its screen open with a trim in the result. */
    private void openTable() {
        EntityPlayerMP player = Minecraft.getMinecraft().getIntegratedServer().getPlayerList().getPlayers().get(0);
        BlockPos pos = new BlockPos(player.posX, 4, player.posZ + 1);
        if (SmithingContent.table != null) {
            player.world.setBlockState(pos, SmithingContent.table.getDefaultState());
        }
        player.openGui(TrailsAndTalesBackport.instance, 0, player.world, pos.getX(), pos.getY(), pos.getZ());
        if (player.openContainer instanceof ContainerSmithing) {
            ContainerSmithing m = (ContainerSmithing) player.openContainer;
            m.getSlot(ContainerSmithing.SLOT_TEMPLATE).putStack(new ItemStack(Templates.RIB.item()));
            m.getSlot(ContainerSmithing.SLOT_BASE).putStack(new ItemStack(Items.DIAMOND_CHESTPLATE));
            m.getSlot(ContainerSmithing.SLOT_ADDITION).putStack(new ItemStack(Items.EMERALD));
            m.detectAndSendChanges();
        }
    }

    private void tooltip(Minecraft mc) {
        ItemStack stack = trimmed(Items.IRON_CHESTPLATE, Templates.SENTRY, "copper");
        stack.addEnchantment(Enchantments.UNBREAKING, 2);
        // getTooltip fires ItemTooltipEvent itself.
        List<String> lines = stack.getTooltip(mc.player, ITooltipFlag.TooltipFlags.NORMAL);
        TrailsAndTalesBackport.LOGGER.info("CLIENT SELFTEST tooltip {}", lines);
        ok(lines.size() > 4 && lines.get(1).endsWith("Upgrade: "), "line 2 is 'Upgrade:'");
        ok(lines.size() > 4 && lines.get(4).contains("Unbreaking"), "the trim comes before the enchantments");
        ItemStack template = new ItemStack(Templates.SENTRY.item());
        List<String> t = template.getTooltip(mc.player, ITooltipFlag.TooltipFlags.NORMAL);
        TrailsAndTalesBackport.LOGGER.info("CLIENT SELFTEST template tooltip {}", t);
        ok(t.get(0).contains("Smithing Template") && t.get(1).contains("Sentry Armor Trim") && t.get(3).contains("Applies to:")
                && t.get(4).contains("Armor") && t.get(6).contains("Ingots & Crystals"), "template tooltip as 1.20");
    }

    private void finish(Minecraft mc) {
        this.phase = 3;
        TrailsAndTalesBackport.LOGGER.info("CLIENT SELFTEST DONE pass={} fail={}", this.pass, this.fail);
        mc.shutdown();
    }

    /** Trimmed icons, four times their size, with a trimmed piece's tooltip. */
    private static final class Icons extends GuiScreen {

        private final ItemStack[] stacks = {
                trimmed(Items.IRON_HELMET, Templates.EYE, "iron"),
                trimmed(Items.IRON_CHESTPLATE, Templates.EYE, "gold"),
                trimmed(Items.DIAMOND_LEGGINGS, Templates.TIDE, "diamond"),
                trimmed(Items.GOLDEN_BOOTS, Templates.RIB, "netherite"),
                trimmed(Items.LEATHER_CHESTPLATE, Templates.WARD, "emerald"),
                trimmed(Items.CHAINMAIL_HELMET, Templates.SNOUT, "lapis"),
                trimmed(Items.DIAMOND_CHESTPLATE, Templates.SPIRE, "quartz"),
                new ItemStack(Items.DIAMOND_CHESTPLATE),
        };

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            drawDefaultBackground();
            RenderHelper.enableGUIStandardItemLighting();
            for (int i = 0; i < this.stacks.length; i++) {
                GlStateManager.pushMatrix();
                GlStateManager.translate(10 + i * 40, 8, 0);
                GlStateManager.scale(2.0F, 2.0F, 1.0F);
                this.itemRender.renderItemAndEffectIntoGUI(this.stacks[i], 0, 0);
                GlStateManager.popMatrix();
            }
            RenderHelper.disableStandardItemLighting();
            ItemStack shown = trimmed(Items.IRON_CHESTPLATE, Templates.SENTRY, "copper");
            renderToolTip(shown, 0, 60);
            renderToolTip(trimmed(Items.DIAMOND_HELMET, Templates.WAYFINDER, "lapis"), 130, 60);
            renderToolTip(trimmed(Items.GOLDEN_LEGGINGS, Templates.SHAPER, "amethyst"), 270, 60);
            renderToolTip(new ItemStack(Templates.SENTRY.item()), 0, 170);
            renderToolTip(trimmed(Items.IRON_BOOTS, Templates.HOST, "copper"), 160, 170);
        }
    }
}
