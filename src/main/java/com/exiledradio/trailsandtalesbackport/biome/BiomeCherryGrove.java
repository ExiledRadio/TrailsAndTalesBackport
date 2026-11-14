package com.exiledradio.trailsandtalesbackport.biome;

import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.passive.EntityRabbit;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeDecorator;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 1.20's cherry grove ({@code worldgen/biome/cherry_grove.json}, from the 1.20.6 jar's data generator) as a 1.12.2
 * biome.
 *
 * <ul>
 *   <li><b>Climate</b>: temperature 0.5, downfall 0.8, rain. The sky is {@code 0x7BA4FF}, which 1.12.2's own
 *       formula gives for temperature 0.5; the fog {@code 0xC0D8FF} is 1.12.2's default.</li>
 *   <li><b>Colors</b>: grass and foliage {@code 0xB6DB61}; water and underwater fog {@code 0x5DB7EF}. 1.12.2's water
 *       texture is blue and the biome color multiplies it, so the tint is the color divided by 1.13's blue, channel by
 *       channel: every channel of 1.20's color is above 1.13's blue, so the tint is white, the texture as it is. With
 *       1.13's gray texture (Update Aquatic) the exact color is given ({@link CherryGroveContent#waterColor}).</li>
 *   <li><b>Spawns</b>: pigs (1, 1-2), rabbits (2, 2-6), sheep (2, 2-4); the common monsters and bats, which are 1.12.2's
 *       defaults already; no squid (1.20's {@code water_creature} list is empty). Bees come from the nests on the
 *       trees, as in 1.20 (Buzzy Bees Backport).</li>
 *   <li><b>Height</b>: 1.12.2's extreme hills' (base 1.0, variation 0.5), the land it takes over
 *       ({@link CherryGroveLayer}), so the terrain is the same block for block with or without this biome.</li>
 *   <li><b>Surface</b>: grass over dirt (1.20's surface rules give the cherry grove nothing of its own).</li>
 *   <li><b>Decoration</b>: {@link CherryGroveDecorator}.</li>
 * </ul>
 */
public class BiomeCherryGrove extends Biome {

    public static final String NAME = "cherry_grove";

    /** 1.20.6's numbers. */
    public static final float TEMPERATURE = 0.5F;
    public static final float DOWNFALL = 0.8F;
    public static final int GRASS_COLOR = 0xB6DB61;
    public static final int FOLIAGE_COLOR = 0xB6DB61;
    public static final int WATER_COLOR = 0x5DB7EF;
    public static final int WATER_FOG_COLOR = 0x5DB7EF;
    public static final int SKY_COLOR = 0x7BA4FF;
    public static final int FOG_COLOR = 0xC0D8FF;

    /** 1.12.2's extreme hills, whose ground the grove takes. */
    public static final float BASE_HEIGHT = 1.0F;
    public static final float HEIGHT_VARIATION = 0.5F;

    /** 1.13's default water color, which the 1.12.2 tint is worked out against. */
    static final int MODERN_DEFAULT_WATER = 0x3F76E4;

    /** The 1.12.2 tint over the blue water texture: 1.20's color divided by 1.13's blue, each channel at most 255. */
    public static final int WATER_TINT = tint(WATER_COLOR, MODERN_DEFAULT_WATER);

    public BiomeCherryGrove() {
        super(new BiomeProperties("Cherry Grove").setBaseHeight(BASE_HEIGHT).setHeightVariation(HEIGHT_VARIATION)
                .setTemperature(TEMPERATURE).setRainfall(DOWNFALL).setWaterColor(WATER_TINT));

        this.spawnableCreatureList.clear();
        this.spawnableCreatureList.add(new SpawnListEntry(EntityPig.class, 1, 1, 2));
        this.spawnableCreatureList.add(new SpawnListEntry(EntityRabbit.class, 2, 2, 6));
        this.spawnableCreatureList.add(new SpawnListEntry(EntitySheep.class, 2, 2, 4));
        this.spawnableWaterCreatureList.clear();
    }

    static int tint(int color, int base) {
        int r = Math.min(255, ((color >> 16) & 0xFF) * 255 / ((base >> 16) & 0xFF));
        int g = Math.min(255, ((color >> 8) & 0xFF) * 255 / ((base >> 8) & 0xFF));
        int b = Math.min(255, (color & 0xFF) * 255 / (base & 0xFF));
        return r << 16 | g << 8 | b;
    }

    @Override
    public BiomeDecorator createBiomeDecorator() {
        return getModdedBiomeDecorator(new CherryGroveDecorator());
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getSkyColorByTemp(float temperature) {
        return SKY_COLOR;
    }

    /**
     * 1.20's {@code Biome.BIOME_INFO_NOISE} (one octave of simplex noise seeded 2345): 1.12.2's
     * {@code GRASS_COLOR_NOISE} is the same noise from the same seed.
     */
    public static double infoNoise(double x, double z) {
        return GRASS_COLOR_NOISE.getValue(x, z);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getGrassColorAtPos(BlockPos pos) {
        return getModdedBiomeGrassColor(GRASS_COLOR);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getFoliageColorAtPos(BlockPos pos) {
        return getModdedBiomeFoliageColor(FOLIAGE_COLOR);
    }
}
