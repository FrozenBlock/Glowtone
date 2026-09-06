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

import net.frozenblock.glowtone.light.occlusion.impl.BlockStateAmbientOcclusionCache;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@ClientOnly
@Mixin(BlockBehaviour.BlockStateBase.class)
public class BlockStateBaseMixin implements BlockStateAmbientOcclusionCache {
	@Unique
	private boolean glowtone$hasAmbientOcclusion;

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
}
