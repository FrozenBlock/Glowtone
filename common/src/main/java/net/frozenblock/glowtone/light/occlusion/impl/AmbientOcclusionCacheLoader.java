package net.frozenblock.glowtone.light.occlusion.impl;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.RandomSource;
import java.util.List;

@ClientOnly
public final class AmbientOcclusionCacheLoader implements ResourceManagerReloadListener {

	@Override
	public void onResourceManagerReload(ResourceManager resourceManager) {
		final RandomSource random = RandomSource.createThreadLocalInstance(0L);
		final List<BlockStateModelPart> parts = new ObjectArrayList<>();
		final BlockStateModelSet modelSet = Minecraft.getInstance().getModelManager().getBlockStateModelSet();

		BuiltInRegistries.BLOCK.forEach(block -> {
			block.getStateDefinition().getPossibleStates().forEach(state -> {
				modelSet.get(state).collectParts(random, parts);
				state.glowtone$setHasAmbientOcclusion(!parts.isEmpty() && parts.getFirst().useAmbientOcclusion());
				parts.clear();
			});
		});
	}
}
