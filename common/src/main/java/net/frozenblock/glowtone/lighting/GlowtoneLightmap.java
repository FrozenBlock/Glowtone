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

package net.frozenblock.glowtone.lighting;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.renderer.Lightmap;
import net.minecraft.client.renderer.state.LightmapRenderState;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ARGB;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

@ClientOnly
public final class GlowtoneLightmap {
	private static final int SIZE = Lightmap.TEXTURE_SIZE;
	private static final float LUMA_RED = 0.2126F;
	private static final float LUMA_GREEN = 0.7152F;
	private static final float LUMA_BLUE = 0.0722F;
	private static final Vector3fc WHITE = new Vector3f(1F, 1F, 1F);
	private static final Vector3fc BLACK = new Vector3f();
	private static final Vector3fc BOSS_DARKENING = new Vector3f(0.7F, 0.6F, 0.6F);

	private static @Nullable DynamicTexture texture;

	public static GpuTextureView textureView() {
		return texture().getTextureView();
	}

	private static DynamicTexture texture() {
		DynamicTexture current = texture;
		if (current == null) current = texture = new DynamicTexture("Glowtone Lightmap", SIZE, SIZE, false);
		return current;
	}

	public static void close() {
		final DynamicTexture current = texture;
		texture = null;
		if (current != null) current.close();
	}

	static void render(LightmapRenderState state, GlowtoneLighting.Applied applied) {
		final DynamicTexture target = texture();
		final NativeImage pixels = target.getPixels();
		if (pixels == null) return;

		final Vector3f color = new Vector3f();
		final Vector3f scratch = new Vector3f();
		for (int sky = 0; sky < SIZE; sky++) {
			for (int block = 0; block < SIZE; block++) {
				compute(state, applied, block, sky, color, scratch);
				pixels.setPixel(block, sky, ARGB.colorFromFloat(1F, color.x, color.y, color.z));
			}
		}

		target.upload();
	}

	static Vector3f texel(LightmapRenderState state, GlowtoneLighting.Applied applied, int block, int sky) {
		final Vector3f color = new Vector3f();
		compute(state, applied, block, sky, color, new Vector3f());
		return color;
	}

	private static void compute(LightmapRenderState state, GlowtoneLighting.Applied applied, int block, int sky, Vector3f color, Vector3f scratch) {
		final Vector3fc ambient = orDefault(state.ambientColor, BLACK);
		final Vector3fc nightVision = orDefault(state.nightVisionColor, WHITE);
		final Vector3fc skyLight = orDefault(state.skyLightColor, WHITE);
		final Vector3fc blockTint = orDefault(state.blockLightTint, WHITE);
		final float skyBrightness = brightness(applied.skyCurve()[sky]) * state.skyFactor;
		final float blockLevel = applied.blockCurve()[block];
		final float blockBrightness = brightness(blockLevel) * state.blockFactor;

		color.set(
			Math.max(ambient.x(), nightVision.x() * state.nightVisionEffectIntensity),
			Math.max(ambient.y(), nightVision.y() * state.nightVisionEffectIntensity),
			Math.max(ambient.z(), nightVision.z() * state.nightVisionEffectIntensity)
		);
		color.add(skyLight.x() * skyBrightness, skyLight.y() * skyBrightness, skyLight.z() * skyBrightness);

		final float whiten = 0.9F * parabolic(blockLevel);
		scratch.set(blockTint).lerp(WHITE, whiten).mul(blockBrightness);
		color.add(scratch);

		scratch.set(color).mul(BOSS_DARKENING);
		color.lerp(scratch, state.bossOverlayWorldDarkening);
		color.sub(state.darknessEffectScale, state.darknessEffectScale, state.darknessEffectScale);
		clamp(color);

		scratch.set(color);
		notGamma(scratch);
		color.lerp(scratch, state.brightness);

		grade(color, applied, scratch);
		clamp(color);
	}

	private static void grade(Vector3f color, GlowtoneLighting.Applied applied, Vector3f scratch) {
		color.mul(applied.exposure());

		final float contrast = applied.contrast();
		color.set(
			(color.x - 0.5F) * contrast + 0.5F,
			(color.y - 0.5F) * contrast + 0.5F,
			(color.z - 0.5F) * contrast + 0.5F
		);

		final float luma = LUMA_RED * color.x + LUMA_GREEN * color.y + LUMA_BLUE * color.z;
		scratch.set(luma, luma, luma);
		scratch.lerp(color, applied.saturation());
		color.set(scratch);

		final Vector3f lift = applied.lift();
		final Vector3f gain = applied.gain();
		color.set(
			lift.x + color.x * (gain.x - lift.x),
			lift.y + color.y * (gain.y - lift.y),
			lift.z + color.z * (gain.z - lift.z)
		);
	}

	private static Vector3fc orDefault(@Nullable Vector3fc value, Vector3fc fallback) {
		return value == null ? fallback : value;
	}

	private static float brightness(float level) {
		return level / (4F - 3F * level);
	}

	private static float parabolic(float level) {
		return (2F * level - 1F) * (2F * level - 1F);
	}

	private static void notGamma(Vector3f color) {
		final float maxComponent = Math.max(color.x, Math.max(color.y, color.z));
		if (maxComponent <= 0F) return;

		final float maxInverted = 1F - maxComponent;
		final float maxScaled = 1F - maxInverted * maxInverted * maxInverted * maxInverted;
		color.mul(maxScaled / maxComponent);
	}

	private static void clamp(Vector3f color) {
		color.set(Math.clamp(color.x, 0F, 1F), Math.clamp(color.y, 0F, 1F), Math.clamp(color.z, 0F, 1F));
	}

	private GlowtoneLightmap() {}
}
