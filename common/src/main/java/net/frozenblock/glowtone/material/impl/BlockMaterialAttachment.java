package net.frozenblock.glowtone.material.impl;

import net.frozenblock.glowtone.data.BlockMaterial;
import net.mehvahdjukaar.candlelight.api.ClientOnly;

@ClientOnly
public interface BlockMaterialAttachment {

	default BlockMaterial.Baked glowtone$getMaterial() {
		throw new AssertionError();
	}

	default void glowtone$setMaterial(BlockMaterial.Baked material) {
		throw new AssertionError();
	}
}
