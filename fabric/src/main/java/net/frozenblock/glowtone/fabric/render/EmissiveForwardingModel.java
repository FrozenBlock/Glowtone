/*
 * Copyright 2025-2026 FrozenBlock
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

package net.frozenblock.glowtone.fabric.render;

import java.util.function.Supplier;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.ForwardingBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.frozenblock.glowtone.GlowtoneConstants;
import net.frozenblock.glowtone.emissive.EmissiveResolver;
import net.frozenblock.glowtone.emissive.QuadUtil;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.Nullable;

public class EmissiveForwardingModel extends ForwardingBakedModel {
	private static final Direction[] CULL_FACES = new Direction[] {
		null, Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST
	};

	public EmissiveForwardingModel(BakedModel wrapped) {
		this.wrapped = wrapped;
	}

	@Override
	public boolean isVanillaAdapter() {
		return !GlowtoneConstants.GLOWTONE_EMISSIVES;
	}

	@Override
	public void emitBlockQuads(BlockAndTintGetter blockView, BlockState state, BlockPos pos, Supplier<RandomSource> randomSupplier, RenderContext context) {
		if (!GlowtoneConstants.GLOWTONE_EMISSIVES) {
			super.emitBlockQuads(blockView, state, pos, randomSupplier, context);
			return;
		}

		final boolean forceEmissive = state != null
			&& state.getBlock() instanceof RedStoneWireBlock
			&& state.hasProperty(BlockStateProperties.POWER)
			&& state.getValue(BlockStateProperties.POWER) > 0;

		final QuadEmitter emitter = context.getEmitter();
		for (Direction cullFace : CULL_FACES) {
			for (BakedQuad quad : this.wrapped.getQuads(state, cullFace, randomSupplier.get())) {
				glowtone$emit(emitter, quad, cullFace, forceEmissive);
			}
		}
	}

	@Override
	public void emitItemQuads(ItemStack stack, Supplier<RandomSource> randomSupplier, RenderContext context) {
		if (!GlowtoneConstants.GLOWTONE_EMISSIVES) {
			super.emitItemQuads(stack, randomSupplier, context);
			return;
		}
		final QuadEmitter emitter = context.getEmitter();
		for (Direction cullFace : CULL_FACES) {
			for (BakedQuad quad : this.wrapped.getQuads(null, cullFace, randomSupplier.get())) {
				glowtone$emit(emitter, quad, cullFace, false);
			}
		}
	}

	private static void glowtone$emit(QuadEmitter emitter, BakedQuad quad, @Nullable Direction cullFace, boolean forceEmissive) {
		final TextureAtlasSprite sprite = quad.getSprite();

		final boolean baseEmissive = forceEmissive || EmissiveResolver.lightEmissionFor(sprite) == 15;
		final RenderMaterial baseMaterial = baseEmissive
			? glowtone$emissiveMaterial(EmissiveResolver.shadeFor(sprite, quad.isShade()))
			: GlowtoneFabricMaterials.STANDARD;
		emitter.fromVanilla(quad, baseMaterial, cullFace);
		emitter.emit();

		final TextureAtlasSprite overlay = EmissiveResolver.overlayFor(sprite);
		if (overlay != null) {
			final boolean shade = EmissiveResolver.shadeFor(overlay, quad.isShade());
			final BakedQuad overlayQuad = QuadUtil.retexture(quad, sprite, overlay, shade);
			emitter.fromVanilla(overlayQuad, glowtone$emissiveMaterial(shade), cullFace);
			emitter.emit();
		}
	}

	private static RenderMaterial glowtone$emissiveMaterial(boolean shade) {
		return shade ? GlowtoneFabricMaterials.EMISSIVE_SHADED : GlowtoneFabricMaterials.EMISSIVE_UNSHADED;
	}
}
