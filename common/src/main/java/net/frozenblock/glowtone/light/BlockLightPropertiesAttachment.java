package net.frozenblock.glowtone.light;

import net.frozenblock.glowtone.light.data.block.BlockLightProperties;

public interface BlockLightPropertiesAttachment {
	BlockLightProperties.Baked glowtone$getProperties();
	void glowtone$setProperties(BlockLightProperties.Baked properties);
}
