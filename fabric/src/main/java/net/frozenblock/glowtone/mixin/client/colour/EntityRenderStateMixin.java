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

package net.frozenblock.glowtone.mixin.client.colour;

import net.frozenblock.glowtone.render.GlowtoneChromaFold;
import net.frozenblock.glowtone.render.GlowtoneChromaTinted;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(EntityRenderState.class)
public abstract class EntityRenderStateMixin implements GlowtoneChromaTinted {
	@Unique
	private int glowtone$chromaTint = GlowtoneChromaFold.NO_TINT;

	@Override
	public int glowtone$chromaTint() {
		return this.glowtone$chromaTint;
	}

	@Override
	public void glowtone$setChromaTint(int tint) {
		this.glowtone$chromaTint = tint;
	}
}
