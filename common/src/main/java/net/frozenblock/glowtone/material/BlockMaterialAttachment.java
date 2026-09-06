package net.frozenblock.glowtone.material;

import net.frozenblock.glowtone.data.BlockMaterial;

public interface BlockMaterialAttachment {
	BlockMaterial.Baked glowtone$getMaterial();

	void glowtone$setMaterial(BlockMaterial.Baked material);
}
