package com.exiledradio.trailsandtalesbackport.selftest;

import com.exiledradio.trailsandtalesbackport.common.wood.WoodSign;
import com.exiledradio.trailsandtalesbackport.hangingsign.HangingSignContent;
import com.exiledradio.trailsandtalesbackport.hangingsign.HangingWood;
import com.exiledradio.trailsandtalesbackport.hangingsign.TileHangingSign;
import com.exiledradio.trailsandtalesbackport.sign.SignData;
import com.exiledradio.trailsandtalesbackport.sign.SignEditing;
import com.exiledradio.trailsandtalesbackport.sign.SignNetwork;
import com.exiledradio.trailsandtalesbackport.sign.SignSides;
import com.exiledradio.trailsandtalesbackport.sign.SignsContent;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.block.BlockStandingSign;
import net.minecraft.block.BlockWallSign;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.world.GameType;

/** Stream Sg: 1.20's sign changes. */
public final class SignSelfTest implements SelfTest {

    @Override
    public String name() {
        return "sign";
    }

    @Override
    public void run(Check c) {
        if (!c.ok(SignsContent.enabled() && SignData.CAP != null, "signs stream on, capability registered")) {
            return;
        }
        BlockPos area = c.area();
        BlockPos at = area.add(8, 0, 8);
        c.world.setBlockState(at, Blocks.STANDING_SIGN.getDefaultState().withProperty(BlockStandingSign.ROTATION, 0));
        TileEntitySign sign = (TileEntitySign) c.world.getTileEntity(at);
        c.ok(SignSides.data(sign) != null, "vanilla's sign tile has the data");
        c.ok(SignSides.data(new WoodSign.Tile()) != null, "our wood sign tile has the data");
        c.ok(SignSides.data(new TileHangingSign()) != null, "hanging sign tile has the data");
        c.ok(SignSides.data(new net.minecraft.tileentity.TileEntityChest()) == null, "other tiles do not");

        nbt(c, sign);
        packets(c, at);
        facing(c, area);
        editing(c, area);
        applicators(c, area);
        waxing(c, area);
        commands(c, area);
        hanging(c, area);
    }

    // --- saving -----------------------------------------------------------------------------------

    private static void nbt(Check c, TileEntitySign sign) {
        sign.signText[0] = new TextComponentString("Front");
        SignSides.lines(sign, false)[1] = new TextComponentString("Back");
        SignSides.setColor(sign, true, EnumDyeColor.RED);
        SignSides.setColor(sign, false, EnumDyeColor.SILVER);
        SignSides.setGlowing(sign, true, true);
        SignSides.setWaxed(sign, true);
        NBTTagCompound tag = sign.writeToNBT(new NBTTagCompound());
        NBTTagCompound ours = tag.getCompoundTag("ForgeCaps").getCompoundTag(SignData.KEY.toString());
        c.eq("light_gray", ours.getCompoundTag("back_text").getString("color"), "1.20 tag: back_text.color by 1.14 name");
        c.eq(true, ours.getBoolean("is_waxed"), "1.20 tag: is_waxed");
        c.eq(4, ours.getCompoundTag("back_text").getTagList("messages", 8).tagCount(), "1.20 tag: four back messages");
        c.eq("red", tag.getCompoundTag("ForgeData").getString(SignSides.FRONT_COLOR_KEY), "front color in Village & Pillage's key");

        TileEntitySign back = new TileEntitySign();
        back.readFromNBT(tag);
        c.eq("Front", back.signText[0].getUnformattedText(), "front text round trip");
        c.eq("Back", SignSides.lines(back, false)[1].getUnformattedText(), "back text round trip");
        c.eq("", SignSides.lines(back, false)[0].getUnformattedText(), "empty back line round trip");
        c.eq(EnumDyeColor.RED, SignSides.color(back, true), "front color round trip");
        c.eq(EnumDyeColor.SILVER, SignSides.color(back, false), "back color round trip");
        c.eq(true, SignSides.glowing(back, true), "front glow round trip");
        c.eq(false, SignSides.glowing(back, false), "back glow round trip");
        c.eq(true, SignSides.waxed(back), "wax round trip");

        // The update packet a client reads is the same save.
        TileEntitySign client = new TileEntitySign();
        client.readFromNBT(sign.getUpdateTag());
        c.eq("Back", SignSides.lines(client, false)[1].getUnformattedText(), "back text reaches clients in the update tag (the update packet's contents)");
        c.eq(true, SignSides.waxed(client), "wax reaches clients");

        // A sign saved before this stream: black, not glowing, not waxed, empty back.
        NBTTagCompound old = new NBTTagCompound();
        old.setString("id", "minecraft:sign");
        old.setString("Text1", "{\"text\":\"old\"}");
        TileEntitySign legacy = new TileEntitySign();
        legacy.readFromNBT(old);
        c.eq(EnumDyeColor.BLACK, SignSides.color(legacy, false), "an old sign's back is black");
        c.eq(false, SignSides.waxed(legacy), "an old sign is not waxed");
        c.eq("", SignSides.lines(legacy, false)[3].getUnformattedText(), "an old sign's back is empty");

        // Reset for the later tests.
        SignSides.setWaxed(sign, false);
        SignSides.setGlowing(sign, true, false);
        SignSides.setColor(sign, true, EnumDyeColor.BLACK);
        SignSides.setColor(sign, false, EnumDyeColor.BLACK);
    }

