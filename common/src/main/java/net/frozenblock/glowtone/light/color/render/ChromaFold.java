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

package net.frozenblock.glowtone.light.color.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.Arrays;
import net.frozenblock.glowtone.light.GlowtoneRegionFlood;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.LightmapRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.level.lighting.LightEngine;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

@ClientOnly
public final class ChromaFold {
	public static final int NO_TINT = 0;
	private static final int WHITE_RGB = 0xFFFFFF;
	private static final float PACKED_LIGHT_SCALE = LightCoordsUtil.MAX_SMOOTH_LIGHT_LEVEL;
	private static final String EMISSIVE_DEFINITION = "EMISSIVE";
	private static final float LUMA_RED = 0.2126F;
	private static final float LUMA_GREEN = 0.7152F;
	private static final float LUMA_BLUE = 0.0722F;

	private static int[] blockTintStack = new int[16];
	private static int[] skyTintStack = new int[16];
	private static int tintDepth;
	private static int blockTint = NO_TINT;
	private static int movingBlockTint = NO_TINT;
	private static int modelTint = NO_TINT;
	private static int modelSkyTint = NO_TINT;

	public static int resolveEntityTint(double x, double y, double z, float eyeHeight, int lightCoords, boolean sky) {
		if (!ChromaBlender.isEnabled()) return ChromaBlender.NEUTRAL_ARGB;

		final ColorProbe probe = ColorProbe.get();
		final int blockX = Mth.floor(x);
		final int eyeY = Mth.floor(y + eyeHeight);
		final int blockZ = Mth.floor(z);

		if (sky) {
			if (skyLightShare(lightCoords) <= 0F) return NO_TINT;
			return skyTintHue(probe, x, y + eyeHeight, z);
		}

		final float weight = blockLightShare(lightCoords);
		if (weight <= 0F) return ChromaBlender.NEUTRAL_ARGB;

		long samples = smoothLighting()
			? sampleTrilinear(probe, x, y + eyeHeight, z)
			: sampleNearest(probe, blockX, eyeY, blockZ);
		if (ChromaBlender.isEmpty(samples)) {
			final int feetBlockY = Mth.floor(y);
			if (feetBlockY != eyeY) samples = ChromaBlender.add(samples, probe.getPackedLevels(blockX, feetBlockY, blockZ));
		}

		return ChromaBlender.toEntityArgb(samples);
	}

	// TODO: sky
	public static int resolveMovingBlockTint(BlockPos pos, int lightCoords) {
		if (!ChromaBlender.isEnabled()) return NO_TINT;

		final ColorProbe probe = ColorProbe.get();
		final int blockX = pos.getX();
		final int blockY = pos.getY();
		final int blockZ = pos.getZ();

		final int sky = skyTint(probe, blockX + 0.5D, blockY + 0.5D, blockZ + 0.5D, lightCoords);
		final float weight = blockLightShare(lightCoords);
		if (weight <= 0F) return sky;

		long samples = ChromaBlender.add(ChromaBlender.EMPTY, probe.getPackedLevels(blockX, blockY, blockZ));
		if (ChromaBlender.isEmpty(samples)) samples = addNeighbours(probe, samples, blockX, blockY, blockZ);
		return combine(fold(ChromaBlender.toEntityArgb(samples), weight), sky);
	}

	// TODO: sky
	public static int resolveBlockEntityBlockTint(BlockPos pos, int lightCoords) {
		if (!ChromaBlender.isEnabled()) return ChromaBlender.NEUTRAL_ARGB;

		final ColorProbe probe = ColorProbe.get();
		final int blockX = pos.getX();
		final int blockY = pos.getY();
		final int blockZ = pos.getZ();

		final float weight = blockLightShare(lightCoords);
		if (weight <= 0F) return ChromaBlender.NEUTRAL_ARGB;

		long samples = ChromaBlender.add(ChromaBlender.EMPTY, probe.getPackedLevels(blockX, blockY, blockZ));
		if (ChromaBlender.isEmpty(samples)) samples = addNeighbours(probe, samples, blockX, blockY, blockZ);
		return ChromaBlender.toEntityArgb(samples);
	}

