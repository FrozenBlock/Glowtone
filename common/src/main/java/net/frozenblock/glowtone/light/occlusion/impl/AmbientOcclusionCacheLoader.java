/*
 * Copyright 2026 FrozenBlock
 * This file is part of Glowtone.
 *
 * This program is free software; you can modify it under
 * the terms of version 1 of the FrozenBlock Modding Oasis License
 * as published by FrozenBlock Modding Oasis.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * FrozenBlock Modding Oasis License for more details.
 *
 * You should have received a copy of the FrozenBlock Modding Oasis License
 * along with this program; if not, see <https://github.com/FrozenBlock/Licenses>.
 */

package net.frozenblock.glowtone.light.occlusion.impl;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.frozenblock.glowtone.render.GlowtoneModelBoxes;
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
		GlowtoneModelBoxes.clear();

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
