package net.frozenblock.glowtone.light.edge.impl;

import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

@ClientOnly
public final class CasterBoxCacheReloader implements ResourceManagerReloadListener {

	@Override
	public void onResourceManagerReload(ResourceManager resourceManager) {
		BuiltInRegistries.BLOCK.forEach(block -> block.getStateDefinition().getPossibleStates().forEach(BlockStateCasterBoxCache::glowtone$clearCasterBoxes));
	}
}