    // --- packets ----------------------------------------------------------------------------------

    private static void packets(Check c, BlockPos at) {
        SignNetwork.Update u = new SignNetwork.Update(at, false, new String[] {"a", "", "long line", "d"});
        ByteBuf buf = Unpooled.buffer();
        u.toBytes(buf);
        SignNetwork.Update u2 = new SignNetwork.Update();
        u2.fromBytes(buf);
        c.eq(at, u2.pos, "update packet: position");
        c.eq(false, u2.front, "update packet: face");
        c.eq("long line", u2.lines[2], "update packet: lines");
        c.eq(0, buf.readableBytes(), "update packet: nothing left over");

        StringBuilder huge = new StringBuilder();
        for (int i = 0; i < 500; i++) {
            huge.append('x');
        }
        ByteBuf big = Unpooled.buffer();
        new SignNetwork.Update(at, true, new String[] {huge.toString(), "", "", ""}).toBytes(big);
        SignNetwork.Update capped = new SignNetwork.Update();
        capped.fromBytes(big);
        c.eq(SignEditing.MAX_LINE, capped.lines[0].length(), "update packet: lines capped at 384 characters");

        SignNetwork.Open o = new SignNetwork.Open(at, false);
        ByteBuf ob = Unpooled.buffer();
        o.toBytes(ob);
        SignNetwork.Open o2 = new SignNetwork.Open();
        o2.fromBytes(ob);
        c.eq(at, o2.pos, "open packet: position");
        c.eq(false, o2.front, "open packet: face");
    }

    // --- which face -------------------------------------------------------------------------------

    private static void facing(Check c, BlockPos area) {
        BlockPos at = area.add(4, 0, 20);
        c.world.setBlockState(at, Blocks.STANDING_SIGN.getDefaultState().withProperty(BlockStandingSign.ROTATION, 0));
        TileEntitySign sign = (TileEntitySign) c.world.getTileEntity(at);
        EntityPlayerMP p = Players.real(c.world, "facer");
        p.setPosition(at.getX() + 0.5, at.getY(), at.getZ() + 3.5);
        c.ok(SignSides.isFacingFront(sign, p), "rotation 0 faces south: a player to the south sees the front");
        p.setPosition(at.getX() + 0.5, at.getY(), at.getZ() - 3.5);
        c.ok(!SignSides.isFacingFront(sign, p), "a player to the north sees the back");
        c.world.setBlockState(at, Blocks.STANDING_SIGN.getDefaultState().withProperty(BlockStandingSign.ROTATION, 4));
        sign = (TileEntitySign) c.world.getTileEntity(at);
        p.setPosition(at.getX() - 3.5, at.getY(), at.getZ() + 0.5);
        c.ok(SignSides.isFacingFront(sign, p), "rotation 4 faces west");

        BlockPos wall = area.add(10, 1, 20);
        c.world.setBlockState(wall.south(), Blocks.STONE.getDefaultState());
        c.world.setBlockState(wall, Blocks.WALL_SIGN.getDefaultState().withProperty(BlockWallSign.FACING, EnumFacing.NORTH));
        TileEntitySign ws = (TileEntitySign) c.world.getTileEntity(wall);
        p.setPosition(wall.getX() + 0.5, wall.getY(), wall.getZ() - 2.5);
        c.ok(SignSides.isFacingFront(ws, p), "a wall sign facing north: front from the north");
        p.setPosition(wall.getX() + 0.5, wall.getY(), wall.getZ() + 0.95);
        c.ok(!SignSides.isFacingFront(ws, p), "behind its board (wall side): the back");
    }

