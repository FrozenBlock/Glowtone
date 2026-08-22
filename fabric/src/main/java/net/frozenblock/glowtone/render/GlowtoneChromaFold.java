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

package net.frozenblock.glowtone.render;

import net.frozenblock.glowtone.light.GlowtoneRegionFlood;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.LightmapRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;

public final class GlowtoneChromaFold {
	public static final int NO_TINT = 0;

	private static final float PACKED_LIGHT_SCALE = 240.0f;
	private static final String EMISSIVE_DEFINE = "EMISSIVE";
	private static final float LUMA_RED = 0.2126f;
	private static final float LUMA_GREEN = 0.7152f;
	private static final float LUMA_BLUE = 0.0722f;

	private static int[] tintStack = new int[16];
	private static int tintDepth;

	private static int itemTint = NO_TINT;

	private GlowtoneChromaFold() {
		throw new UnsupportedOperationException("GlowtoneChromaFold is a static holder.");
	}

	public static int resolveEntity(double x, double feetY, double z, float eyeHeight, int lightCoords) {
		var engine = GlowtoneColorProbe.get();
		int blockX = Mth.floor(x);
		int blockZ = Mth.floor(z);
		int eyeY = Mth.floor(feetY + eyeHeight);

		int sky = skyTint(engine, blockX, eyeY, blockZ, lightCoords);

		float weight = blockLightShare(lightCoords);
		if (weight <= 0.0f) {
			return sky;
		}

		long samples = smoothLighting()
				? sampleTrilinear(engine, x, feetY + eyeHeight, z)
				: sampleNearest(engine, blockX, eyeY, blockZ);
		if (GlowtoneChromaBlend.isEmpty(samples)) {
			int feetBlockY = Mth.floor(feetY);
			if (feetBlockY != eyeY) {
				samples = GlowtoneChromaBlend.add(samples, engine.getPackedLevels(blockX, feetBlockY, blockZ));
			}
		}
		final int chromaArgb = GlowtoneChromaBlend.toEntityArgb(samples);
		final int folded = fold(chromaArgb, weight);
		final int resolved = combine(folded, sky);
		return resolved;
	}

	public static int resolveBlockEntity(BlockPos pos, int lightCoords) {
		var engine = GlowtoneColorProbe.get();
		int blockX = pos.getX();
		int blockY = pos.getY();
		int blockZ = pos.getZ();

		int sky = skyTint(engine, blockX, blockY, blockZ, lightCoords);

		float weight = blockLightShare(lightCoords);
		if (weight <= 0.0f) {
			return sky;
		}

		long samples = GlowtoneChromaBlend.add(GlowtoneChromaBlend.EMPTY, engine.getPackedLevels(blockX, blockY, blockZ));
		if (GlowtoneChromaBlend.isEmpty(samples)) {
			samples = addNeighbours(engine, samples, blockX, blockY, blockZ);
		}
		return combine(fold(GlowtoneChromaBlend.toEntityArgb(samples), weight), sky);
	}

	public static int resolveParticle(double x, double y, double z, int lightCoords) {
		var engine = GlowtoneColorProbe.get();
		int blockX = Mth.floor(x);
		int blockY = Mth.floor(y);
		int blockZ = Mth.floor(z);

		int sky = skyTint(engine, blockX, blockY, blockZ, lightCoords);

		float weight = blockLightShare(lightCoords);
		if (weight <= 0.0f) {
			return sky;
		}

		int levels = engine.getPackedLevels(blockX, blockY, blockZ);
		return combine(
				fold(GlowtoneChromaBlend.toEntityArgb(GlowtoneChromaBlend.add(GlowtoneChromaBlend.EMPTY, levels)), weight), sky
		);
	}

	public static int resolveHand(double probeX, double probeY, double probeZ, int lightCoords) {
		var engine = GlowtoneColorProbe.get();
		int blockX = Mth.floor(probeX);
		int blockY = Mth.floor(probeY);
		int blockZ = Mth.floor(probeZ);

		int sky = skyTint(engine, blockX, blockY, blockZ, lightCoords);

		float weight = blockLightShare(lightCoords);
		if (weight <= 0.0f) {
			return sky;
		}

		long samples = smoothLighting()
				? sampleTrilinear(engine, probeX, probeY, probeZ)
				: sampleNearest(engine, blockX, blockY, blockZ);
		return combine(fold(GlowtoneChromaBlend.toEntityArgb(samples), weight), sky);
	}

	private static int skyTint(GlowtoneColorProbe engine, int x, int y, int z, int lightCoords) {
		float weight = skyLightShare(lightCoords);
		if (weight <= 0.0f) {
			return NO_TINT;
		}

		int red = 0;
		int green = 0;
		int blue = 0;
		for (int i = 0; i < SKY_SAMPLES.length; i += 3) {
			int rgb = engine.getSkyRgb(x + SKY_SAMPLES[i], y + SKY_SAMPLES[i + 1], z + SKY_SAMPLES[i + 2]);
			red += (rgb >> 16) & 0xFF;
			green += (rgb >> 8) & 0xFF;
			blue += rgb & 0xFF;
		}

		int count = SKY_SAMPLES.length / 3;
		int averaged = ((red / count) << 16) | ((green / count) << 8) | (blue / count);
		return fold(GlowtoneChromaBlend.skyTintArgb(averaged), weight);
	}

