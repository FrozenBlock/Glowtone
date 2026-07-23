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

package net.frozenblock.glowtone.neoforge.render;

import java.util.ArrayList;
import java.util.List;
import net.frozenblock.glowtone.GlowtoneConstants;
import net.frozenblock.glowtone.emissive.EmissiveResolver;
import net.frozenblock.glowtone.emissive.QuadUtil;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.IQuadTransformer;
import net.neoforged.neoforge.client.model.QuadTransformers;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.common.util.TriState;
import org.jetbrains.annotations.Nullable;

public class NeoForgeEmissiveModel extends BakedModelWrapper<BakedModel> {
	private static final IQuadTransformer MAX_EMISSIVITY = QuadTransformers.settingMaxEmissivity();
	private static final RenderType OVERLAY_RENDER_TYPE = RenderType.cutoutMipped();

	@Nullable
	private Boolean glowtone$allFullbright;
	@Nullable
	private Boolean glowtone$hasOverlays;

	public NeoForgeEmissiveModel(BakedModel originalModel) {
		super(originalModel);
	}

	@Override
	public BakedModel applyTransform(net.minecraft.world.item.ItemDisplayContext cameraTransformType, com.mojang.blaze3d.vertex.PoseStack poseStack, boolean applyLeftHandTransform) {
		super.applyTransform(cameraTransformType, poseStack, applyLeftHandTransform);
		return this;
	}

	@Override
	public boolean useAmbientOcclusion() {
		return glowtone$disableAmbientOcclusion() ? false : super.useAmbientOcclusion();
	}

	@Override
	public TriState useAmbientOcclusion(BlockState state, ModelData data, RenderType renderType) {
		return glowtone$disableAmbientOcclusion() ? TriState.FALSE : super.useAmbientOcclusion(state, data, renderType);
	}

	private boolean glowtone$disableAmbientOcclusion() {
		if (!GlowtoneConstants.GLOWTONE_SHADING || !GlowtoneConstants.GLOWTONE_EMISSIVES) return false;
		Boolean cached = this.glowtone$allFullbright;
		if (cached == null) {
			cached = glowtone$computeAllFullbright();
			this.glowtone$allFullbright = cached;
		}
		return cached;
	}

	private boolean glowtone$computeAllFullbright() {
		try {
			final RandomSource random = RandomSource.create();
			final List<BakedQuad> quads = new ArrayList<>(super.getQuads(null, null, random));
			for (Direction direction : Direction.values()) {
				quads.addAll(super.getQuads(null, direction, random));
			}
			if (quads.isEmpty()) return false;
			for (BakedQuad quad : quads) {
				if (EmissiveResolver.lightEmissionFor(quad.getSprite()) != 15) return false;
			}
			return true;
		} catch (Exception exception) {
			return false;
		}
	}

	@Override
	public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
		final ChunkRenderTypeSet base = super.getRenderTypes(state, rand, data);
		if (!GlowtoneConstants.GLOWTONE_EMISSIVES || !glowtone$hasOverlays()) return base;

