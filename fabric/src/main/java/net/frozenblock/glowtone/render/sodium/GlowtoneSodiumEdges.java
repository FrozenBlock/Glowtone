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

package net.frozenblock.glowtone.render.sodium;

import net.caffeinemc.mods.sodium.client.render.model.MutableQuadViewImpl;
import net.frozenblock.glowtone.light.color.render.ChromaBaker;
import net.frozenblock.glowtone.light.edge.EdgeNeighbours;
import net.frozenblock.glowtone.render.GlowtoneModelBoxes;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public final class GlowtoneSodiumEdges {
	private GlowtoneSodiumEdges() {
	}

	public static void beginBlock(
		BlockStateModel model, BlockState blockState, @Nullable BlockAndTintGetter level, BlockPos pos
	) {
		final ChromaBaker.SectionState state = ChromaBaker.state();
		state.setModelFaces(null);

		if (!state.highlightEnabled() && !state.contactShading()) return;
		if (level == null) return;
		if (EdgeNeighbours.isBlockLike(blockState.getOcclusionShape())) return;

		state.setModelFaces(
			GlowtoneModelBoxes.forState(model, level, pos, blockState, blockState.getSeed(pos)));
	}

	public static void begin(
		MutableQuadViewImpl quad, @Nullable BlockAndTintGetter level, @Nullable BlockPos pos
	) {
		final ChromaBaker.SectionState state = ChromaBaker.state();
		state.setEmissiveQuad(((GlowtoneSodiumQuad) quad).glowtone$emissive());

		final boolean shade = state.contactShading();
		final boolean highlight = state.highlightEnabled() && quad.ambientOcclusion().toBoolean(true);
		if (!highlight && !shade) return;

		final EdgeNeighbours neighbours = state.edgeNeighbours();
		if (level == null || pos == null) {
			neighbours.markDirty();
		} else {
			neighbours.gather(level, pos);
		}

		final float[] positions = state.quadPositions();
		for (int vertex = 0; vertex < 4; vertex++) {
			positions[vertex * 3] = quad.getX(vertex);
			positions[vertex * 3 + 1] = quad.getY(vertex);
			positions[vertex * 3 + 2] = quad.getZ(vertex);
		}

		state.pendingEdges().setQuad(positions, quad.getLightFace(), neighbours, highlight, shade);
		state.beginQuadEdges();
	}
}