    // --- editing ----------------------------------------------------------------------------------

    private static void editing(Check c, BlockPos area) {
        BlockPos at = area.add(8, 0, 8);
        TileEntitySign sign = (TileEntitySign) c.world.getTileEntity(at);
        EntityPlayerMP p = Players.real(c.world, "editor");
        p.setGameType(GameType.SURVIVAL);
        p.setPosition(at.getX() + 0.5, at.getY(), at.getZ() + 2.5);

        // Right-clicking with an empty hand, through the server's own right-click path (and so the Forge event).
        click(p, at, ItemStack.EMPTY);
        SignData d = SignSides.data(sign);
        c.eq(p.getUniqueID(), d.editor, "right-clicking a placed sign opens the editor for that player");
        c.ok(SignEditing.update(p, at, true, new String[] {"one", "two", "§cthree", ""}), "front text accepted from the editor");
        c.eq("one", sign.signText[0].getUnformattedText(), "front line 1 set");
        c.eq("three", sign.signText[2].getUnformattedText(), "formatting codes stripped");
        c.ok(d.editor == null, "editing ends with the update");
        c.ok(!SignEditing.update(p, at, true, new String[] {"x", "", "", ""}), "a second update without opening is refused");
        c.eq("one", sign.signText[0].getUnformattedText(), "and changes nothing");

        // From behind: the back.
        p.setPosition(at.getX() + 0.5, at.getY(), at.getZ() - 2.5);
        click(p, at, ItemStack.EMPTY);
        c.ok(SignEditing.update(p, at, false, new String[] {"", "behind", "", ""}), "back text accepted from behind");
        c.eq("behind", SignSides.lines(sign, false)[1].getUnformattedText(), "back line set");
        c.eq("one", sign.signText[0].getUnformattedText(), "front untouched by the back's edit");

        // Another player may not send text for someone else's open editor.
        click(p, at, ItemStack.EMPTY);
        EntityPlayerMP q = Players.real(c.world, "intruder");
        q.setPosition(at.getX() + 0.5, at.getY(), at.getZ() - 2.5);
        c.ok(!SignEditing.update(q, at, false, new String[] {"no", "", "", ""}), "another player's text is refused");
        // Too far away.
        p.setPosition(at.getX() + 0.5, at.getY(), at.getZ() - 12.5);
        c.ok(!SignEditing.update(p, at, false, new String[] {"far", "", "", ""}), "text from more than 8 blocks away is refused");
        p.setPosition(at.getX() + 0.5, at.getY(), at.getZ() - 2.5);
        SignSides.data(sign).editor = null;

        // While someone else (in the world, near) edits, nobody else opens it.
        click(q, at, ItemStack.EMPTY);
        c.world.playerEntities.add(q);
        try {
            click(p, at, ItemStack.EMPTY);
            c.eq(q.getUniqueID(), SignSides.data(sign).editor, "the sign stays with the player editing it");
        } finally {
            c.world.playerEntities.remove(q);
        }
        click(p, at, ItemStack.EMPTY);
        c.eq(p.getUniqueID(), SignSides.data(sign).editor, "an editor who left frees the sign");
        SignSides.data(sign).editor = null;

        // Adventure mode: no editing.
        p.setGameType(GameType.ADVENTURE);
        click(p, at, ItemStack.EMPTY);
        c.ok(SignSides.data(sign).editor == null, "no editing in Adventure mode");
        p.setGameType(GameType.SURVIVAL);

        // Text that is not plain (translated, from commands) is never opened.
        BlockPos t = area.add(14, 0, 8);
        c.world.setBlockState(t, Blocks.STANDING_SIGN.getDefaultState());
        TileEntitySign translated = (TileEntitySign) c.world.getTileEntity(t);
        translated.signText[0] = new TextComponentTranslation("tile.stone.name");
        p.setPosition(t.getX() + 0.5, t.getY(), t.getZ() + 2.5);
        click(p, t, ItemStack.EMPTY);
        c.ok(SignSides.data(translated).editor == null, "a sign with translated text is not editable");

        // Placing a sign: vanilla's editor mark, our screen's front update accepted.
        BlockPos floor = area.add(20, -1, 8);
        p.setPosition(floor.getX() + 0.5, floor.getY() + 1, floor.getZ() + 2.5);
        ItemStack signs = new ItemStack(Items.SIGN, 4);
        p.setHeldItem(EnumHand.MAIN_HAND, signs);
        p.interactionManager.processRightClickBlock(p, c.world, signs, EnumHand.MAIN_HAND, floor, EnumFacing.UP, 0.5F, 1.0F, 0.5F);
        TileEntity placed = c.world.getTileEntity(floor.up());
        c.ok(placed instanceof TileEntitySign && ((TileEntitySign) placed).getPlayer() == p, "a placed sign opens the editor for the placer");
        c.ok(SignEditing.update(p, floor.up(), true, new String[] {"placed", "", "", ""}), "the placer's first text accepted");
        c.eq("placed", ((TileEntitySign) placed).signText[0].getUnformattedText(), "placed sign's text set");
        c.ok(!SignEditing.update(p, floor.up(), true, new String[] {"again", "", "", ""}), "and only once");
        p.setHeldItem(EnumHand.MAIN_HAND, ItemStack.EMPTY);

        // Sneaking with an item: the item is used, the sign is not touched.
        p.setPosition(at.getX() + 0.5, at.getY(), at.getZ() + 2.5);
        p.setSneaking(true);
        click(p, at, new ItemStack(Items.DYE, 1, EnumDyeColor.RED.getDyeDamage()));
        p.setSneaking(false);
        c.eq(EnumDyeColor.BLACK, SignSides.color(sign, true), "sneaking with dye does not dye");
        c.ok(SignSides.data(sign).editor == null, "nor open the editor");
    }

