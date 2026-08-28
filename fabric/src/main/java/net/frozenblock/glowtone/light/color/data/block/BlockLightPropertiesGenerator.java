package net.frozenblock.glowtone.light.color.data.block;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.level.block.Block;

@Environment(EnvType.CLIENT)
public interface BlockLightPropertiesGenerator {
	Block block();

	BlockLightPropertiesDispatcher create();
}
