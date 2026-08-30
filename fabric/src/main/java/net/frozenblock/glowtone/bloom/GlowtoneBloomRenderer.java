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

package net.frozenblock.glowtone.bloom;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderPassDescriptor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Supplier;
import net.frozenblock.glowtone.config.pack.GlowtonePackSettings;
import net.frozenblock.glowtone.GlowtoneConstants;
import net.frozenblock.glowtone.config.GlowtoneConfig;
import net.frozenblock.glowtone.config.option.bloom.BloomOption;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.renderer.RenderPipelines;
import org.joml.Vector4f;
import org.joml.Vector4fc;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryStack;

@ClientOnly
public final class GlowtoneBloomRenderer {
	private static final Vector4f CLEAR_COLOR = new Vector4f(0F, 0F, 0F, 0F);
	private static final int UNIFORM_SIZE = new Std140SizeCalculator().putVec2().putFloat().putFloat().get();
	private static final int BLUR_ITERATIONS = 4;
	private static final int BLUR_DOWNSAMPLE = 2;

	private static final BindGroupLayout BLOOM_BIND_GROUP = BindGroupLayout.builder()
		.withSampler("InSampler")
		.withUniform("BloomConfig", UniformType.UNIFORM_BUFFER)
		.build();

	private static final RenderPipeline BLUR_PIPELINE = RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
		.withLocation(GlowtoneConstants.id("pipeline/bloom_blur"))
		.withVertexShader("core/screenquad")
		.withFragmentShader(GlowtoneConstants.id("post/bloom_blur"))
		.withBindGroupLayout(BLOOM_BIND_GROUP)
		.withColorTargetState(ColorTargetState.DEFAULT)
		.build();

	private static final RenderPipeline COMPOSITE_PIPELINE = RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
		.withLocation(GlowtoneConstants.id("pipeline/bloom_composite"))
		.withVertexShader("core/screenquad")
		.withFragmentShader(GlowtoneConstants.id("post/bloom_composite"))
		.withBindGroupLayout(BLOOM_BIND_GROUP)
		.withColorTargetState(new ColorTargetState(BlendFunction.ADDITIVE))
		.build();

	private static boolean emissiveAttached;
	private static @Nullable TextureTarget emissiveTarget;
	private static @Nullable TextureTarget blurTargetA;
	private static @Nullable TextureTarget blurTargetB;
	private static @Nullable RenderPipeline lastBasePipeline;
	private static @Nullable RenderPipeline lastTwinPipeline;
	private static @Nullable GpuBuffer horizontalUniform;
	private static @Nullable GpuBuffer verticalUniform;
	private static @Nullable GpuBuffer compositeUniform;
	private static int uniformWidth;
	private static int uniformHeight;
	private static float uniformStrength = -1F;
	private static float uniformRadius = -1F;
	private static float uniformIntensity = -1F;

	private GlowtoneBloomRenderer() {}

	public static boolean isEnabled() {
		return GlowtoneConfig.bloomEnabled();
	}

	public static @Nullable GpuTextureView emissiveView(int width, int height) {
		final TextureTarget target = emissiveTarget;
		if (target == null || target.width != width || target.height != height) return null;
		return target.getColorTextureView();
	}

	public static @Nullable RenderPass createEmissiveRenderPass(
		CommandEncoder encoder,
		Supplier<String> label,
		GpuTextureView colorView,
		Optional<Vector4fc> clearColor,
		@Nullable GpuTextureView depthView,
		OptionalDouble clearDepth
	) {
		emissiveAttached = false;
		final GpuTextureView emissive = emissiveView(colorView.getWidth(0), colorView.getHeight(0));
		if (emissive == null) return null;

		RenderPassDescriptor descriptor = RenderPassDescriptor.create(label)
			.withColorAttachment(colorView, clearColor)
			.withColorAttachment(emissive, Optional.empty())
			.withRenderArea(new RenderPass.RenderArea(0, 0, colorView.getWidth(0), colorView.getHeight(0)));
		if (depthView != null) descriptor = descriptor.withDepthAttachment(depthView, clearDepth);
		final RenderPass pass = encoder.createRenderPass(descriptor);
		emissiveAttached = true;
		return pass;
	}

	public static RenderPipeline pipelineFor(RenderPipeline pipeline) {
		if (!emissiveAttached) return pipeline;
		if (pipeline == lastBasePipeline) return lastTwinPipeline;

		final RenderPipeline twin = GlowtoneEmissivePipeline.of(pipeline);
		lastBasePipeline = pipeline;
		lastTwinPipeline = twin;
		return twin;
	}

