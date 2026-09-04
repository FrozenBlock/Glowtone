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

package net.frozenblock.glowtone.mixin.client.ao_edge.fabric;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.AltModelBlockRendererImpl;
import net.frozenblock.glowtone.config.GlowtoneDebugEntries;
import net.frozenblock.glowtone.config.option.ao.AmbientOcclusionOption;
import net.frozenblock.glowtone.config.option.edge.EdgeHighlightOption;
import net.frozenblock.glowtone.light.color.render.ChromaBaker;
import net.frozenblock.glowtone.light.edge.EdgeNeighbours;
import net.frozenblock.glowtone.render.GlowtoneModelBoxes;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@ClientOnly
@Mixin(value = AltModelBlockRendererImpl.class, priority = 1001)
public class AltModelBlockRendererImplMixin {
	@Shadow
	private BlockAndTintGetter level;

	@Shadow
	private BlockPos pos;

	@Inject(method = "tesselateBlock", at = @At("HEAD"), require = 0)
	private void glowtone$captureModelBoxes(
		QuadEmitter output,
		float x, float y, float z,
		BlockAndTintGetter level,
		BlockPos pos,
		BlockState blockState,
		BlockStateModel model,
		long seed,
		CallbackInfo info
	) {
		final ChromaBaker.SectionState state = ChromaBaker.state();
		state.setModelFaces(null);

		if (!EdgeHighlightOption.enabled()
			&& !AmbientOcclusionOption.glowtoneActive()
			&& !GlowtoneDebugEntries.enabled(GlowtoneDebugEntries.AMBIENT_OCCLUSION)) {
			return;
		}
		if (EdgeNeighbours.isBlockLike(blockState.getOcclusionShape())) return;

		state.setModelFaces(GlowtoneModelBoxes.forState(model, level, pos, blockState, seed));
	}

	@ModifyReturnValue(method = "transform", at = @At("RETURN"), require = 0)
	private boolean glowtone$captureNeighbors(boolean original, MutableQuadView quad) {
		if (!original) return original;

		final ChromaBaker.SectionState state = ChromaBaker.state();
		final boolean highlight = EdgeHighlightOption.enabled() && quad.ambientOcclusion().orElse(true);
		final boolean glowtoneAo = AmbientOcclusionOption.glowtoneActive();
		final boolean shade = (glowtoneAo && AmbientOcclusionOption.SHADER_CONTACT_SHADING) || GlowtoneDebugEntries.enabled(GlowtoneDebugEntries.AMBIENT_OCCLUSION);
		final boolean bake = glowtoneAo && AmbientOcclusionOption.BAKED_CONTACT_SHADING && !shade;

		if (highlight || shade || bake) {
			final EdgeNeighbours neighbours = state.edgeNeighbours();
			if (this.level == null || this.pos == null) {
				neighbours.markDirty();
			} else {
				neighbours.gather(this.level, this.pos);
			}
			state.pendingEdges().set(quad, neighbours, highlight, shade, bake);
			state.beginQuadEdges();
		}

		return original;
	}
}