	public static int resolveParticle(double x, double y, double z, int lightCoords) {
		if (!ChromaBlender.isEnabled()) return NO_TINT;

		final ColorProbe probe = ColorProbe.get();
		final int blockX = Mth.floor(x);
		final int blockY = Mth.floor(y);
		final int blockZ = Mth.floor(z);

		final int sky = skyTint(probe, x, y, z, lightCoords);
		final float weight = blockLightShare(lightCoords);
		if (weight <= 0F) return sky;

		final int levels = probe.getPackedLevels(blockX, blockY, blockZ);
		return combine(fold(ChromaBlender.toEntityArgb(ChromaBlender.add(ChromaBlender.EMPTY, levels)), weight), sky);
	}

	public static int resolveHandTint(double x, double y, double z, int lightCoords, boolean sky) {
		if (!ChromaBlender.isEnabled()) return NO_TINT;

		final ColorProbe probe = ColorProbe.get();
		final int blockX = Mth.floor(x);
		final int blockY = Mth.floor(y);
		final int blockZ = Mth.floor(z);

		if (sky) {
			if (skyLightShare(lightCoords) <= 0F) return NO_TINT;
			return skyTintHue(probe, x, y, z);
		}

		final int skyTint = skyTint(probe, x, y, z, lightCoords);
		final float weight = blockLightShare(lightCoords);
		if (weight <= 0F) return skyTint;

		final long samples = smoothLighting()
			? sampleTrilinear(probe, x, y, z)
			: sampleNearest(probe, blockX, blockY, blockZ);
		return combine(fold(ChromaBlender.toEntityArgb(samples), weight), skyTint);
	}

	private static int skyTintHue(ColorProbe probe, double x, double y, double z) {
		final int step = GlowtoneRegionFlood.ENTITY_CELL_BLOCKS;
		final double centreOffset = step / 2D;

		final double gridX = (x - centreOffset) / step;
		final double gridY = (y - centreOffset) / step;
		final double gridZ = (z - centreOffset) / step;

		final int cellX = Mth.floor(gridX);
		final int cellY = Mth.floor(gridY);
		final int cellZ = Mth.floor(gridZ);

		final int fracX = (int) ((gridX - cellX) * ChromaBlender.WEIGHT_ONE);
		final int fracY = (int) ((gridY - cellY) * ChromaBlender.WEIGHT_ONE);
		final int fracZ = (int) ((gridZ - cellZ) * ChromaBlender.WEIGHT_ONE);

		int red = 0;
		int green = 0;
		int blue = 0;
		int total = 0;
		boolean any = false;
		for (int corner = 0; corner < 8; corner++) {
			final int offsetX = corner & 1;
			final int offsetY = (corner >> 1) & 1;
			final int offsetZ = (corner >> 2) & 1;

			final int weightX = offsetX == 0 ? ChromaBlender.WEIGHT_ONE - fracX : fracX;
			final int weightY = offsetY == 0 ? ChromaBlender.WEIGHT_ONE - fracY : fracY;
			final int weightZ = offsetZ == 0 ? ChromaBlender.WEIGHT_ONE - fracZ : fracZ;

			final int weight = weightX * weightY / ChromaBlender.WEIGHT_ONE
				* weightZ / ChromaBlender.WEIGHT_ONE;
			if (weight <= 0) continue;

			final int sampleX = (cellX + offsetX) * step;
			final int sampleY = (cellY + offsetY) * step;
			final int sampleZ = (cellZ + offsetZ) * step;
			int rgb = WHITE_RGB;
			if (probe.hasSkyHues(sampleX, sampleY, sampleZ)) {
				any = true;
				rgb = probe.getSkyRgb(sampleX, sampleY, sampleZ);
			}

			red += ((rgb >> 16) & 0xFF) * weight;
			green += ((rgb >> 8) & 0xFF) * weight;
			blue += (rgb & 0xFF) * weight;
			total += weight;
		}

		if (!any || total == 0) return ChromaBlender.skyTintArgb(WHITE_RGB);
		return ChromaBlender.skyTintArgb(((red / total) << 16) | ((green / total) << 8) | (blue / total));
	}

