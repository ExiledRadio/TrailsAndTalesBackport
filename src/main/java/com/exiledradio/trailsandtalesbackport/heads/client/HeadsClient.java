package com.exiledradio.trailsandtalesbackport.heads.client;

import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import com.exiledradio.trailsandtalesbackport.heads.HeadsContent;
import com.exiledradio.trailsandtalesbackport.heads.TilePiglinHead;

import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.layers.LayerCustomHead;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/** Stream Hd's client: the piglin head's block and item renderers and the worn-head layer. */
public final class HeadsClient {

    /** Renderers already given a {@link LayerPiglinHead} (or found to need none). */
    private final Set<RenderLivingBase<?>> seen = Collections.newSetFromMap(new IdentityHashMap<RenderLivingBase<?>, Boolean>());

    private HeadsClient() {
    }

    public static void init() {
        if (!HeadsContent.enabled()) {
            return;
        }
        ClientRegistry.bindTileEntitySpecialRenderer(TilePiglinHead.class, new RenderPiglinHead());
        MinecraftForge.EVENT_BUS.register(new HeadsClient());
    }

    @SubscribeEvent
    public void registerModels(ModelRegistryEvent event) {
        if (HeadsContent.headItem != null) {
            HeadsContent.headItem.setTileEntityItemStackRenderer(new RenderPiglinHead.ItemRenderer());
        }
    }

    /**
     * Before an entity is drawn: remembers it (for the item renderer) and, the first time its renderer is
     * seen, adds a worn-piglin-head layer beside each {@code LayerCustomHead} it has. Renderers are reached
     * as they are used, so those another mod adds late get the layer too.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void preRender(RenderLivingEvent.Pre<?> event) {
        RenderPiglinHead.drawing = event.getEntity();
        RenderLivingBase<?> renderer = event.getRenderer();
        if (this.seen.add(renderer)) {
            addLayers(renderer);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void postRender(RenderLivingEvent.Post<?> event) {
        RenderPiglinHead.drawing = null;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static void addLayers(RenderLivingBase<?> renderer) {
        try {
            List layers = ReflectionHelper.getPrivateValue(RenderLivingBase.class, renderer, "layerRenderers", "field_177097_h");
            List<LayerRenderer<?>> added = new ArrayList<LayerRenderer<?>>();
            for (Object layer : layers) {
                if (layer instanceof LayerCustomHead) {
                    ModelRenderer head = ReflectionHelper.getPrivateValue(LayerCustomHead.class,
                            (LayerCustomHead) layer, "modelRenderer", "field_177209_a");
                    if (head != null) {
                        added.add(new LayerPiglinHead(head));
                    }
                }
            }
            layers.addAll(added);
        } catch (RuntimeException e) {
            TrailsAndTalesBackport.LOGGER.warn("Worn piglin heads won't show on {}", renderer.getClass().getName(), e);
        }
    }
}