    // --- dye, glow ink, ink sac -------------------------------------------------------------------

    private static void applicators(Check c, BlockPos area) {
        BlockPos at = area.add(8, 0, 8);
        TileEntitySign sign = (TileEntitySign) c.world.getTileEntity(at);
        EntityPlayerMP p = Players.real(c.world, "dyer");
        p.setGameType(GameType.SURVIVAL);
        p.setPosition(at.getX() + 0.5, at.getY(), at.getZ() + 2.5);

        ItemStack red = new ItemStack(Items.DYE, 3, EnumDyeColor.RED.getDyeDamage());
        click(p, at, red);
        c.eq(EnumDyeColor.RED, SignSides.color(sign, true), "red dye on the front");
        c.eq(EnumDyeColor.BLACK, SignSides.color(sign, false), "the back stays black");
        c.eq(2, red.getCount(), "one dye used");
        c.ok(SignSides.data(sign).editor == null, "dyeing does not open the editor");
        click(p, at, red);
        c.eq(2, red.getCount(), "dyeing it the same color uses nothing");
        c.eq(p.getUniqueID(), SignSides.data(sign).editor, "a dye that changes nothing opens the editor (1.20)");
        SignSides.data(sign).editor = null;

        p.setPosition(at.getX() + 0.5, at.getY(), at.getZ() - 2.5);
        ItemStack blue = new ItemStack(Items.DYE, 1, EnumDyeColor.LIGHT_BLUE.getDyeDamage());
        click(p, at, blue);
        c.eq(EnumDyeColor.LIGHT_BLUE, SignSides.color(sign, false), "dye from behind colors the back");
        c.eq(EnumDyeColor.RED, SignSides.color(sign, true), "the front keeps its color");

        // A face with no text takes no dye (1.20: canApplyToSign needs a message); the editor opens instead.
        BlockPos blank = area.add(26, 0, 8);
        c.world.setBlockState(blank, Blocks.STANDING_SIGN.getDefaultState());
        TileEntitySign empty = (TileEntitySign) c.world.getTileEntity(blank);
        p.setPosition(blank.getX() + 0.5, blank.getY(), blank.getZ() + 2.5);
        ItemStack green = new ItemStack(Items.DYE, 1, EnumDyeColor.GREEN.getDyeDamage());
        click(p, blank, green);
        c.eq(EnumDyeColor.BLACK, SignSides.color(empty, true), "no dye on a face without text");
        c.eq(1, green.getCount(), "the dye is kept");
        c.eq(p.getUniqueID(), SignSides.data(empty).editor, "the editor opens instead");
        SignSides.data(empty).editor = null;

        // Glow ink (Caves & Cliffs' item; a stand-in here), then the ink sac.
        Item saved = SignsContent.glowInkSac;
        SignsContent.glowInkSac = Items.GLOWSTONE_DUST;
        try {
            p.setPosition(at.getX() + 0.5, at.getY(), at.getZ() + 2.5);
            ItemStack glow = new ItemStack(Items.GLOWSTONE_DUST, 2);
            click(p, at, glow);
            c.eq(true, SignSides.glowing(sign, true), "glow ink makes the front glow");
            c.eq(false, SignSides.glowing(sign, false), "not the back");
            c.eq(1, glow.getCount(), "one glow ink sac used");
            click(p, at, glow);
            c.eq(1, glow.getCount(), "glowing text again uses nothing");
            ItemStack ink = new ItemStack(Items.DYE, 2, EnumDyeColor.BLACK.getDyeDamage());
            click(p, at, ink);
            c.eq(false, SignSides.glowing(sign, true), "the ink sac stops the glow");
            c.eq(EnumDyeColor.RED, SignSides.color(sign, true), "and leaves the color");
            c.eq(1, ink.getCount(), "one ink sac used");
            if (!SignsContent.modernDyes) {
                click(p, at, ink);
                c.eq(EnumDyeColor.BLACK, SignSides.color(sign, true), "without 1.14's dyes, the ink sac on unlit text is black dye");
            }
        } finally {
            SignsContent.glowInkSac = saved;
        }
        // Creative uses nothing.
        p.setGameType(GameType.CREATIVE);
        ItemStack lime = new ItemStack(Items.DYE, 1, EnumDyeColor.LIME.getDyeDamage());
        click(p, at, lime);
        c.eq(EnumDyeColor.LIME, SignSides.color(sign, true), "dyeing in creative");
        c.eq(1, lime.getCount(), "uses no dye");
        p.setGameType(GameType.SURVIVAL);
    }

