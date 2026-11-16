package com.exiledradio.trailsandtalesbackport.smithing.client;

import com.exiledradio.backportcore.smithing.SmithingTemplates;
import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import com.exiledradio.trailsandtalesbackport.smithing.ContainerSmithing;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 1.20's {@code SmithingScreen}: its background, "Upgrade Gear" at (44, 15), the red cross at (65, 46) when all
 * three inputs make nothing, an armor stand at (141, 75), scale 25, wearing (or holding) what is in the result slot,
 * and the empty input slots' icons cycling every 1.5 seconds with a 4-tick fade (1.20's
 * {@code CyclingSlotBackground}). Hovering an empty input slot or the cross says what goes there, wrapped at 115
 * pixels.
 */
@SideOnly(Side.CLIENT)
public class GuiSmithing extends GuiContainer {

    static final ResourceLocation TEXTURE = new ResourceLocation(TrailsAndTalesBackport.MOD_ID, "textures/gui/container/smithing.png");
    static final ResourceLocation ERROR = new ResourceLocation(TrailsAndTalesBackport.MOD_ID, "textures/gui/container/smithing_error.png");

    private static final List<ResourceLocation> TEMPLATE_ICONS = Arrays.asList(
            SmithingClient.icon("smithing_template_armor_trim"), SmithingClient.icon("smithing_template_netherite_upgrade"));
    private static final List<ResourceLocation> TRIM_BASE = Arrays.asList(vanilla("helmet"), vanilla("chestplate"),
            vanilla("leggings"), vanilla("boots"));
    private static final List<ResourceLocation> UPGRADE_BASE = Arrays.asList(vanilla("helmet"), SmithingClient.icon("sword"),
            vanilla("chestplate"), SmithingClient.icon("pickaxe"), vanilla("leggings"), SmithingClient.icon("axe"),
            vanilla("boots"), SmithingClient.icon("hoe"), SmithingClient.icon("shovel"));
    private static final List<ResourceLocation> TRIM_ADDITIONS = Arrays.asList(SmithingClient.icon("ingot"),
            SmithingClient.icon("redstone_dust"), SmithingClient.icon("lapis_lazuli"), SmithingClient.icon("quartz"),
            SmithingClient.icon("diamond"), SmithingClient.icon("emerald"), SmithingClient.icon("amethyst_shard"));
    private static final List<ResourceLocation> UPGRADE_ADDITIONS = Collections.singletonList(SmithingClient.icon("ingot"));

    private final InventoryPlayer playerInventory;
    private final ContainerSmithing container;
    private final Cycling templateIcon = new Cycling(ContainerSmithing.SLOT_TEMPLATE);
    private final Cycling baseIcon = new Cycling(ContainerSmithing.SLOT_BASE);
    private final Cycling additionIcon = new Cycling(ContainerSmithing.SLOT_ADDITION);
    private EntityArmorStand stand;
    private ItemStack shown = ItemStack.EMPTY;

    public GuiSmithing(InventoryPlayer playerInventory, ContainerSmithing container) {
        super(container);
        this.playerInventory = playerInventory;
        this.container = container;
    }

    private static ResourceLocation vanilla(String piece) {
        return new ResourceLocation("minecraft", "items/empty_armor_slot_" + piece);
    }

    @Override
    public void initGui() {
        super.initGui();
        this.stand = new EntityArmorStand(this.mc.world);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setBoolean("NoBasePlate", true);
        tag.setBoolean("ShowArms", true);
        this.stand.readEntityFromNBT(tag);
        this.shown = ItemStack.EMPTY;
        dress(this.container.getSlot(ContainerSmithing.SLOT_RESULT).getStack());
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        ItemStack template = this.container.getSlot(ContainerSmithing.SLOT_TEMPLATE).getStack();
        boolean upgrade = SmithingTemplates.isUpgradeTemplate(template);
        boolean trim = SmithingTemplates.isTrimTemplate(template);
        this.templateIcon.tick(TEMPLATE_ICONS);
        this.baseIcon.tick(upgrade ? UPGRADE_BASE : trim ? TRIM_BASE : Collections.<ResourceLocation>emptyList());
        this.additionIcon.tick(upgrade ? UPGRADE_ADDITIONS : trim ? TRIM_ADDITIONS : Collections.<ResourceLocation>emptyList());
    }