	private static int skyTint(ColorProbe probe, double x, double y, double z, int lightCoords) {
		final float weight = skyLightShare(lightCoords);
		if (weight <= 0F) return NO_TINT;

		return fold(skyTintHue(probe, x, y, z), weight);
	}

	private static float skyLightShare(int lightCoords) {
		float skyLevel = Math.min(1F, LightCoordsUtil.smoothSky(lightCoords) / PACKED_LIGHT_SCALE);
		if (skyLevel <= 0F) return 0F;

		float blockFactor = 1F;
		float skyFactor = 1F;
		float ambient = 0F;

		final LightmapRenderState lightmap = lightmapRenderState();
		if (lightmap != null && lightmap.blockFactor > 0F) {
			blockFactor = lightmap.blockFactor;
			skyFactor = Math.max(0F, lightmap.skyFactor);
			final Vector3fc ambientColor = lightmap.ambientColor;
			if (ambientColor != null) ambient = LUMA_RED * ambientColor.x() + LUMA_GREEN * ambientColor.y() + LUMA_BLUE * ambientColor.z();
		}

		final float blockLevel = Math.min(1F, LightCoordsUtil.smoothBlock(lightCoords) / PACKED_LIGHT_SCALE);
		final float skyPart = brightness(skyLevel) * skyFactor;
		final float total = brightness(blockLevel) * blockFactor + skyPart + ambient;
		return total <= 1.0e-5f ? 0F : Math.min(1F, skyPart / total);
	}

	private static int combine(int first, int second) {
		if (first == NO_TINT) return second;
		if (second == NO_TINT) return first;
		return ARGB.multiply(first, second);
	}

	private static boolean smoothLighting() {
		final Minecraft minecraft = Minecraft.getInstance();
		return minecraft != null && minecraft.options.ambientOcclusion().get();
	}

	private static long sampleNearest(ColorProbe probe, int blockX, int blockY, int blockZ) {
		return ChromaBlender.add(
			ChromaBlender.EMPTY,
			probe.getPackedLevels(blockX, blockY, blockZ)
		);
	}

	private static long sampleTrilinear(ColorProbe probe, double x, double y, double z) {
		final int step = GlowtoneRegionFlood.ENTITY_CELL_BLOCKS;
		final double centreOffset = step / 2D;

		final double gridX = (x - centreOffset) / step;
		final double gridY = (y - centreOffset) / step;
		final double gridZ = (z - centreOffset) / step;

		final int cellX = Mth.floor(gridX);
		final int cellY = Mth.floor(gridY);
		final int cellZ = Mth.floor(gridZ);

		final int fracX = (int) ((gridX - cellX) * ChromaBlender.WEIGHT_ONE);
		final int fracY = (int) ((gridY - cellY) * ChromaBlender.WEIGHT_ONE);
		final int fracZ = (int) ((gridZ - cellZ) * ChromaBlender.WEIGHT_ONE);

		long samples = ChromaBlender.EMPTY;
		for (int corner = 0; corner < 8; corner++) {
			final int offsetX = corner & 1;
			final int offsetY = (corner >> 1) & 1;
			final int offsetZ = (corner >> 2) & 1;

			final int weightX = offsetX == 0 ? ChromaBlender.WEIGHT_ONE - fracX : fracX;
			final int weightY = offsetY == 0 ? ChromaBlender.WEIGHT_ONE - fracY : fracY;
			final int weightZ = offsetZ == 0 ? ChromaBlender.WEIGHT_ONE - fracZ : fracZ;

			final int weight = weightX * weightY / ChromaBlender.WEIGHT_ONE
				* weightZ / ChromaBlender.WEIGHT_ONE;
			if (weight <= 0) continue;

			samples = ChromaBlender.addWeighted(
				samples,
				probe.getPackedLevels(
					(cellX + offsetX) * step,
					(cellY + offsetY) * step,
					(cellZ + offsetZ) * step
				),
				weight
			);
		}
		return samples;
	}