		if (!base.contains(RenderType.solid())) return base;
		return ChunkRenderTypeSet.union(base, ChunkRenderTypeSet.of(OVERLAY_RENDER_TYPE));
	}

	@Override
	public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
		return glowtone$augment(super.getQuads(state, side, rand), glowtone$redstonePowered(state));
	}

	@Override
	public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData extraData, @Nullable RenderType renderType) {
		final List<BakedQuad> quads = super.getQuads(state, side, rand, extraData, renderType);
		if (!GlowtoneConstants.GLOWTONE_EMISSIVES) return quads;

		final boolean forceEmissive = glowtone$redstonePowered(state);
		final boolean hasOverlays = glowtone$hasOverlays();

		final boolean addedOverlayPass = hasOverlays
			&& renderType == OVERLAY_RENDER_TYPE
			&& !super.getRenderTypes(state, rand, extraData).contains(OVERLAY_RENDER_TYPE);
		if (addedOverlayPass) {
			final List<BakedQuad> overlays = new ArrayList<>();
			glowtone$appendOverlays(quads, overlays);
			return overlays;
		}

		final boolean overlaysHere = hasOverlays && renderType != RenderType.solid();
		if (!forceEmissive && !overlaysHere && !glowtone$hasFullbrightBase(quads)) return quads;

		final List<BakedQuad> result = new ArrayList<>(quads.size() + 4);
		for (BakedQuad quad : quads) {
			result.add(glowtone$asBase(quad, forceEmissive));
		}
		if (overlaysHere) {
			glowtone$appendOverlays(quads, result);
		}
		return result;
	}

	private static void glowtone$appendOverlays(List<BakedQuad> source, List<BakedQuad> out) {
		for (BakedQuad quad : source) {
			final TextureAtlasSprite sprite = quad.getSprite();
			final TextureAtlasSprite overlay = EmissiveResolver.overlayFor(sprite);
			if (overlay != null) {
				final boolean shade = EmissiveResolver.shadeFor(overlay, quad.isShade());
				out.add(MAX_EMISSIVITY.process(QuadUtil.retexture(quad, sprite, overlay, shade)));
			}
		}
	}

	private static BakedQuad glowtone$asBase(BakedQuad quad, boolean forceEmissive) {
		final TextureAtlasSprite sprite = quad.getSprite();
		if (forceEmissive || EmissiveResolver.lightEmissionFor(sprite) == 15) {
			final boolean shade = EmissiveResolver.shadeFor(sprite, quad.isShade());
			return MAX_EMISSIVITY.process(QuadUtil.withShade(quad, shade));
		}
		return quad;
	}

	private static boolean glowtone$hasFullbrightBase(List<BakedQuad> quads) {
		for (BakedQuad quad : quads) {
			if (EmissiveResolver.lightEmissionFor(quad.getSprite()) == 15) return true;
		}
		return false;
	}

	private boolean glowtone$hasOverlays() {
		Boolean cached = this.glowtone$hasOverlays;
		if (cached == null) {
			cached = glowtone$computeHasOverlays();
			this.glowtone$hasOverlays = cached;
		}
		return cached;
	}

	private boolean glowtone$computeHasOverlays() {
		try {
			final RandomSource random = RandomSource.create();
			final List<BakedQuad> quads = new ArrayList<>(super.getQuads(null, null, random));
			for (Direction direction : Direction.values()) {
				quads.addAll(super.getQuads(null, direction, random));
			}
			for (BakedQuad quad : quads) {
				if (EmissiveResolver.overlayFor(quad.getSprite()) != null) return true;
			}
			return false;
		} catch (Exception exception) {
			return false;
		}
	}

	@Override
	public List<BakedModel> getRenderPasses(ItemStack itemStack, boolean fabulous) {
		if (!GlowtoneConstants.GLOWTONE_EMISSIVES) {
			return super.getRenderPasses(itemStack, fabulous);
		}
		final List<BakedModel> passes = new ArrayList<>(this.originalModel.getRenderPasses(itemStack, fabulous));
		passes.add(new GlowtoneEmissiveItemPass(this.originalModel));
		return passes;
	}

	private static boolean glowtone$redstonePowered(@Nullable BlockState state) {
		return state != null
			&& state.getBlock() instanceof RedStoneWireBlock
			&& state.hasProperty(BlockStateProperties.POWER)
			&& state.getValue(BlockStateProperties.POWER) > 0;
	}

	private static List<BakedQuad> glowtone$augment(List<BakedQuad> quads, boolean forceEmissive) {
		if (!GlowtoneConstants.GLOWTONE_EMISSIVES || quads.isEmpty()) return quads;

		if (!forceEmissive && !glowtone$hasEmissiveContent(quads)) return quads;

		final List<BakedQuad> result = new ArrayList<>(quads.size() * 2);
		for (BakedQuad quad : quads) {
			final TextureAtlasSprite sprite = quad.getSprite();

			if (forceEmissive || EmissiveResolver.lightEmissionFor(sprite) == 15) {
				final boolean shade = EmissiveResolver.shadeFor(sprite, quad.isShade());
				result.add(MAX_EMISSIVITY.process(QuadUtil.withShade(quad, shade)));
			} else {
				result.add(quad);
			}

			final TextureAtlasSprite overlay = EmissiveResolver.overlayFor(sprite);
			if (overlay != null) {
				final boolean shade = EmissiveResolver.shadeFor(overlay, quad.isShade());
				result.add(MAX_EMISSIVITY.process(QuadUtil.retexture(quad, sprite, overlay, shade)));
			}
		}
		return result;
	}

	private static boolean glowtone$hasEmissiveContent(List<BakedQuad> quads) {
		for (BakedQuad quad : quads) {
			final TextureAtlasSprite sprite = quad.getSprite();
			if (EmissiveResolver.lightEmissionFor(sprite) == 15 || EmissiveResolver.overlayFor(sprite) != null) {
				return true;
			}
		}
		return false;
	}
}