    // --- waxing -----------------------------------------------------------------------------------

    private static void waxing(Check c, BlockPos area) {
        BlockPos at = area.add(8, 0, 8);
        TileEntitySign sign = (TileEntitySign) c.world.getTileEntity(at);
        EntityPlayerMP p = Players.real(c.world, "waxer");
        p.setGameType(GameType.SURVIVAL);
        p.setPosition(at.getX() + 0.5, at.getY(), at.getZ() + 2.5);
        SignSides.data(sign).editor = null;

        Item saved = SignsContent.honeycomb;
        SignsContent.honeycomb = Items.SLIME_BALL;
        try {
            ItemStack comb = new ItemStack(Items.SLIME_BALL, 2);
            click(p, at, comb);
            c.eq(true, SignSides.waxed(sign), "honeycomb waxes the sign");
            c.eq(1, comb.getCount(), "one honeycomb used");
            click(p, at, comb);
            c.eq(1, comb.getCount(), "a waxed sign takes no more");

            click(p, at, ItemStack.EMPTY);
            c.ok(SignSides.data(sign).editor == null, "a waxed sign does not open for editing");
            SignSides.data(sign).editor = p.getUniqueID();
            c.ok(!SignEditing.update(p, at, true, new String[] {"waxed", "", "", ""}), "a waxed sign refuses new text");
            SignSides.data(sign).editor = null;
            EnumDyeColor before = SignSides.color(sign, true);
            ItemStack dye = new ItemStack(Items.DYE, 1, EnumDyeColor.PURPLE.getDyeDamage());
            click(p, at, dye);
            c.eq(before, SignSides.color(sign, true), "a waxed sign takes no dye");
            c.eq(1, dye.getCount(), "and the dye is kept");

            // Waxing a sign with no text works (1.20's honeycomb needs no message).
            BlockPos blank = area.add(26, 0, 14);
            c.world.setBlockState(blank, Blocks.STANDING_SIGN.getDefaultState());
            p.setPosition(blank.getX() + 0.5, blank.getY(), blank.getZ() + 2.5);
            click(p, blank, comb);
            c.eq(true, SignSides.waxed((TileEntitySign) c.world.getTileEntity(blank)), "an empty sign can be waxed");

            // Without Buzzy Bees there is no honeycomb, so nothing waxes.
            SignsContent.honeycomb = null;
            BlockPos other = area.add(26, 0, 20);
            c.world.setBlockState(other, Blocks.STANDING_SIGN.getDefaultState());
            p.setPosition(other.getX() + 0.5, other.getY(), other.getZ() + 2.5);
            click(p, other, new ItemStack(Items.SLIME_BALL));
            c.eq(false, SignSides.waxed((TileEntitySign) c.world.getTileEntity(other)), "no honeycomb, no waxing");
        } finally {
            SignsContent.honeycomb = saved;
        }
        c.eq(net.minecraftforge.fml.common.Loader.isModLoaded("buzzybeesbackport"), SignsContent.honeycomb != null,
                "honeycomb found by name iff Buzzy Bees is installed");
    }

