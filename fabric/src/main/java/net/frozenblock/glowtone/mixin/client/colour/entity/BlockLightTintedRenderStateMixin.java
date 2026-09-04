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

package net.frozenblock.glowtone.mixin.client.colour.entity;

import net.frozenblock.glowtone.light.color.render.ChromaFold;
import net.frozenblock.glowtone.light.color.render.impl.BlockLightTinted;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@ClientOnly
@Mixin({
	EntityRenderState.class,
	BlockEntityRenderState.class
})
public class BlockLightTintedRenderStateMixin implements BlockLightTinted {
	@Unique
	private int glowtone$blockLightTint = ChromaFold.NO_TINT;

	@Unique
	@Override
	public int glowtone$blockLightTint() {
		return this.glowtone$blockLightTint;
	}

	@Unique
	@Override
	public void glowtone$setBlockLightTint(int tint) {
		this.glowtone$blockLightTint = tint;
	}
}
