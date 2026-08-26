package net.frozenblock.glowtone.light.color.data;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.level.block.Block;

@Environment(EnvType.CLIENT)
public interface BlockLightGenerator {
	Block block();

	BlockLightDispatcher create();
}
