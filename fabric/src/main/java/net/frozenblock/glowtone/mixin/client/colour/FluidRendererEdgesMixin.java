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

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.frozenblock.glowtone.config.option.edge.EdgeHighlightOption;
import net.frozenblock.glowtone.light.color.render.ChromaBaker;
import net.frozenblock.glowtone.light.edge.EdgeNeighbours;
import net.frozenblock.glowtone.light.edge.FluidEdges;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.FluidRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(FluidRenderer.class)
public class FluidRendererEdgesMixin {

	@Inject(method = "tesselate", at = @At("HEAD"))
	private void glowtone$beginFluid(
		BlockAndTintGetter level,
		BlockPos pos,
		FluidRenderer.Output output,
		BlockState blockState,
		FluidState fluidState,
		CallbackInfo info
	) {
		if (!EdgeHighlightOption.enabled()) return;
		ChromaBaker.state().beginFluid(level, pos);
	}

	@Inject(method = "tesselate", at = @At("RETURN"))
	private void glowtone$endFluid(
		BlockAndTintGetter level,
		BlockPos pos,
		FluidRenderer.Output output,
		BlockState blockState,
		FluidState fluidState,
		CallbackInfo info
	) {
		ChromaBaker.state().endFluid();
	}

	@Inject(method = "addFace", at = @At("RETURN"))
	private void glowtone$fluidRims(
		VertexConsumer builder,
		float x0, float y0, float z0, float u0, float v0,
		float x1, float y1, float z1, float u1, float v1,
		float x2, float y2, float z2, float u2, float v2,
		float x3, float y3, float z3, float u3, float v3,
		int color,
		int lightCoords,
		boolean addBackFace,
		CallbackInfo info
	) {
		if (!EdgeHighlightOption.enabled()) return;

		final ChromaBaker.SectionState state = ChromaBaker.state();
		final BlockAndTintGetter level = state.fluidLevel();
		if (level == null) return;

		final BlockPos pos = state.fluidPos();
		final int originX = pos.getX() & 15;
		final int originY = pos.getY() & 15;
		final int originZ = pos.getZ() & 15;

		final FluidEdges edges = state.fluidEdges();
		edges.quad(
			x0, y0, z0, u0, v0,
			x1, y1, z1, u1, v1,
			x2, y2, z2, u2, v2,
			x3, y3, z3, u3, v3,
			color,
			lightCoords
		);
		if (!edges.locate(originX, originY, originZ)) return;

		final EdgeNeighbours neighbours = state.edgeNeighbours();
		neighbours.gather(level, pos);
		edges.emit(state, builder, neighbours, originX, originY, originZ);
	}
}
