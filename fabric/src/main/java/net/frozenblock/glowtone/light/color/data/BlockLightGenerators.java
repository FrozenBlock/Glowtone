package net.frozenblock.glowtone.light.color.data;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.level.block.Block;
import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
public class BlockLightGenerators {
	public final Consumer<BlockLightGenerator> blockStateOutput;

	public BlockLightGenerators(Consumer<BlockLightGenerator> blockStateOutput) {
		this.blockStateOutput = blockStateOutput;
	}

	public static MultiVariantGenerator createSimpleBlock(Block block, BlockLight light) {
		return MultiVariantGenerator.dispatch(block, light);
	}

	public void createTrivialBlock(BlockLight light, Block... blocks) {
		for (Block block : blocks) this.blockStateOutput.accept(createSimpleBlock(block, light));
	}
}