    // --- click commands ---------------------------------------------------------------------------

    private static void commands(Check c, BlockPos area) {
        BlockPos at = area.add(4, 0, 26);
        c.world.setBlockState(at, Blocks.STANDING_SIGN.getDefaultState());
        TileEntitySign sign = (TileEntitySign) c.world.getTileEntity(at);
        BlockPos target = area.add(4, 5, 26);
        BlockPos backTarget = area.add(5, 5, 26);
        sign.signText[0] = new TextComponentString("run").setStyle(new Style().setClickEvent(new ClickEvent(
                ClickEvent.Action.RUN_COMMAND, "/setblock " + target.getX() + " " + target.getY() + " " + target.getZ() + " stone")));
        SignSides.lines(sign, false)[0] = new TextComponentString("back").setStyle(new Style().setClickEvent(new ClickEvent(
                ClickEvent.Action.RUN_COMMAND, "/setblock " + backTarget.getX() + " " + backTarget.getY() + " " + backTarget.getZ() + " stone")));
        EntityPlayerMP p = Players.real(c.world, "clicker");
        p.setGameType(GameType.SURVIVAL);
        p.setPosition(at.getX() + 0.5, at.getY(), at.getZ() + 2.5);
        click(p, at, ItemStack.EMPTY);
        c.eq(Blocks.STONE, c.world.getBlockState(target).getBlock(), "the front's click command runs");
        c.ok(c.world.isAirBlock(backTarget), "not the back's");
        c.ok(SignSides.data(sign).editor == null, "a sign with commands runs them instead of opening");
        p.setPosition(at.getX() + 0.5, at.getY(), at.getZ() - 2.5);
        click(p, at, ItemStack.EMPTY);
        c.eq(Blocks.STONE, c.world.getBlockState(backTarget).getBlock(), "from behind, the back's runs");
        // Waxed: commands still run.
        c.world.setBlockToAir(target);
        SignSides.setWaxed(sign, true);
        p.setPosition(at.getX() + 0.5, at.getY(), at.getZ() + 2.5);
        click(p, at, ItemStack.EMPTY);
        c.eq(Blocks.STONE, c.world.getBlockState(target).getBlock(), "a waxed sign still runs its commands");
    }

    // --- hanging signs ----------------------------------------------------------------------------

    private static void hanging(Check c, BlockPos area) {
        if (HangingSignContent.CEILING.isEmpty()) {
            return;
        }
        BlockPos roof = area.add(16, 4, 26);
        c.world.setBlockState(roof, Blocks.STONE.getDefaultState());
        BlockPos at = roof.down();
        c.world.setBlockState(at, HangingSignContent.CEILING.get(HangingWood.OAK).getDefaultState());
        TileEntitySign sign = (TileEntitySign) c.world.getTileEntity(at);
        c.ok(SignSides.isHanging(sign.getBlockType()), "hanging signs are known as hanging (their screen)");
        EntityPlayerMP p = Players.real(c.world, "hangingeditor");
        p.setGameType(GameType.SURVIVAL);
        p.setPosition(at.getX() + 0.5, at.getY() - 1, at.getZ() - 2.5);
        c.ok(!SignSides.isFacingFront(sign, p), "a hanging sign at rotation 0: the back from the north");
        click(p, at, ItemStack.EMPTY);
        c.ok(SignEditing.update(p, at, false, new String[] {"hung", "", "", ""}), "a hanging sign's back is editable");
        c.eq("hung", SignSides.lines(sign, false)[0].getUnformattedText(), "hanging sign back text set");
        NBTTagCompound tag = sign.writeToNBT(new NBTTagCompound());
        TileHangingSign back = new TileHangingSign();
        back.readFromNBT(tag);
        c.eq("hung", SignSides.lines(back, false)[0].getUnformattedText(), "hanging sign back text round trip");
    }

    private static void click(EntityPlayerMP p, BlockPos pos, ItemStack held) {
        p.setHeldItem(EnumHand.MAIN_HAND, held);
        p.interactionManager.processRightClickBlock(p, p.world, held, EnumHand.MAIN_HAND, pos, EnumFacing.NORTH, 0.5F, 0.5F, 0.5F);
    }
}