	private static long addNeighbours(ColorProbe probe, long samples, int x, int y, int z) {
		final int step = GlowtoneRegionFlood.ENTITY_CELL_BLOCKS;
		samples = ChromaBlender.add(samples, probe.getPackedLevels(x - step, y, z));
		samples = ChromaBlender.add(samples, probe.getPackedLevels(x + step, y, z));
		samples = ChromaBlender.add(samples, probe.getPackedLevels(x, y, z - step));
		samples = ChromaBlender.add(samples, probe.getPackedLevels(x, y, z + step));
		samples = ChromaBlender.add(samples, probe.getPackedLevels(x, y + step, z));
		samples = ChromaBlender.add(samples, probe.getPackedLevels(x, y - step, z));
		return samples;
	}

	private static int fold(int chromaArgb, float weight) {
		if (chromaArgb == ChromaBlender.NEUTRAL_ARGB) return NO_TINT;

		final int folded = 0xFF000000
			| (channelTowardWhite((chromaArgb >> 16) & 0xFF, weight) << 16)
			| (channelTowardWhite((chromaArgb >> 8) & 0xFF, weight) << 8)
			| channelTowardWhite(chromaArgb & 0xFF, weight);
		return folded == ChromaBlender.NEUTRAL_ARGB ? NO_TINT : folded;
	}

	private static int channelTowardWhite(int chroma, float weight) {
		return Math.clamp(Math.round(255F + (chroma - 255F) * weight), 0, 255);
	}

	private static float blockLightShare(int lightCoords) {
		final float blockLevel = Math.min(1F, LightCoordsUtil.smoothBlock(lightCoords) / PACKED_LIGHT_SCALE);
		if (blockLevel <= 0F) return 0F;

		float blockFactor = 1F;
		float skyFactor = 1F;
		float ambient = 0F;

		final LightmapRenderState lightmap = lightmapRenderState();
		if (lightmap != null && lightmap.blockFactor > 0F) {
			blockFactor = lightmap.blockFactor;
			skyFactor = Math.max(0F, lightmap.skyFactor);
			final Vector3fc ambientColor = lightmap.ambientColor;
			if (ambientColor != null) ambient = LUMA_RED * ambientColor.x() + LUMA_GREEN * ambientColor.y() + LUMA_BLUE * ambientColor.z();
		}

		final float skyLevel = Math.min(1F, LightCoordsUtil.smoothSky(lightCoords) / PACKED_LIGHT_SCALE);
		final float blockPart = brightness(blockLevel) * blockFactor;
		final float total = blockPart + brightness(skyLevel) * skyFactor + ambient;
		return total <= 1.0e-5F ? 0F : Math.min(1F, blockPart / total);
	}

	private static float brightness(float level) {
		return level / (4F - 3F * level);
	}

	@Nullable
	private static LightmapRenderState lightmapRenderState() {
		final Minecraft minecraft = Minecraft.getInstance();
		if (minecraft == null || minecraft.gameRenderer == null) return null;
		return minecraft.gameRenderer.gameRenderState().lightmapRenderState;
	}

	public static void pushSubmitTint(int tint, int skyTint) {
		if (tintDepth == blockTintStack.length) {
			blockTintStack = Arrays.copyOf(blockTintStack, tintDepth * 2);
			skyTintStack = Arrays.copyOf(skyTintStack, tintDepth * 2);
		}
		skyTintStack[tintDepth] = skyTint;
		blockTintStack[tintDepth++] = tint;
	}

