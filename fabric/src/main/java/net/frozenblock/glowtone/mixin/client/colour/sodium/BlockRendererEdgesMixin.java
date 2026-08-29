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

package net.frozenblock.glowtone.mixin.client.colour.sodium;

import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.Material;
import net.caffeinemc.mods.sodium.client.render.model.MutableQuadViewImpl;
import net.frozenblock.glowtone.render.sodium.GlowtoneSodiumContext;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.frozenblock.glowtone.render.sodium.GlowtoneSodiumEdges;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(BlockRenderer.class)
public class BlockRendererEdgesMixin {

	@Inject(method = "renderModel", at = @At("HEAD"))
	private void glowtone$captureModelBoxes(BlockStateModel model, BlockState state, BlockPos pos, BlockPos origin, CallbackInfo info) {
		GlowtoneSodiumEdges.beginBlock(
			model, state, ((GlowtoneSodiumContext) this).glowtone$level(), pos);
	}

	@Inject(method = "bufferQuad", at = @At("HEAD"))
	private void glowtone$buildEdges(MutableQuadViewImpl quad, float[] brightnesses, Material material, CallbackInfo info) {
		final GlowtoneSodiumContext context = (GlowtoneSodiumContext) this;
		GlowtoneSodiumEdges.begin(quad, context.glowtone$level(), context.glowtone$pos());
	}
}
