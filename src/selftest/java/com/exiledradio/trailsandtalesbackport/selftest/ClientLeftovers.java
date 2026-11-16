package com.exiledradio.trailsandtalesbackport.selftest;

import com.exiledradio.backportcore.presentation.client.GameMusic;
import com.exiledradio.trailsandtalesbackport.TrailsAndTalesBackport;
import com.exiledradio.trailsandtalesbackport.archaeology.ArchaeologyContent;
import com.exiledradio.trailsandtalesbackport.asm.Asm;
import com.exiledradio.trailsandtalesbackport.lookfeel.client.ArmPoses;
import com.exiledradio.trailsandtalesbackport.lookfeel.client.DimensionScreen;
import com.exiledradio.trailsandtalesbackport.lookfeel.client.LookHooks;
import com.exiledradio.trailsandtalesbackport.music.client.BiomeMusicPack;
import com.exiledradio.trailsandtalesbackport.sign.client.OtherSignRenderer;
import com.exiledradio.trailsandtalesbackport.sign.client.WaxParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundEventAccessor;
import net.minecraft.client.gui.GuiDownloadTerrain;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.ScreenShotHelper;

import java.io.File;
import java.util.function.BiConsumer;

/**
 * The client half of {@code LeftoversSelfTest}, run in the client check's test world: the biome music sound lists
 * and step, patch P1 (Ctrl + pick block), the dimension screen's portal backgrounds (photographed), wax-on particles,
 * the 1.20.5 shield arm and the brushing arm in third person (photographed), other mods' sign renderers.
 */
final class ClientLeftovers {

    private final Minecraft mc;
    private final File shots;
    private final BiConsumer<Boolean, String> ok;
    private int frames;

    ClientLeftovers(Minecraft mc, File shots, BiConsumer<Boolean, String> ok) {
        this.mc = mc;
        this.shots = shots;
        this.ok = ok;
    }

    private void ok(boolean condition, String what) {
        this.ok.accept(condition, what);
    }

    private void shot(String name) {
        ScreenShotHelper.saveScreenshot(this.shots, name, this.mc.displayWidth, this.mc.displayHeight, this.mc.getFramebuffer());
        File f = new File(new File(this.shots, "screenshots"), name);
        File g = new File(this.shots, name);
        ok((f.isFile() && f.length() > 1000) || (g.isFile() && g.length() > 1000), "saved " + name);
    }

