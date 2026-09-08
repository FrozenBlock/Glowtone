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

package net.frozenblock.glowtone.mixin.client.ao_edge;

import net.frozenblock.glowtone.light.edge.impl.BlockStateCasterBoxCache;
import net.frozenblock.glowtone.light.occlusion.impl.BlockStateAmbientOcclusionCache;
import net.frozenblock.glowtone.render.GlowtoneCasterShapes;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@ClientOnly
@Mixin(BlockBehaviour.BlockStateBase.class)
public class BlockStateBaseMixin implements BlockStateAmbientOcclusionCache, BlockStateCasterBoxCache {
	@Unique
	private boolean glowtone$hasAmbientOcclusion;
	@Unique
	private AABB[] glowtone$casterBoxes;

	@Unique
	@Override
	public void glowtone$setHasAmbientOcclusion(boolean hasAmbientOcclusion) {
		this.glowtone$hasAmbientOcclusion = hasAmbientOcclusion;
	}

	@Unique
	@Override
	public boolean glowtone$hasAmbientOcclusion() {
		return this.glowtone$hasAmbientOcclusion;
	}

	@Unique
	@Override
	public @Nullable AABB[] glowtone$getOrCreateCasterBoxes(BlockGetter level, BlockPos pos) {
		if (this.glowtone$casterBoxes != null) return this.glowtone$casterBoxes;
		if (!((Object)this instanceof BlockState blockState)) return null;

		this.glowtone$casterBoxes = BlockStateCasterBoxCache.glowtone$boxesFromShape(GlowtoneCasterShapes.of(level, pos, blockState));
		return this.glowtone$casterBoxes;
	}

	@Unique
	@Override
	public void glowtone$clearCasterBoxes() {
		this.glowtone$casterBoxes = null;
	}
}
