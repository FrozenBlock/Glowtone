package net.frozenblock.glowtone.light.impl;

import net.frozenblock.glowtone.light.data.block.BlockLightProperties;

public interface BlockLightPropertiesAttachment {

	default BlockLightProperties.Baked glowtone$getLightProperties() {
		throw new AssertionError();
	}

	default void glowtone$setLightProperties(BlockLightProperties.Baked properties) {
		throw new AssertionError();
	}
}
