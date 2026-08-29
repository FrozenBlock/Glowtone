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

import net.caffeinemc.mods.sodium.api.util.ColorARGB;
import net.caffeinemc.mods.sodium.client.model.color.ColorProvider;
import net.caffeinemc.mods.sodium.client.model.light.data.QuadLightData;
import net.caffeinemc.mods.sodium.client.model.quad.ModelQuadView;
import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuilder;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.DefaultFluidRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.Material;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.TranslucentGeometryCollector;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.frozenblock.glowtone.light.color.render.ChromaBaker;
import net.frozenblock.glowtone.light.edge.EdgeNeighbours;
import net.frozenblock.glowtone.light.edge.FluidEdges;
import net.frozenblock.glowtone.GlowtoneConstants;
import net.minecraft.tags.FluidTags;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Environment(EnvType.CLIENT)
@Mixin(DefaultFluidRenderer.class)
public class DefaultFluidRendererMixin {

	@Shadow
	@Final
	private int[] quadColors;

	@Shadow
	@Final
	private float[] brightness;

	@Shadow
	@Final
	private QuadLightData quadLightData;

	@Inject(method = "render", at = @At("HEAD"))
	private void glowtone$beginFluid(
		LevelSlice level,
		BlockState blockState,
		FluidState fluidState,
		BlockPos blockPos,
		BlockPos offset,
		TranslucentGeometryCollector collector,
		ChunkModelBuilder meshBuilder,
		Material material,
		ColorProvider<FluidState> colorProvider,
		FluidModel sprites,
		CallbackInfo info
	) {
		ChromaBaker.state().setEmissiveQuad(GlowtoneConstants.GLOWTONE_EMISSIVES && fluidState.is(FluidTags.LAVA));

		final ChromaBaker.SectionState state = ChromaBaker.state();
		if (!state.highlightEnabled()) return;

		state.beginFluid(level, blockPos);
	}

	@Inject(method = "render", at = @At("RETURN"))
	private void glowtone$endFluid(CallbackInfo info) {
		ChromaBaker.state().endFluid();
	}

	@Inject(method = "writeQuad", at = @At("RETURN"))
	private void glowtone$fluidEdges(
		ChunkModelBuilder builder,
		TranslucentGeometryCollector collector,
		Material material,
		BlockPos offset,
		ModelQuadView quad,
		ModelQuadFacing facing,
		boolean flip,
		CallbackInfo info
	) {
		if (flip) return;

		final ChromaBaker.SectionState state = ChromaBaker.state();
		if (!state.highlightEnabled()) return;

		final BlockAndTintGetter level = state.fluidLevel();
		if (level == null) return;

		final BlockPos fluidPos = state.fluidPos();
		final int originX = fluidPos.getX() & 15;
		final int originY = fluidPos.getY() & 15;
		final int originZ = fluidPos.getZ() & 15;

		final FluidEdges edges = state.fluidEdges();
		edges.quad(
			originX + quad.getX(0), originY + quad.getY(0), originZ + quad.getZ(0), quad.getTexU(0), quad.getTexV(0),
			originX + quad.getX(1), originY + quad.getY(1), originZ + quad.getZ(1), quad.getTexU(1), quad.getTexV(1),
			originX + quad.getX(2), originY + quad.getY(2), originZ + quad.getZ(2), quad.getTexU(2), quad.getTexV(2),
			originX + quad.getX(3), originY + quad.getY(3), originZ + quad.getZ(3), quad.getTexU(3), quad.getTexV(3),
			glowtone$swapRedAndBlue(ColorARGB.mulRGB(this.quadColors[0], this.brightness[0])),
			this.quadLightData.lm[0]
		);
		if (!edges.locate(originX, originY, originZ)) return;

		final EdgeNeighbours neighbours = state.edgeNeighbours();
		neighbours.gather(level, fluidPos);
		edges.emit(
			state,
			builder.asFallbackVertexConsumer(material, collector),
			neighbours,
			originX, originY, originZ
		);
	}

	@Unique
	private static int glowtone$swapRedAndBlue(int packed) {
		return (packed & 0xFF00FF00) | ((packed >> 16) & 0xFF) | ((packed & 0xFF) << 16);
	}
}