	private static float skyLightShare(int lightCoords) {
		float skyLevel = Math.min(1.0f, LightCoordsUtil.smoothSky(lightCoords) / PACKED_LIGHT_SCALE);
		if (skyLevel <= 0.0f) {
			return 0.0f;
		}

		float blockFactor = 1.0f;
		float skyFactor = 1.0f;
		float ambient = 0.0f;

		LightmapRenderState lightmap = lightmapRenderState();
		if (lightmap != null && lightmap.blockFactor > 0.0f) {
			blockFactor = lightmap.blockFactor;
			skyFactor = Math.max(0.0f, lightmap.skyFactor);
			var ambientColor = lightmap.ambientColor;
			if (ambientColor != null) {
				ambient = LUMA_RED * ambientColor.x() + LUMA_GREEN * ambientColor.y() + LUMA_BLUE * ambientColor.z();
			}
		}

		float blockLevel = Math.min(1.0f, LightCoordsUtil.smoothBlock(lightCoords) / PACKED_LIGHT_SCALE);
		float skyPart = brightness(skyLevel) * skyFactor;
		float total = brightness(blockLevel) * blockFactor + skyPart + ambient;
		return total <= 1.0e-5f ? 0.0f : Math.min(1.0f, skyPart / total);
	}

	private static int combine(int first, int second) {
		if (first == NO_TINT) return second;
		if (second == NO_TINT) return first;
		return ARGB.multiply(first, second);
	}

	private static final int[] SKY_SAMPLES = {
			0, 0, 0,
			-1, 0, 0, 1, 0, 0,
			0, -1, 0, 0, 1, 0,
			0, 0, -1, 0, 0, 1,
	};

	private static boolean smoothLighting() {
		final Minecraft minecraft = Minecraft.getInstance();
		return minecraft != null && minecraft.options.ambientOcclusion().get();
	}

	private static long sampleNearest(GlowtoneColorProbe engine, int blockX, int blockY, int blockZ) {
		return GlowtoneChromaBlend.add(
				GlowtoneChromaBlend.EMPTY,
				engine.getPackedLevels(blockX, blockY, blockZ)
		);
	}

	private static long sampleTrilinear(GlowtoneColorProbe engine, double x, double y, double z) {
		final int step = GlowtoneRegionFlood.ENTITY_CELL_BLOCKS;
		final double centreOffset = step / 2.0;

		final double gridX = (x - centreOffset) / step;
		final double gridY = (y - centreOffset) / step;
		final double gridZ = (z - centreOffset) / step;

		final int cellX = Mth.floor(gridX);
		final int cellY = Mth.floor(gridY);
		final int cellZ = Mth.floor(gridZ);

		final int fracX = (int) ((gridX - cellX) * GlowtoneChromaBlend.WEIGHT_ONE);
		final int fracY = (int) ((gridY - cellY) * GlowtoneChromaBlend.WEIGHT_ONE);
		final int fracZ = (int) ((gridZ - cellZ) * GlowtoneChromaBlend.WEIGHT_ONE);

		long samples = GlowtoneChromaBlend.EMPTY;
		for (int corner = 0; corner < 8; corner++) {
			final int offsetX = corner & 1;
			final int offsetY = (corner >> 1) & 1;
			final int offsetZ = (corner >> 2) & 1;

			final int weightX = offsetX == 0 ? GlowtoneChromaBlend.WEIGHT_ONE - fracX : fracX;
			final int weightY = offsetY == 0 ? GlowtoneChromaBlend.WEIGHT_ONE - fracY : fracY;
			final int weightZ = offsetZ == 0 ? GlowtoneChromaBlend.WEIGHT_ONE - fracZ : fracZ;

			final int weight = weightX * weightY / GlowtoneChromaBlend.WEIGHT_ONE
					* weightZ / GlowtoneChromaBlend.WEIGHT_ONE;
			if (weight <= 0) continue;

			samples = GlowtoneChromaBlend.addWeighted(
					samples,
					engine.getPackedLevels(
							(cellX + offsetX) * step,
							(cellY + offsetY) * step,
							(cellZ + offsetZ) * step
					),
					weight
			);
		}
		return samples;
	}