    /** 1.20's {@code updateArmorStandPreview}: armor on its slot, anything else in the off hand. */
    private void dress(ItemStack stack) {
        if (ItemStack.areItemStacksEqual(stack, this.shown) || this.stand == null) {
            return;
        }
        this.shown = stack.copy();
        for (EntityEquipmentSlot s : EntityEquipmentSlot.values()) {
            this.stand.setItemStackToSlot(s, ItemStack.EMPTY);
        }
        if (!stack.isEmpty()) {
            ItemStack copy = stack.copy();
            if (stack.getItem() instanceof ItemArmor) {
                this.stand.setItemStackToSlot(((ItemArmor) stack.getItem()).armorType, copy);
            } else {
                this.stand.setItemStackToSlot(EntityEquipmentSlot.OFFHAND, copy);
            }
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        renderHoveredToolTip(mouseX, mouseY);
        hints(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        this.fontRenderer.drawString(I18n.format("container.upgrade"), 44, 15, 0x404040);
        this.fontRenderer.drawString(this.playerInventory.getDisplayName().getUnformattedText(), 8, this.ySize - 94, 0x404040);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(TEXTURE);
        drawTexturedModalRect(this.guiLeft, this.guiTop, 0, 0, this.xSize, this.ySize);
        this.templateIcon.draw(partialTicks);
        this.baseIcon.draw(partialTicks);
        this.additionIcon.draw(partialTicks);
        if (this.container.hasRecipeError()) {
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.enableBlend();
            this.mc.getTextureManager().bindTexture(ERROR);
            Gui.drawModalRectWithCustomSizedTexture(this.guiLeft + 65, this.guiTop + 46, 0, 0, 28, 21, 28, 21);
        }
        dress(this.container.getSlot(ContainerSmithing.SLOT_RESULT).getStack());
        drawStand(this.guiLeft + 141, this.guiTop + 75, 25);
    }

    /**
     * The armor stand as 1.20 draws it: no base plate, arms shown, body turned 30 degrees toward the inventory, seen a
     * little from above (25 degrees), not following the mouse.
     */
    private void drawStand(int x, int y, int scale) {
        EntityArmorStand e = this.stand;
        if (e == null) {
            return;
        }
        GlStateManager.enableColorMaterial();
        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x, (float) y, 50.0F);
        GlStateManager.scale((float) -scale, (float) scale, (float) scale);
        GlStateManager.rotate(180.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(135.0F, 0.0F, 1.0F, 0.0F);
        RenderHelper.enableStandardItemLighting();
        GlStateManager.rotate(-135.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(-25.0F, 1.0F, 0.0F, 0.0F);
        e.renderYawOffset = e.prevRenderYawOffset = 30.0F;
        e.rotationYaw = e.prevRotationYaw = 30.0F;
        e.rotationYawHead = e.prevRotationYawHead = 30.0F;
        e.rotationPitch = e.prevRotationPitch = 25.0F;
        RenderManager rm = Minecraft.getMinecraft().getRenderManager();
        rm.setPlayerViewY(180.0F);
        rm.setRenderShadow(false);
        rm.renderEntity(e, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F, false);
        rm.setRenderShadow(true);
        GlStateManager.popMatrix();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();
        GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GlStateManager.disableTexture2D();
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
    }

    /** 1.20's {@code renderOnboardingTooltips}. */
    private void hints(int mouseX, int mouseY) {
        String text = null;
        Slot hovered = getSlotUnderMouse();
        ItemStack template = this.container.getSlot(ContainerSmithing.SLOT_TEMPLATE).getStack();
        if (hovered != null && !hovered.getHasStack() && hovered.slotNumber <= ContainerSmithing.SLOT_ADDITION) {
            boolean upgrade = SmithingTemplates.isUpgradeTemplate(template);
            String kind = upgrade ? "netherite_upgrade" : "armor_trim";
            if (hovered.slotNumber == ContainerSmithing.SLOT_TEMPLATE) {
                text = I18n.format("container.upgrade.missing_template_tooltip");
            } else if (upgrade || SmithingTemplates.isTrimTemplate(template)) {
                text = I18n.format("item.minecraft.smithing_template." + kind
                        + (hovered.slotNumber == ContainerSmithing.SLOT_BASE ? ".base_slot_description" : ".additions_slot_description"));
            }
        } else if (this.container.hasRecipeError() && isPointInRegion(65, 46, 28, 21, mouseX, mouseY)) {
            text = I18n.format("container.upgrade.error_tooltip");
        }
        if (text != null) {
            drawHoveringText(new ArrayList<String>(this.fontRenderer.listFormattedStringToWidth(text, 115)), mouseX, mouseY);
        }
    }

    /** 1.20's {@code CyclingSlotBackground}: an empty slot's icons, one every 30 ticks, fading in over 4. */
    private final class Cycling {

        private final int slot;
        private List<ResourceLocation> icons = Collections.emptyList();
        private int tick;
        private int index;

        Cycling(int slot) {
            this.slot = slot;
        }

        void tick(List<ResourceLocation> now) {
            if (!this.icons.equals(now)) {
                this.icons = now;
                this.index = 0;
            }
            if (!this.icons.isEmpty() && ++this.tick % 30 == 0) {
                this.index = (this.index + 1) % this.icons.size();
            }
        }

        void draw(float partialTicks) {
            Slot s = GuiSmithing.this.container.getSlot(this.slot);
            if (this.icons.isEmpty() || s.getHasStack()) {
                return;
            }
            boolean fading = this.icons.size() > 1 && this.tick >= 30;
            float alpha = fading ? Math.min(this.tick % 30 + partialTicks, 4.0F) / 4.0F : 1.0F;
            if (alpha < 1.0F) {
                icon(s, this.icons.get(Math.floorMod(this.index - 1, this.icons.size())), 1.0F - alpha);
            }
            icon(s, this.icons.get(this.index), alpha);
        }

        private void icon(Slot s, ResourceLocation name, float alpha) {
            TextureAtlasSprite sprite = GuiSmithing.this.mc.getTextureMapBlocks().getAtlasSprite(name.toString());
            GuiSmithing.this.mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                    GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
            GlStateManager.color(1.0F, 1.0F, 1.0F, alpha);
            drawTexturedModalRect(GuiSmithing.this.guiLeft + s.xPos, GuiSmithing.this.guiTop + s.yPos, sprite, 16, 16);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }
}
