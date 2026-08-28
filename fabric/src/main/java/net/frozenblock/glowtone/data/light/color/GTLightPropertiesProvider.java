package net.frozenblock.glowtone.data.light.color;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.frozenblock.glowtone.GlowtoneConstants;
import net.frozenblock.glowtone.data.light.color.block.BlockLightProperties;
import net.frozenblock.glowtone.data.light.color.block.BlockLightPropertiesGenerators;
import net.frozenblock.glowtone.data.light.color.block.MultiVariantGenerator;
import net.frozenblock.glowtone.light.color.GlowtoneTransmittance;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

@Environment(EnvType.CLIENT)
public final class GTLightPropertiesProvider extends LightPropertiesProvider {

	public GTLightPropertiesProvider(PackOutput output) {
		super(output, GlowtoneConstants.MOD_ID);
	}

	@Override
	public void generateBlockLights(BlockLightPropertiesGenerators blockLights) {
		blockLights.createTrivialBlock(BlockLightProperties.color(0xFFB347), Blocks.TORCH, Blocks.WALL_TORCH);
		blockLights.createTrivialBlock(BlockLightProperties.color(0xFFC46B), Blocks.LANTERN);
		blockLights.createTrivialBlock(BlockLightProperties.color(0xFFA246), Blocks.CAMPFIRE);
		blockLights.createTrivialBlock(BlockLightProperties.color(0xFFAE3C), Blocks.JACK_O_LANTERN);
		blockLights.createTrivialBlock(BlockLightProperties.color(0xFF8C2A), Blocks.FIRE);

		blockLights.createTrivialBlock(BlockLightProperties.color(0x7FE6D2), Blocks.COPPER_TORCH, Blocks.COPPER_WALL_TORCH);
		blockLights.createTrivialBlock(BlockLightProperties.color(0x7FE6D2), Blocks.COPPER_LANTERN.asList().toArray(Block[]::new));
		blockLights.createTrivialBlock(BlockLightProperties.color(0xFFD9A8), Blocks.COPPER_BULB.asList().toArray(Block[]::new));

		blockLights.createTrivialBlock(
			BlockLightProperties.color(0x3FC7D6),
			Blocks.SOUL_TORCH, Blocks.SOUL_WALL_TORCH, Blocks.SOUL_LANTERN, Blocks.SOUL_FIRE, Blocks.SOUL_CAMPFIRE
		);

		blockLights.createTrivialBlock(BlockLightProperties.color(0xFF3020), Blocks.REDSTONE_TORCH, Blocks.REDSTONE_WALL_TORCH, Blocks.REDSTONE_BLOCK);
		blockLights.createTrivialBlock(BlockLightProperties.color(0xFF4A38), Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE);
		blockLights.createTrivialBlock(BlockLightProperties.color(0xFFB35C), Blocks.REDSTONE_LAMP);

		blockLights.createTrivialBlock(BlockLightProperties.color(0xFFD98A), Blocks.GLOWSTONE);
		blockLights.createTrivialBlock(BlockLightProperties.color(0xFFAF5E), Blocks.SHROOMLIGHT);
		blockLights.createTrivialBlock(BlockLightProperties.color(0xCFE8FF), Blocks.SEA_LANTERN, Blocks.BEACON);
		blockLights.createTrivialBlock(BlockLightProperties.color(0xA8EFE4), Blocks.CONDUIT);

		blockLights.createTrivialBlock(BlockLightProperties.color(0xFF7A1A), Blocks.LAVA, Blocks.LAVA_CAULDRON);
		blockLights.createTrivialBlock(BlockLightProperties.color(0xFF6A14), Blocks.MAGMA_BLOCK);

		blockLights.createTrivialBlock(BlockLightProperties.color(0xFFF3C0), Blocks.OCHRE_FROGLIGHT);
		blockLights.createTrivialBlock(BlockLightProperties.color(0xCFF5C0), Blocks.VERDANT_FROGLIGHT);
		blockLights.createTrivialBlock(BlockLightProperties.color(0xF8D8EC), Blocks.PEARLESCENT_FROGLIGHT);

		blockLights.createTrivialBlock(BlockLightProperties.color(0xF2E8FF), Blocks.END_ROD);
		blockLights.createTrivialBlock(BlockLightProperties.color(0xC7B6FF), Blocks.END_PORTAL, Blocks.END_GATEWAY);
		// TODO: only with eyes?
		blockLights.createTrivialBlock(BlockLightProperties.color(0xBCE8C4), Blocks.END_PORTAL_FRAME);
		blockLights.createTrivialBlock(BlockLightProperties.color(0xC8A2FF), Blocks.ENCHANTING_TABLE);
		blockLights.createTrivialBlock(BlockLightProperties.color(0x9B4DFF), Blocks.CRYING_OBSIDIAN);
		blockLights.createTrivialBlock(BlockLightProperties.color(0xB14DFF), Blocks.RESPAWN_ANCHOR, Blocks.DRAGON_EGG);
		blockLights.createTrivialBlock(BlockLightProperties.color(0xC9A0FF), Blocks.AMETHYST_CLUSTER, Blocks.LARGE_AMETHYST_BUD, Blocks.MEDIUM_AMETHYST_BUD, Blocks.SMALL_AMETHYST_BUD);

		blockLights.createTrivialBlock(BlockLightProperties.color(0xB6E39A), Blocks.GLOW_LICHEN);
		blockLights.createTrivialBlock(BlockLightProperties.color(0xFFCF63), Blocks.CAVE_VINES, Blocks.CAVE_VINES_PLANT);
		blockLights.createTrivialBlock(BlockLightProperties.color(0xA8D66A), Blocks.SEA_PICKLE);
		blockLights.createTrivialBlock(BlockLightProperties.color(0xE8C99A), Blocks.BROWN_MUSHROOM);
		blockLights.createTrivialBlock(BlockLightProperties.color(0xE8E06A), Blocks.FIREFLY_BUSH);

		blockLights.createTrivialBlock(BlockLightProperties.color(0x3DD9C8), Blocks.SCULK_CATALYST, Blocks.SCULK_SHRIEKER);
		blockLights.createTrivialBlock(BlockLightProperties.color(0x35C8E0), Blocks.SCULK_SENSOR, Blocks.CALIBRATED_SCULK_SENSOR);

		blockLights.createTrivialBlock(BlockLightProperties.color(0xFFA040), Blocks.FURNACE, Blocks.SMOKER, Blocks.BLAST_FURNACE);
		blockLights.createTrivialBlock(BlockLightProperties.color(0xFFCF9A), Blocks.BREWING_STAND);

		createTrialBlock(blockLights, Blocks.TRIAL_SPAWNER);
		createTrialBlock(blockLights, Blocks.VAULT);

		blockLights.createTrivialBlock(BlockLightProperties.color(0xFF7A3A), Blocks.CREAKING_HEART);

		// LIGHT AND TRANSMITTANCE
		blockLights.createTrivialBlock(BlockLightProperties.lightAndFilterColor(0xB24BFF, 0xC7F), Blocks.NETHER_PORTAL);

		// TRANSMITTANCE
		for (DyeColor dye : DyeColor.values()) {
			final int transmittanceColor = filterForDye(dye);
			blockLights.createTrivialBlock(BlockLightProperties.filterColor(transmittanceColor), Blocks.STAINED_GLASS.pick(dye), Blocks.STAINED_GLASS_PANE.pick(dye));
		}

		blockLights.createTrivialBlock(BlockLightProperties.filterColor(GlowtoneTransmittance.FULLY_TRANSMISSIVE), Blocks.GLASS, Blocks.GLASS_PANE);

		blockLights.createTrivialBlock(BlockLightProperties.filterColor(0x111), Blocks.TINTED_GLASS);

		blockLights.createTrivialBlock(BlockLightProperties.filterColor(0xACF), Blocks.WATER);
		blockLights.createTrivialBlock(BlockLightProperties.filterColor(0xDEF), Blocks.ICE, Blocks.PACKED_ICE, Blocks.FROSTED_ICE);
		blockLights.createTrivialBlock(BlockLightProperties.filterColor(0xCDF), Blocks.BLUE_ICE);

		blockLights.createTrivialBlock(BlockLightProperties.filterColor(0xFC6), Blocks.HONEY_BLOCK);
		blockLights.createTrivialBlock(BlockLightProperties.filterColor(0xBFB), Blocks.SLIME_BLOCK);
	}

	private static int filterForDye(DyeColor dye) {
		return switch (dye) {
			case WHITE -> 0xEEE;
			case ORANGE -> 0xF80;
			case MAGENTA -> 0xF4C;
			case LIGHT_BLUE -> 0x6CF;
			case YELLOW -> 0xFE2;
			case LIME -> 0x9F2;
			case PINK -> 0xF8A;
			case GRAY -> 0x555;
			case LIGHT_GRAY -> 0xAAA;
			case CYAN -> 0x2AB;
			case PURPLE -> 0x82D;
			case BLUE -> 0x24E;
			case BROWN -> 0x852;
			case GREEN -> 0x4A2;
			case RED -> 0xF11;
			case BLACK -> 0x111;
		};
	}

	private static void createTrialBlock(BlockLightPropertiesGenerators blockLights, Block block) {
		blockLights.blockStateOutput.accept(
			MultiVariantGenerator.dispatch(block)
				.with(BlockLightPropertiesGenerators.initial(BlockStateProperties.OMINOUS)
					.select(false, BlockLightProperties.color(255, 193, 149))
					.select(true, BlockLightProperties.color(51, 255, 255))
			)
		);
	}
}
