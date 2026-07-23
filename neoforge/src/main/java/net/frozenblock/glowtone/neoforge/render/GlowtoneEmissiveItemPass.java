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
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import org.jetbrains.annotations.Nullable;

public class GlowtoneEmissiveItemPass extends BakedModelWrapper<BakedModel> {
	public GlowtoneEmissiveItemPass(BakedModel originalModel) {
		super(originalModel);
	}

	@Override
	public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
		final List<BakedQuad> emissive = new ArrayList<>();
		for (BakedQuad quad : super.getQuads(state, side, rand)) {
			final TextureAtlasSprite sprite = quad.getSprite();
			if (EmissiveResolver.lightEmissionFor(sprite) == 15) {
				emissive.add(quad);
			}
			final TextureAtlasSprite overlay = EmissiveResolver.overlayFor(sprite);
			if (overlay != null) {
				emissive.add(QuadUtil.retexture(quad, sprite, overlay, quad.isShade()));
			}
		}
		return emissive;
	}

	@Override
	public List<RenderType> getRenderTypes(ItemStack itemStack, boolean fabulous) {
		final RenderType renderType = GlowtoneConstants.GLOWTONE_SHADING
			? GlowtoneNeoForgeItemRenderTypes.ITEM_EMISSIVE
			: RenderType.entityTranslucentEmissive(TextureAtlas.LOCATION_BLOCKS);
		return List.of(renderType);
	}
}
