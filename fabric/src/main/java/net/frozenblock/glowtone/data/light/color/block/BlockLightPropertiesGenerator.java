package net.frozenblock.glowtone.data.light.color.block;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.level.block.Block;

@Environment(EnvType.CLIENT)
public interface BlockLightPropertiesGenerator {
	Block block();

	BlockLightPropertiesDispatcher create();
}