	public static void beginFrame(int width, int height) {
		if (!isEnabled()) {
			free();
			return;
		}

		final int blurWidth = Math.max(1, width / BLUR_DOWNSAMPLE);
		final int blurHeight = Math.max(1, height / BLUR_DOWNSAMPLE);

		if (emissiveTarget == null || blurTargetA == null || blurTargetB == null) {
			free();
			emissiveTarget = new TextureTarget("Glowtone Emissive", width, height, false, GpuFormat.RGBA8_UNORM);
			blurTargetA = new TextureTarget("Glowtone Bloom Blur A", blurWidth, blurHeight, false, GpuFormat.RGBA8_UNORM);
			blurTargetB = new TextureTarget("Glowtone Bloom Blur B", blurWidth, blurHeight, false, GpuFormat.RGBA8_UNORM);
		} else if (emissiveTarget.width != width || emissiveTarget.height != height) {
			emissiveTarget.resize(width, height);
			blurTargetA.resize(blurWidth, blurHeight);
			blurTargetB.resize(blurWidth, blurHeight);
		}

		RenderSystem.getDevice().createCommandEncoder().clearColorTexture(emissiveTarget.getColorTexture(), CLEAR_COLOR);
	}

	public static void render(RenderTarget mainTarget) {
		if (!isEnabled()) return;

		final TextureTarget emissive = emissiveTarget;
		final TextureTarget blurA = blurTargetA;
		final TextureTarget blurB = blurTargetB;
		if (emissive == null || blurA == null || blurB == null) return;
		if (emissive.width != mainTarget.width || emissive.height != mainTarget.height) return;

		updateUniforms(blurA.width, blurA.height, BloomOption.strength());

		final CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();

		GpuTextureView source = emissive.getColorTextureView();
		for (int iteration = 0; iteration < BLUR_ITERATIONS; iteration++) {
			blur(encoder, source, blurA, horizontalUniform);
			blur(encoder, blurA.getColorTextureView(), blurB, verticalUniform);
			source = blurB.getColorTextureView();
		}

		try (RenderPass pass = encoder.createRenderPass(
			() -> "Glowtone bloom composite",
			mainTarget.getColorTextureView(),
			Optional.empty(),
			null,
			OptionalDouble.empty()
		)) {
			pass.setPipeline(COMPOSITE_PIPELINE);
			RenderSystem.bindDefaultUniforms(pass);
			pass.setUniform("BloomConfig", compositeUniform);
			pass.bindTexture("InSampler", source, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
			pass.draw(3, 1, 0, 0);
		}
	}

	private static void blur(CommandEncoder encoder, GpuTextureView input, TextureTarget output, GpuBuffer uniform) {
		try (RenderPass pass = encoder.createRenderPass(
			() -> "Glowtone bloom blur",
			output.getColorTextureView(),
			Optional.empty(),
			null,
			OptionalDouble.empty()
		)) {
			pass.setPipeline(BLUR_PIPELINE);
			RenderSystem.bindDefaultUniforms(pass);
			pass.setUniform("BloomConfig", uniform);
			pass.bindTexture("InSampler", input, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
			pass.draw(3, 1, 0, 0);
		}
	}

	private static void updateUniforms(int width, int height, float strength) {
		// Without these in the key, a reload changes the settings but never the uniforms.
		final float packRadius = GlowtonePackSettings.bloomRadius();
		final float packIntensity = GlowtonePackSettings.bloomIntensity();
		if (width == uniformWidth
			&& height == uniformHeight
			&& strength == uniformStrength
			&& packRadius == uniformRadius
			&& packIntensity == uniformIntensity
		) {
			return;
		}

		closeUniforms();
		uniformWidth = width;
		uniformHeight = height;
		uniformStrength = strength;
		uniformRadius = packRadius;
		uniformIntensity = packIntensity;

		final float radius = packRadius / BLUR_DOWNSAMPLE;
		horizontalUniform = createUniform("Glowtone bloom blur X", 1F / width, 0F, radius, strength);
		verticalUniform = createUniform("Glowtone bloom blur Y", 0F, 1F / height, radius, strength);
		compositeUniform = createUniform("Glowtone bloom composite", 0F, 0F, 0F, strength * packIntensity);
	}

	private static GpuBuffer createUniform(String label, float dirX, float dirY, float radius, float strength) {
		try (MemoryStack stack = MemoryStack.stackPush()) {
			final ByteBuffer data = Std140Builder.onStack(stack, UNIFORM_SIZE)
				.putVec2(dirX, dirY)
				.putFloat(radius)
				.putFloat(strength)
				.get();
			return RenderSystem.getDevice().createBuffer(() -> label, GpuBuffer.USAGE_UNIFORM, data);
		}
	}

	private static void closeUniforms() {
		if (horizontalUniform != null) horizontalUniform.close();
		if (verticalUniform != null) verticalUniform.close();
		if (compositeUniform != null) compositeUniform.close();
		horizontalUniform = null;
		verticalUniform = null;
		compositeUniform = null;
	}

	public static void free() {
		if (emissiveTarget != null) {
			emissiveTarget.destroyBuffers();
			emissiveTarget = null;
		}
		if (blurTargetA != null) {
			blurTargetA.destroyBuffers();
			blurTargetA = null;
		}
		if (blurTargetB != null) {
			blurTargetB.destroyBuffers();
			blurTargetB = null;
		}
		lastBasePipeline = null;
		lastTwinPipeline = null;
		closeUniforms();
		uniformWidth = 0;
		uniformHeight = 0;
		uniformStrength = -1F;
		uniformRadius = -1F;
		uniformIntensity = -1F;
	}
}
