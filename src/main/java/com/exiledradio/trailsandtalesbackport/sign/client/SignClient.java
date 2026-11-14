package com.exiledradio.trailsandtalesbackport.sign.client;

import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import com.exiledradio.trailsandtalesbackport.sign.SignsContent;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiEditSign;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Stream Sg's client side.
 *
 * <ul>
 *   <li>Vanilla's sign editor (opened by placing any sign) is swapped for {@link SignEditScreen}, on the
 *       front face.</li>
 *   <li>The server's "open the editor" packet opens {@link SignEditScreen} on the face it names.</li>
 *   <li>Renderers that draw both faces are bound in post-init, after every mod's own (Village & Pillage
 *       binds its dyed-sign and wood-sign renderers while models register): {@link StandardSignRenderer}
 *       for vanilla's tile, this mod's wood sign tile and Village & Pillage's, and
 *       {@link HangingSignSidesRenderer} for hanging signs.</li>
 * </ul>
 */
public final class SignClient {

    private SignClient() {
    }

    public static void init() {
        if (SignsContent.enabled()) {
            MinecraftForge.EVENT_BUS.register(new SignClient());
        }
    }

    public static void postInit() {
        if (!SignsContent.enabled()) {
            return;
        }
        StandardSignRenderer standard = new StandardSignRenderer();
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntitySign.class, standard);
        bind(com.exiledradio.trailsandtalesbackport.common.wood.WoodSign.Tile.class, standard);
        try {
            Class<?> theirs = Class.forName("com.exiledradio.villagerbackport.building.TileEntityWoodSign", false,
                    SignClient.class.getClassLoader());
            if (TileEntitySign.class.isAssignableFrom(theirs)) {
                bind(theirs.asSubclass(TileEntitySign.class), standard);
            }
        } catch (ClassNotFoundException absent) {
            // Village & Pillage is not installed.
        }
        ClientRegistry.bindTileEntitySpecialRenderer(com.exiledradio.trailsandtalesbackport.hangingsign.TileHangingSign.class,
                new HangingSignSidesRenderer());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void bind(Class<? extends TileEntitySign> type, TileEntitySpecialRenderer<TileEntitySign> renderer) {
        ClientRegistry.bindTileEntitySpecialRenderer((Class) type, (TileEntitySpecialRenderer) renderer);
    }

    /** The server's packet 31: open the editor on that face of the sign there. */
    public static void open(final BlockPos pos, final boolean front) {
        final Minecraft mc = Minecraft.getMinecraft();
        mc.addScheduledTask(() -> {
            if (mc.world == null) {
                return;
            }
            TileEntity tile = mc.world.getTileEntity(pos);
            if (tile instanceof TileEntitySign) {
                mc.displayGuiScreen(new SignEditScreen((TileEntitySign) tile, front));
            }
        });
    }

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        if (event.getGui() != null && event.getGui().getClass() == GuiEditSign.class) {
            try {
                TileEntitySign sign = ObfuscationReflectionHelper.getPrivateValue(GuiEditSign.class,
                        (GuiEditSign) event.getGui(), "field_146848_f");
                if (sign != null) {
                    event.setGui(new SignEditScreen(sign, true));
                }
            } catch (RuntimeException e) {
                TrailsAndTalesBackport.LOGGER.warn("Could not swap vanilla's sign editor for 1.20's", e);
            }
        }
    }
}
