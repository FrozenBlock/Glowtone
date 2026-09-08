package net.frozenblock.glowtone.material.impl;

import net.frozenblock.glowtone.data.BlockMaterial;
import net.mehvahdjukaar.candlelight.api.ClientOnly;

@ClientOnly
public interface BlockMaterialAttachment {
	BlockMaterial.Baked glowtone$getMaterial();

	void glowtone$setMaterial(BlockMaterial.Baked material);
}
