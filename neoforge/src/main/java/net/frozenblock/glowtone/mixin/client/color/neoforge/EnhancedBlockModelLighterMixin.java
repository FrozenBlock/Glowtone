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

package net.frozenblock.glowtone.mixin.client.color.neoforge;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.QuadInstance;
import net.frozenblock.glowtone.config.option.ao.AmbientOcclusionOption;
import net.frozenblock.glowtone.config.GlowtoneDebugEntries;
import net.frozenblock.glowtone.config.option.edge.EdgeHighlightOption;
import net.frozenblock.glowtone.light.color.render.ChromaBaker;
import net.frozenblock.glowtone.light.edge.EdgeNeighbours;
import net.frozenblock.glowtone.light.edge.impl.NeoForgeMutableQuad;
import net.frozenblock.glowtone.light.occlusion.OcclusionOverrideHelper;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.ao.EnhancedBlockModelLighter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.frozenblock.glowtone.material.shader.MaterialShaderPatcher;

@Mixin(EnhancedBlockModelLighter.class)
public class EnhancedBlockModelLighterMixin {
	@Inject(method = "prepareQuadAmbientOcclusion", at = @At("TAIL"))
	private void glowtone$buildAmbientOcclusionEdges(
		BlockAndTintGetter level,
		BlockState state,
		BlockPos pos,
		BakedQuad quad,
		QuadInstance outputInstance,
		CallbackInfo info
	) {
		glowtone$buildEdges(level, state, pos, quad, outputInstance, true);
	}

	@Inject(method = "prepareQuadFlat", at = @At("TAIL"))
	private void glowtone$pinFlatQuadColor(
		BlockAndTintGetter level,
		BlockState state,
		BlockPos pos,
		int lightCoords,
		BakedQuad quad,
		QuadInstance outputInstance,
		CallbackInfo info
	) {
		final boolean relative = lightCoords == -1 && EnhancedBlockModelLighter.class.cast(this).faceCubic;
		final Direction direction = relative ? quad.direction() : null;
		if (direction == null) {
			ChromaBaker.beginFlatQuad(pos.getX(), pos.getY(), pos.getZ());
		} else {
			ChromaBaker.beginFlatQuad(
				pos.getX() + direction.getStepX(),
				pos.getY() + direction.getStepY(),
				pos.getZ() + direction.getStepZ()
			);
		}
		glowtone$buildEdges(level, state, pos, quad, outputInstance, quad.materialInfo().ambientOcclusion());
	}

	private static void glowtone$buildEdges(
		BlockAndTintGetter level,
		BlockState blockState,
		BlockPos pos,
		BakedQuad quad,
		QuadInstance outputInstance,
		boolean ambientOcclusion
	) {
		final ChromaBaker.SectionState section = ChromaBaker.state();
		if (MaterialShaderPatcher.anyQuadOffset()) {
			final float[] positions = section.quadPositions();
			for (int vertex = 0; vertex < 4; vertex++) {
				positions[vertex * 3] = quad.position(vertex).x();
				positions[vertex * 3 + 1] = quad.position(vertex).y();
				positions[vertex * 3 + 2] = quad.position(vertex).z();
			}

			section.beginQuadOffsets(positions);
		}

		final boolean building = ChromaBaker.buildingSection();
		final boolean highlight = (building ? section.highlightEnabled() : EdgeHighlightOption.enabled()) && ambientOcclusion;
		final boolean shade = building
			? section.contactShading()
			: (AmbientOcclusionOption.glowtoneActive() && AmbientOcclusionOption.SHADER_CONTACT_SHADING)
				|| GlowtoneDebugEntries.enabled(GlowtoneDebugEntries.AMBIENT_OCCLUSION);
		final boolean bake = AmbientOcclusionOption.BAKED_CONTACT_SHADING && !shade && AmbientOcclusionOption.glowtoneActive();
		if (!highlight && !shade && !bake) return;

		final EdgeNeighbours neighbours = section.edgeNeighbours();
		neighbours.gather(level, pos);
		section.pendingEdges().set(new NeoForgeMutableQuad(quad, outputInstance), neighbours, highlight, shade, bake);
		section.beginQuadEdges();
	}

	@ModifyArg(
		method = {"calculateAxisAligned", "calculateIrregular"},
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/util/ARGB;gray(F)I"
		),
		index = 0
	)
	private float glowtone$clampCorner(float occlusion, @Local(argsOnly = true, name = "state") BlockState state) {
		final float clamped = Mth.clamp(occlusion, 0F, 1F);
		final boolean vanilla = ChromaBaker.buildingSection()
			? ChromaBaker.vanillaOcclusionActive()
			: AmbientOcclusionOption.vanillaActive();
		if (!vanilla) return clamped;

		return OcclusionOverrideHelper.receives(state, true) ? clamped : 1F;
	}
}