	public static void popSubmitTint() {
		if (tintDepth > 0) tintDepth--;
	}

	public static void resetScopes() {
		tintDepth = 0;
		ColorProbe.get().invalidate();
	}

	public static int blockTint() {
		return tintDepth == 0 ? NO_TINT : blockTintStack[tintDepth - 1];
	}

	public static int skyTint() {
		return tintDepth == 0 ? NO_TINT : skyTintStack[tintDepth - 1];
	}

	public static void beginBlockQuads(int tint, int lightCoords, RenderType renderType) {
		blockTint = lightCoords == LightCoordsUtil.FULL_BRIGHT || !liesUnderLightmap(renderType)
			? NO_TINT : tint;
	}

	public static void beginBlockQuads(int tint) {
		blockTint = tint;
	}

	public static void endBlockQuads() {
		blockTint = NO_TINT;
	}

	public static void beginMovingBlockQuads(int tint) {
		movingBlockTint = tint;
	}

	public static void endMovingBlockQuads() {
		movingBlockTint = NO_TINT;
	}

	public static int tintMovingBlockQuadColor(int quadColor) {
		return movingBlockTint == NO_TINT ? quadColor : ARGB.multiply(quadColor, movingBlockTint);
	}

	public static int tintBlockQuadColor(int quadColor, int selfEmission) {
		if (selfEmission >= LightEngine.MAX_LEVEL || blockTint == NO_TINT) return quadColor;
		if (selfEmission <= 0) return ARGB.multiply(quadColor, blockTint);

		final float selfEmissionStrength = selfEmission / (float) LightEngine.MAX_LEVEL;
		return ARGB.multiply(quadColor, ARGB.srgbLerp(selfEmissionStrength, blockTint, ARGB.white(1F)));
	}

	public static void beginModelQuads(int tint) {
		modelTint = tint;
	}

	public static void beginModelQuads(int tint, int skyTint) {
		modelTint = tint;
		modelSkyTint = skyTint;
	}

	public static void endModelQuads() {
		modelTint = NO_TINT;
		modelSkyTint = NO_TINT;
	}

	public static int modelTintColor() {
		return shaderChroma(modelTint);
	}

	public static int modelSkyTintColor() {
		return modelSkyTint == NO_TINT ? ChromaBaker.NEUTRAL_SKY_ARGB : modelSkyTint;
	}

	public static int blockTintOrNeutral(int tint) {
		return tint == NO_TINT ? ChromaBlender.NEUTRAL_ARGB : tint;
	}

	public static int shaderChroma(int tint) {
		if (tint == NO_TINT) return ChromaBaker.NEUTRAL_ARGB;

		return 0xFF000000
			| (halfChannel(tint >> 16) << 16)
			| (halfChannel(tint >> 8) << 8)
			| halfChannel(tint);
	}

	private static int halfChannel(int shifted) {
		return ((shifted & 0xFF) + 1) >> 1;
	}

	public static int tintParticleColor(int color, int lightCoords, double x, double y, double z) {
		if (lightCoords == LightCoordsUtil.FULL_BRIGHT) return color;

		final int tint = resolveParticle(x, y, z, lightCoords);
		return tint == NO_TINT ? color : ARGB.multiply(color, tint);
	}

	private static boolean liesUnderLightmap(RenderType renderType) {
		if (renderType.isOutline()) return false;

		final RenderPipeline pipeline = renderType.pipeline();
		if (pipeline.getShaderDefines().flags().contains(EMISSIVE_DEFINITION)) return false;

		final VertexFormat[] bindings = pipeline.getVertexFormatBindings();
		return bindings.length > 0 && bindings[0].contains(DefaultVertexFormat.UV2_SEMANTIC_NAME);
	}

	private ChromaFold() {}
}
