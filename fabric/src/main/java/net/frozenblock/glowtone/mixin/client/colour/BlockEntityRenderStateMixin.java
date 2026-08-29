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

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.frozenblock.glowtone.light.color.render.ChromaFold;
import net.frozenblock.glowtone.light.color.render.impl.GlowtoneChromaTinted;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(BlockEntityRenderState.class)
public class BlockEntityRenderStateMixin implements GlowtoneChromaTinted {
	@Unique
	private int glowtone$chromaTint = ChromaFold.NO_TINT;

	@Unique
	@Override
	public int glowtone$chromaTint() {
		return this.glowtone$chromaTint;
	}

	@Unique
	@Override
	public void glowtone$setChromaTint(int tint) {
		this.glowtone$chromaTint = tint;
	}

	@Inject(method = "extractBase", at = @At("TAIL"))
	private static void glowtone$resolveChromaTint(
		BlockEntity blockEntity,
		BlockEntityRenderState state,
		ModelFeatureRenderer.CrumblingOverlay breakProgress,
		CallbackInfo info
	) {
		state.glowtone$setChromaTint(ChromaFold.resolveBlockEntity(state.blockPos, state.lightCoords));
	}
}
