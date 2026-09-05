package net.frozenblock.glowtone.tag;

import net.frozenblock.glowtone.GlowtoneConstants;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public final class GlowtoneBlockTags {

	private static TagKey<Block> bind(String name) {
		return TagKey.create(Registries.BLOCK, GlowtoneConstants.id(name));
	}

	private GlowtoneBlockTags() {}
}
