package com.exiledradio.trailsandtalesbackport.camel.client;

import com.exiledradio.backportcore.presentation.VanillaRevisions;
import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import com.exiledradio.trailsandtalesbackport.camel.EntityCamel;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Client parts of the camel stream: the renderer, and the dash's cooldown on the jump bar.
 *
 * <p>1.20's jump bar, while the camel's dash cools down, shows the cooldown bar in place of the charge and does
 * not charge at all. 1.12.2's player charges the bar whatever the mount, so after each of its ticks on a cooling
 * camel the charge is put back to nothing and its counter to where it was (1.20 leaves it untouched), and the
 * cooldown bar (1.20's {@code hud/jump_bar_cooldown} sprite) is drawn over the jump bar.
 */
public final class CamelClient {

    static final ResourceLocation TEXTURE = new ResourceLocation(TrailsAndTalesBackport.MOD_ID, "textures/entity/camel/camel.png");
    static final ResourceLocation BABY_TEXTURE = new ResourceLocation(TrailsAndTalesBackport.MOD_ID, "textures/entity/camel/camel_baby.png");
    static final ResourceLocation COOLDOWN = new ResourceLocation(TrailsAndTalesBackport.MOD_ID, "textures/gui/jump_bar_cooldown.png");
    private static final String JUMP_POWER_COUNTER = "field_110320_a";
    private static final String JUMP_POWER = "field_110321_bQ";

    private int frozenCounter;

    private CamelClient() {
    }

    public static void init() {
        RenderingRegistry.registerEntityRenderingHandler(EntityCamel.class, Render::new);
        MinecraftForge.EVENT_BUS.register(new CamelClient());
    }

    /** The camel the local player steers while its dash cools down, or null. */
    private static EntityCamel coolingCamel(EntityPlayerSP player) {
        Entity mount = player == null ? null : player.getRidingEntity();
        if (mount instanceof EntityCamel && player.isRidingHorse() && ((EntityCamel) mount).getJumpCooldown() > 0) {
            return (EntityCamel) mount;
        }
        return null;
    }

    @SubscribeEvent
    public void playerTick(TickEvent.PlayerTickEvent event) {
        if (!(event.player instanceof EntityPlayerSP) || coolingCamel((EntityPlayerSP) event.player) == null) {
            return;
        }
        EntityPlayerSP player = (EntityPlayerSP) event.player;
        if (event.phase == TickEvent.Phase.START) {
            this.frozenCounter = ObfuscationReflectionHelper.<Integer, EntityPlayerSP>getPrivateValue(EntityPlayerSP.class, player, JUMP_POWER_COUNTER);
            this.captured = true;
        } else if (this.captured) {
            this.captured = false;
            ObfuscationReflectionHelper.setPrivateValue(EntityPlayerSP.class, player, this.frozenCounter, JUMP_POWER_COUNTER);
            ObfuscationReflectionHelper.setPrivateValue(EntityPlayerSP.class, player, 0.0F, JUMP_POWER);
        }
    }

    private boolean captured;

    /** 1.20's {@code renderJumpMeter}: the cooldown bar over the background while the dash cools down. */
    @SubscribeEvent
    public void overlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.JUMPBAR) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (coolingCamel(mc.player) == null) {
            return;
        }
        int x = event.getResolution().getScaledWidth() / 2 - 91;
        int y = event.getResolution().getScaledHeight() - 32 + 3;
        mc.getTextureManager().bindTexture(COOLDOWN);
        GlStateManager.enableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        Gui.drawModalRectWithCustomSizedTexture(x, y, 0.0F, 0.0F, 182, 5, 182.0F, 5.0F);
        GlStateManager.disableBlend();
        mc.getTextureManager().bindTexture(Gui.ICONS);
    }

    /**
     * 1.20's {@code CamelRenderer}: shadow 0.7, one texture. While Core's {@code VanillaRevisions.babyModels()}
     * (26.1 installed, as with Game Drops Backport), a baby is 26.3's: {@code BabyCamelModel} and
     * {@code camel_baby.png}.
     */
    static final class Render extends RenderLiving<EntityCamel> {
        private final ModelCamel adult;
        private final ModelCamel baby = new ModelCamel(true);

        Render(RenderManager manager) {
            super(manager, new ModelCamel(), 0.7F);
            this.adult = (ModelCamel) this.mainModel;
        }

        /** @return true when this one is drawn as 26.1's baby */
        static boolean modernBaby(EntityCamel entity) {
            return entity.isChild() && VanillaRevisions.babyModels();
        }

        @Override
        public void doRender(EntityCamel entity, double x, double y, double z, float entityYaw, float partialTicks) {
            this.mainModel = modernBaby(entity) ? this.baby : this.adult;
            super.doRender(entity, x, y, z, entityYaw, partialTicks);
        }

        @Override
        protected ResourceLocation getEntityTexture(EntityCamel entity) {
            return modernBaby(entity) ? BABY_TEXTURE : TEXTURE;
        }
    }
}