    /** @return true when done */
    boolean tick(int t) {
        if (t == 1) {
            instant();
            this.mc.displayGuiScreen(new GuiDownloadTerrain());
            DimensionScreen.reason = DimensionScreen.Reason.NETHER_PORTAL;
            this.frames = DimensionScreen.framesDrawn;
        } else if (t == 30) {
            ok(DimensionScreen.framesDrawn > this.frames, "the nether portal screen was drawn: " + (DimensionScreen.framesDrawn - this.frames));
            shot("nether_portal.png");
            DimensionScreen.reason = DimensionScreen.Reason.END_PORTAL;
            this.frames = DimensionScreen.framesDrawn;
        } else if (t == 60) {
            ok(DimensionScreen.framesDrawn > this.frames, "the end portal screen was drawn: " + (DimensionScreen.framesDrawn - this.frames));
            shot("end_portal.png");
            DimensionScreen.reason = DimensionScreen.Reason.OTHER;
            this.mc.displayGuiScreen(null);
            this.mc.gameSettings.thirdPersonView = 2;
            this.mc.player.rotationPitch = 30.0F;
            this.mc.player.setHeldItem(EnumHand.OFF_HAND, new ItemStack(Items.SHIELD));
            this.mc.player.setActiveHand(EnumHand.OFF_HAND);
            this.frames = ArmPoses.blockPosed;
        } else if (t == 100) {
            ok(ArmPoses.blockPosed > this.frames, "the shield arm follows the head: " + (ArmPoses.blockPosed - this.frames));
            shot("shield.png");
            this.mc.player.stopActiveHand();
            this.mc.player.setHeldItem(EnumHand.OFF_HAND, ItemStack.EMPTY);
            if (ArchaeologyContent.brush != null) {
                this.mc.player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(ArchaeologyContent.brush));
                this.mc.player.setActiveHand(EnumHand.MAIN_HAND);
            }
            this.frames = ArmPoses.brushPosed;
        } else if (t == 130) {
            ok(ArchaeologyContent.brush == null || ArmPoses.brushPosed > this.frames,
                    "the brushing arm pose: " + (ArmPoses.brushPosed - this.frames));
            shot("brush.png");
            this.mc.player.stopActiveHand();
            this.mc.gameSettings.thirdPersonView = 0;
            int before = WaxParticles.spawned;
            WaxParticles.spawn(this.mc.world, this.mc.player.getPosition().up(2));
            int n = WaxParticles.spawned - before;
            ok(n >= 18 && n <= 30, "wax-on: three to five on each face: " + n);
            return true;
        }
        return false;
    }

    private void instant() {
        // Biome music.
        ok(BiomeMusicPack.installed, "the biome music pack is installed");
        SoundEventAccessor desert = this.mc.getSoundHandler().getAccessor(new ResourceLocation(TrailsAndTalesBackport.MOD_ID, "music.overworld.desert"));
        ok(desert != null && desert.getWeight() >= 3, "music.overworld.desert has its sounds (weight " + (desert == null ? -1 : desert.getWeight()) + ")");
        SoundEventAccessor grove = this.mc.getSoundHandler().getAccessor(new ResourceLocation(TrailsAndTalesBackport.MOD_ID, "music.overworld.cherry_grove"));
        ok(grove != null && grove.getWeight() >= 7, "music.overworld.cherry_grove: Bromeliad and Echo in the Wind x3, Left to Bloom (" + (grove == null ? -1 : grove.getWeight()) + ")");
        ok(GameMusic.names().contains("1.20 biome music"), "the 1.20 music step is registered: " + GameMusic.names());

        // P1.
        boolean p1 = false;
        for (String a : Asm.APPLIED) {
            p1 |= a.startsWith("P1 ");
        }
        ok(p1, "P1 is applied (Minecraft.storeTEInStack): " + Asm.APPLIED);
        ItemStack chest = new ItemStack(Blocks.CHEST);
        NBTTagCompound block = new NBTTagCompound();
        block.setString("CustomName", "Loot");
        chest.setTagInfo("BlockEntityTag", block);
        NBTTagCompound display = new NBTTagCompound();
        NBTTagList lore = new NBTTagList();
        lore.appendTag(new NBTTagString("(+NBT)"));
        display.setTag("Lore", lore);
        chest.setTagInfo("display", display);
        ItemStack picked = LookHooks.pickedBlock(chest);
        ok(!picked.getTagCompound().getCompoundTag("display").hasKey("Lore"), "Ctrl + pick block: no (+NBT)");
        ok("Loot".equals(picked.getDisplayName()), "Ctrl + pick block: a named chest gives a named item (" + picked.getDisplayName() + ")");

        // The dimension screen's reasons (1.20.6's determineLevelLoadingReason).
        ok(DimensionScreen.reasonFor(false, 0, -1) == DimensionScreen.Reason.NETHER_PORTAL, "to the Nether: nether portal");
        ok(DimensionScreen.reasonFor(false, -1, 0) == DimensionScreen.Reason.NETHER_PORTAL, "from the Nether: nether portal");
        ok(DimensionScreen.reasonFor(false, 0, 1) == DimensionScreen.Reason.END_PORTAL, "to the End: end portal");
        ok(DimensionScreen.reasonFor(false, 1, 0) == DimensionScreen.Reason.END_PORTAL, "from the End: end portal");
        ok(DimensionScreen.reasonFor(true, -1, 0) == DimensionScreen.Reason.OTHER, "respawning after death: the menu background");
        ok(DimensionScreen.reasonFor(false, 0, 7) == DimensionScreen.Reason.OTHER, "another dimension: the menu background");

        // Other mods' signs: none in the dev client, so nothing is wrapped (vanilla's and ours are not).
        ok(OtherSignRenderer.WRAPPED.isEmpty(), "no sign renderer of this mod or vanilla's is wrapped: " + OtherSignRenderer.WRAPPED);
    }
}
