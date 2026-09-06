package net.frozenblock.glowtone.light.data.block;

import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.world.level.block.Block;

@ClientOnly
public interface BlockLightPropertiesGenerator {
	Block block();

	BlockLightPropertiesDispatcher create();
}
