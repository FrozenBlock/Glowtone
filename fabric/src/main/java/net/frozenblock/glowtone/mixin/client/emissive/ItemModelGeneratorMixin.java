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

package net.frozenblock.glowtone.mixin.client.emissive;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import java.util.ArrayList;
import java.util.List;
import net.frozenblock.glowtone.GlowtoneConstants;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.cuboid.ItemModelGenerator;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@ClientOnly
@Mixin(value = ItemModelGenerator.class, priority = 1500)
public class ItemModelGeneratorMixin {
	@Unique
	private static final ThreadLocal<TextureAtlasSprite> glowtone$baseSprite = new ThreadLocal<>();
	@Unique
	private static final ThreadLocal<List<BakedQuad>> glowtone$baseRim = new ThreadLocal<>();
	@Unique
	private static final ThreadLocal<List<Direction>> glowtone$baseRimFacing = new ThreadLocal<>();

	@WrapMethod(method = "bakeSideFaces")
	private static void glowtone$mirrorBaseRim(
		QuadCollection.Builder builder, ModelBaker.Interner interner, ModelState modelState, BakedQuad.MaterialInfo materialInfo,
		Operation<Void> original
	) {
		final TextureAtlasSprite sprite = materialInfo.sprite();
		if (!sprite.contents().name().getPath().endsWith(GlowtoneConstants.EMISSIVE_SUFFIX)) {
			glowtone$captureBaseRim(builder, interner, modelState, materialInfo, original, sprite);
			return;
		}

		final List<BakedQuad> rim = glowtone$baseRim.get();
		final List<Direction> facing = glowtone$baseRimFacing.get();
		final TextureAtlasSprite base = glowtone$baseSprite.get();
		if (rim == null || facing == null || base == null) {
			original.call(builder, interner, modelState, materialInfo);
			return;
		}

		for (int index = 0; index < rim.size(); index++) {
			final BakedQuad quad = glowtone$retexture(rim.get(index), base, sprite, materialInfo);
			final Direction culled = facing.get(index);
			if (culled == null) {
				builder.addUnculledFace(quad);
			} else {
				builder.addCulledFace(culled, quad);
			}
		}
	}

	@Unique
	private static void glowtone$captureBaseRim(
		QuadCollection.Builder builder, ModelBaker.Interner interner, ModelState modelState, BakedQuad.MaterialInfo materialInfo,
		Operation<Void> original, TextureAtlasSprite sprite
	) {
		final QuadCollection.Builder captured = new QuadCollection.Builder();
		original.call(captured, interner, modelState, materialInfo);

		final QuadCollection built = captured.build();
		final List<BakedQuad> rim = new ArrayList<>();
		final List<Direction> facing = new ArrayList<>();

		for (BakedQuad quad : built.getQuads(null)) {
			builder.addUnculledFace(quad);
			rim.add(quad);
			facing.add(null);
		}
		for (Direction direction : Direction.values()) {
			for (BakedQuad quad : built.getQuads(direction)) {
				builder.addCulledFace(direction, quad);
				rim.add(quad);
				facing.add(direction);
			}
		}

		glowtone$baseSprite.set(sprite);
		glowtone$baseRim.set(rim);
		glowtone$baseRimFacing.set(facing);
	}

	@Unique
	private static BakedQuad glowtone$retexture(
		BakedQuad quad, TextureAtlasSprite base, TextureAtlasSprite overlay, BakedQuad.MaterialInfo materialInfo
	) {
		return new BakedQuad(
			quad.position0(), quad.position1(), quad.position2(), quad.position3(),
			glowtone$remap(quad.packedUV0(), base, overlay),
			glowtone$remap(quad.packedUV1(), base, overlay),
			glowtone$remap(quad.packedUV2(), base, overlay),
			glowtone$remap(quad.packedUV3(), base, overlay),
			quad.direction(),
			materialInfo
		);
	}

	@Unique
	private static long glowtone$remap(long packedUV, TextureAtlasSprite base, TextureAtlasSprite overlay) {
		return UVPair.pack(
			glowtone$remapAxis(UVPair.unpackU(packedUV), base.getU0(), base.getU1(), overlay.getU0(), overlay.getU1()),
			glowtone$remapAxis(UVPair.unpackV(packedUV), base.getV0(), base.getV1(), overlay.getV0(), overlay.getV1())
		);
	}

	@Unique
	private static float glowtone$remapAxis(float value, float fromMin, float fromMax, float toMin, float toMax) {
		final float span = fromMax - fromMin;
		if (span == 0F) return toMin;

		return toMin + (value - fromMin) / span * (toMax - toMin);
	}
}