	private static long addNeighbours(GlowtoneColorProbe engine, long samples, int x, int y, int z) {
		final int step = GlowtoneRegionFlood.ENTITY_CELL_BLOCKS;
		samples = GlowtoneChromaBlend.add(samples, engine.getPackedLevels(x - step, y, z));
		samples = GlowtoneChromaBlend.add(samples, engine.getPackedLevels(x + step, y, z));
		samples = GlowtoneChromaBlend.add(samples, engine.getPackedLevels(x, y, z - step));
		samples = GlowtoneChromaBlend.add(samples, engine.getPackedLevels(x, y, z + step));
		samples = GlowtoneChromaBlend.add(samples, engine.getPackedLevels(x, y + step, z));
		samples = GlowtoneChromaBlend.add(samples, engine.getPackedLevels(x, y - step, z));
		return samples;
	}

	private static int fold(int chromaArgb, float weight) {
		if (chromaArgb == GlowtoneChromaBlend.NEUTRAL_ARGB) {
			return NO_TINT;
		}

		int folded = 0xFF000000
				| (channelTowardWhite((chromaArgb >> 16) & 0xFF, weight) << 16)
				| (channelTowardWhite((chromaArgb >> 8) & 0xFF, weight) << 8)
				| channelTowardWhite(chromaArgb & 0xFF, weight);
		return folded == GlowtoneChromaBlend.NEUTRAL_ARGB ? NO_TINT : folded;
	}

	private static int channelTowardWhite(int chroma, float weight) {
		return Math.clamp(Math.round(255.0f + (chroma - 255.0f) * weight), 0, 255);
	}

	private static float blockLightShare(int lightCoords) {
		float blockLevel = Math.min(1.0f, LightCoordsUtil.smoothBlock(lightCoords) / PACKED_LIGHT_SCALE);
		if (blockLevel <= 0.0f) {
			return 0.0f;
		}

		float blockFactor = 1.0f;
		float skyFactor = 1.0f;
		float ambient = 0.0f;

		LightmapRenderState lightmap = lightmapRenderState();
		if (lightmap != null && lightmap.blockFactor > 0.0f) {
			blockFactor = lightmap.blockFactor;
			skyFactor = Math.max(0.0f, lightmap.skyFactor);
			var ambientColor = lightmap.ambientColor;
			if (ambientColor != null) {
				ambient = LUMA_RED * ambientColor.x() + LUMA_GREEN * ambientColor.y() + LUMA_BLUE * ambientColor.z();
			}
		}

		float skyLevel = Math.min(1.0f, LightCoordsUtil.smoothSky(lightCoords) / PACKED_LIGHT_SCALE);
		float blockPart = brightness(blockLevel) * blockFactor;
		float total = blockPart + brightness(skyLevel) * skyFactor + ambient;
		return total <= 1.0e-5f ? 0.0f : Math.min(1.0f, blockPart / total);
	}

	private static float brightness(float level) {
		return level / (4.0f - 3.0f * level);
	}

	private static @Nullable LightmapRenderState lightmapRenderState() {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft == null || minecraft.gameRenderer == null) {
			return null;
		}
		return minecraft.gameRenderer.gameRenderState().lightmapRenderState;
	}

	public static void pushTint(int tint) {
		if (tintDepth == tintStack.length) {
			tintStack = Arrays.copyOf(tintStack, tintDepth * 2);
		}
		tintStack[tintDepth++] = tint;
	}

	public static void popTint() {
		if (tintDepth > 0) {
			tintDepth--;
		}
	}

	public static void resetScopes() {
		tintDepth = 0;
		itemTint = NO_TINT;
		GlowtoneColorProbe.get().invalidate();
	}

	public static int currentTint() {
		return tintDepth == 0 ? NO_TINT : tintStack[tintDepth - 1];
	}

	public static int tintModelColor(int submittedColor, RenderType renderType) {
		int tint = currentTint();
		if (tint == NO_TINT || !liesUnderLightmap(renderType)) {
			return submittedColor;
		}
		return ARGB.multiply(submittedColor, tint);
	}

	public static void beginItemQuads(int tint, int lightCoords) {
		itemTint = lightCoords == LightCoordsUtil.FULL_BRIGHT ? NO_TINT : tint;
	}

	public static void endItemQuads() {
		itemTint = NO_TINT;
	}

	public static int tintItemColor(int quadColor) {
		return itemTint == NO_TINT ? quadColor : ARGB.multiply(quadColor, itemTint);
	}

	public static int tintParticleColor(int color, int lightCoords, double x, double y, double z) {
		if (lightCoords == LightCoordsUtil.FULL_BRIGHT) {
			return color;
		}
		int tint = resolveParticle(x, y, z, lightCoords);
		return tint == NO_TINT ? color : ARGB.multiply(color, tint);
	}

	private static boolean liesUnderLightmap(RenderType renderType) {
		if (renderType.isOutline()) {
			return false;
		}
		RenderPipeline pipeline = renderType.pipeline();
		if (pipeline.getShaderDefines().flags().contains(EMISSIVE_DEFINE)) {
			return false;
		}
		VertexFormat[] bindings = pipeline.getVertexFormatBindings();
		return bindings.length > 0 && bindings[0].contains(DefaultVertexFormat.UV2_SEMANTIC_NAME);
	}
}
