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
import com.mojang.blaze3d.platform.BlendFactor;
import com.mojang.blaze3d.platform.BlendOp;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderPassDescriptor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Supplier;
import net.frozenblock.glowtone.config.pack.GlowtonePackSettings;
import net.frozenblock.glowtone.GlowtoneConstants;
import net.frozenblock.glowtone.config.GlowtoneConfig;
import net.frozenblock.glowtone.config.GlowtoneDebugEntries;
import net.frozenblock.glowtone.config.option.bloom.BloomOption;
import net.frozenblock.glowtone.render.SceneDepth;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.renderer.RenderPipelines;
import org.joml.Vector4f;
import org.joml.Vector4fc;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryStack;

@ClientOnly
public final class GlowtoneBloomRenderer {
	private static final Vector4f CLEAR_COLOR = new Vector4f(0F, 0F, 0F, 0F);
	private static final int UNIFORM_SIZE = new Std140SizeCalculator()
		.putVec2().putFloat().putFloat().putFloat().putFloat().putFloat().get();
	private static final int BLUR_ITERATIONS = 4;
	private static final int BLUR_DOWNSAMPLE = 2;
	private static final GpuFormat DEPTH_FORMAT = GpuFormat.R16_FLOAT;
	private static final int MAX_DEPTH_TARGETS = 8;

	private static final BindGroupLayout BLUR_BIND_GROUP = BindGroupLayout.builder()
		.withSampler("InSampler")
		.withSampler("DepthSampler")
		.withUniform("BloomConfig", UniformType.UNIFORM_BUFFER)
		.build();

	private static final BindGroupLayout DEBUG_BIND_GROUP = BindGroupLayout.builder()
		.withSampler("InSampler")
		.build();

	private static final BindGroupLayout COMPOSITE_BIND_GROUP = BindGroupLayout.builder()
		.withSampler("InSampler")
		.withUniform("BloomConfig", UniformType.UNIFORM_BUFFER)
		.build();

	private static final BindGroupLayout DEPTH_BIND_GROUP = BindGroupLayout.builder()
		.withSampler("DepthSampler")
		.withUniform("BloomConfig", UniformType.UNIFORM_BUFFER)
		.build();

	private static final BindGroupLayout FOLD_BIND_GROUP = BindGroupLayout.builder()
		.withSampler("InSampler")
		.withSampler("DepthSampler")
		.withSampler("ResolvedSampler")
		.withUniform("BloomConfig", UniformType.UNIFORM_BUFFER)
		.build();

	private static final RenderPipeline FOLD_PIPELINE = RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
		.withLocation(GlowtoneConstants.id("pipeline/bloom_fold"))
		.withVertexShader("core/screenquad")
		.withFragmentShader(GlowtoneConstants.id("post/bloom_fold"))
		.withBindGroupLayout(FOLD_BIND_GROUP)
		.withColorTargetState(new ColorTargetState(BlendFunction.ADDITIVE))
		.build();

	private static final RenderPipeline DEPTH_PIPELINE = RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
		.withLocation(GlowtoneConstants.id("pipeline/bloom_depth"))
		.withVertexShader("core/screenquad")
		.withFragmentShader(GlowtoneConstants.id("post/bloom_depth"))
		.withBindGroupLayout(DEPTH_BIND_GROUP)
		.withColorTargetState(new ColorTargetState(Optional.empty(), DEPTH_FORMAT, ColorTargetState.WRITE_ALL))
		.build();

	private static final RenderPipeline DEPTH_MERGE_PIPELINE = RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
		.withLocation(GlowtoneConstants.id("pipeline/bloom_depth_merge"))
		.withVertexShader("core/screenquad")
		.withFragmentShader(GlowtoneConstants.id("post/bloom_depth"))
		.withBindGroupLayout(DEPTH_BIND_GROUP)
		.withColorTargetState(new ColorTargetState(
			Optional.of(new BlendFunction(BlendFactor.ONE, BlendFactor.ONE, BlendOp.MIN, BlendFactor.ONE, BlendFactor.ONE, BlendOp.MIN)),
			DEPTH_FORMAT,
			ColorTargetState.WRITE_ALL
		))
		.build();

	private static final RenderPipeline BLUR_PIPELINE = RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
		.withLocation(GlowtoneConstants.id("pipeline/bloom_blur"))
		.withVertexShader("core/screenquad")
		.withFragmentShader(GlowtoneConstants.id("post/bloom_blur"))
		.withBindGroupLayout(BLUR_BIND_GROUP)
		.withColorTargetState(ColorTargetState.DEFAULT)
		.build();

	private static final RenderPipeline COMPOSITE_PIPELINE = RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
		.withLocation(GlowtoneConstants.id("pipeline/bloom_composite"))
		.withVertexShader("core/screenquad")
		.withFragmentShader(GlowtoneConstants.id("post/bloom_composite"))
		.withBindGroupLayout(COMPOSITE_BIND_GROUP)
		.withColorTargetState(new ColorTargetState(BlendFunction.ADDITIVE))
		.build();

	private static final RenderPipeline DEBUG_PIPELINE = RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
		.withLocation(GlowtoneConstants.id("pipeline/bloom_debug"))
		.withVertexShader("core/screenquad")
		.withFragmentShader(GlowtoneConstants.id("post/bloom_debug"))
		.withBindGroupLayout(DEBUG_BIND_GROUP)
		.withColorTargetState(ColorTargetState.DEFAULT)
		.build();

	private static boolean emissiveAttached;
	private static boolean depthCaptured;
	private static @Nullable TextureTarget emissiveTarget;
	private static @Nullable TextureTarget deferredEmissiveTarget;
	private static @Nullable TextureTarget blurTargetA;
	private static @Nullable TextureTarget blurTargetB;
	private static @Nullable TextureTarget depthTarget;
	private static @Nullable RenderPipeline lastBasePipeline;
	private static @Nullable RenderPipeline lastTwinPipeline;
	private static @Nullable GpuBuffer horizontalUniform;
	private static @Nullable GpuBuffer verticalUniform;
	private static @Nullable GpuBuffer compositeUniform;
	private static @Nullable GpuBuffer depthUniform;
	private static final List<GpuTextureView> sceneDepthViews = new ArrayList<>(MAX_DEPTH_TARGETS);
	private static @Nullable GpuTextureView deferredDepthView;
	private static boolean deferredWanted;
	private static int uniformWidth;
	private static int uniformHeight;
	private static float uniformStrength = -1F;
	private static float uniformRadius = -1F;
	private static float uniformIntensity = -1F;
	private static float uniformFar = -1F;

	private GlowtoneBloomRenderer() {}

	public static boolean isEnabled() {
		return GlowtoneConfig.bloomEnabled();
	}

	public static boolean toggleBufferDebug() {
		return GlowtoneDebugEntries.toggle(GlowtoneDebugEntries.EMISSIVE_BUFFER);
	}

	private static @Nullable GpuTextureView emissiveView(int width, int height) {
		return viewOf(emissiveTarget, width, height);
	}

	private static @Nullable GpuTextureView primaryDepthView() {
		return sceneDepthViews.isEmpty() ? null : sceneDepthViews.get(0);
	}

	private static @Nullable GpuTextureView viewOf(@Nullable TextureTarget target, int width, int height) {
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
		return createEmissiveRenderPass(encoder, label, colorView, clearColor, depthView, clearDepth, false);
	}

	public static @Nullable RenderPass createEmissiveRenderPass(
		CommandEncoder encoder,
		Supplier<String> label,
		GpuTextureView colorView,
		Optional<Vector4fc> clearColor,
		@Nullable GpuTextureView depthView,
		OptionalDouble clearDepth,
		boolean terrain
	) {
		emissiveAttached = false;
		if (depthView != null && !sceneDepthViews.contains(depthView) && sceneDepthViews.size() < MAX_DEPTH_TARGETS) {
			sceneDepthViews.add(depthView);
		}

		final boolean deferred = terrain && depthView != null && depthView != primaryDepthView();
		if (deferred) deferredWanted = true;

		final GpuTextureView emissive = deferred
			? viewOf(deferredEmissiveTarget, colorView.getWidth(0), colorView.getHeight(0))
			: emissiveView(colorView.getWidth(0), colorView.getHeight(0));
		if (emissive == null) return null;
		if (deferred) deferredDepthView = depthView;

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

		final int blurWidth = blurSize(width);
		final int blurHeight = blurSize(height);

		deferredDepthView = null;
		sceneDepthViews.clear();

		if (emissiveTarget == null || blurTargetA == null || blurTargetB == null || depthTarget == null) {
			free();
			emissiveTarget = new TextureTarget("Glowtone Emissive", width, height, false, GpuFormat.RGBA8_UNORM);
			blurTargetA = new TextureTarget("Glowtone Bloom Blur A", blurWidth, blurHeight, false, GpuFormat.RGBA8_UNORM);
			blurTargetB = new TextureTarget("Glowtone Bloom Blur B", blurWidth, blurHeight, false, GpuFormat.RGBA8_UNORM);
			depthTarget = new TextureTarget("Glowtone Bloom Depth", blurWidth, blurHeight, false, DEPTH_FORMAT);
		} else if (emissiveTarget.width != width || emissiveTarget.height != height) {
			emissiveTarget.resize(width, height);
			blurTargetA.resize(blurWidth, blurHeight);
			blurTargetB.resize(blurWidth, blurHeight);
			depthTarget.resize(blurWidth, blurHeight);
		}

		if (deferredWanted && deferredEmissiveTarget == null) {
			deferredEmissiveTarget = new TextureTarget("Glowtone Deferred Emissive", width, height, false, GpuFormat.RGBA8_UNORM);
		} else if (deferredEmissiveTarget != null && (deferredEmissiveTarget.width != width || deferredEmissiveTarget.height != height)) {
			deferredEmissiveTarget.resize(width, height);
		}

		final CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
		encoder.clearColorTexture(emissiveTarget.getColorTexture(), CLEAR_COLOR);
		if (deferredEmissiveTarget != null) encoder.clearColorTexture(deferredEmissiveTarget.getColorTexture(), CLEAR_COLOR);
	}

	public static void captureDepth(RenderTarget mainTarget) {
		depthCaptured = false;
		if (!isEnabled()) return;

		final GpuTextureView main = mainTarget.getDepthTextureView();
		if (main == null || !prepareDepth(mainTarget)) return;

		final CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
		depthPass(encoder, main, DEPTH_PIPELINE, "Glowtone bloom depth");
		for (GpuTextureView view : sceneDepthViews) {
			if (view != main) depthPass(encoder, view, DEPTH_MERGE_PIPELINE, "Glowtone bloom depth resolve");
		}
		depthCaptured = true;
	}

	private static boolean prepareDepth(RenderTarget mainTarget) {
		final TextureTarget target = depthTarget;
		if (target == null) return false;
		if (target.width != blurSize(mainTarget.width) || target.height != blurSize(mainTarget.height)) return false;

		updateUniforms(target.width, target.height, mainTarget.width, mainTarget.height, BloomOption.strength());
		return depthUniform != null;
	}

	private static void depthPass(CommandEncoder encoder, GpuTextureView depth, RenderPipeline pipeline, String label) {
		final TextureTarget target = depthTarget;
		if (target == null) return;

		try (RenderPass pass = encoder.createRenderPass(
			() -> label,
			target.getColorTextureView(),
			Optional.empty(),
			null,
			OptionalDouble.empty()
		)) {
			pass.setPipeline(pipeline);
			RenderSystem.bindDefaultUniforms(pass);
			pass.setUniform("BloomConfig", depthUniform);
			pass.bindTexture("DepthSampler", depth, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
			pass.draw(3, 1, 0, 0);
		}
	}

	public static void render(RenderTarget mainTarget) {
		if (!isEnabled()) return;

		final TextureTarget emissive = emissiveTarget;
		final TextureTarget blurA = blurTargetA;
		final TextureTarget blurB = blurTargetB;
		final TextureTarget scene = depthTarget;
		if (emissive == null || blurA == null || blurB == null || scene == null) return;
		if (emissive.width != mainTarget.width || emissive.height != mainTarget.height) return;
		if (!depthCaptured) return;

		final CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();

		final GpuTextureView held = mainTarget.getDepthTextureView();
		if (held != null) depthPass(encoder, held, DEPTH_MERGE_PIPELINE, "Glowtone bloom depth merge");

		foldDeferredEmissive(encoder, emissive);

		if (GlowtoneDebugEntries.enabled(GlowtoneDebugEntries.EMISSIVE_BUFFER)) {
			draw(encoder, DEBUG_PIPELINE, "Glowtone emissive buffer", mainTarget, emissive.getColorTextureView(), null, null);
			return;
		}

		final GpuTextureView depth = scene.getColorTextureView();
		GpuTextureView source = emissive.getColorTextureView();
		for (int iteration = 0; iteration < BLUR_ITERATIONS; iteration++) {
			blur(encoder, source, blurA, horizontalUniform, depth);
			blur(encoder, blurA.getColorTextureView(), blurB, verticalUniform, depth);
			source = blurB.getColorTextureView();
		}

		draw(encoder, COMPOSITE_PIPELINE, "Glowtone bloom composite", mainTarget, source, compositeUniform, null);
	}

	private static void foldDeferredEmissive(CommandEncoder encoder, TextureTarget emissive) {
		final TextureTarget deferred = deferredEmissiveTarget;
		final TextureTarget resolved = depthTarget;
		final GpuTextureView source = deferredDepthView;
		if (deferred == null || resolved == null || source == null) return;
		if (deferred.width != emissive.width || deferred.height != emissive.height) return;

		try (RenderPass pass = encoder.createRenderPass(
			() -> "Glowtone bloom fold",
			emissive.getColorTextureView(),
			Optional.empty(),
			null,
			OptionalDouble.empty()
		)) {
			pass.setPipeline(FOLD_PIPELINE);
			RenderSystem.bindDefaultUniforms(pass);
			pass.setUniform("BloomConfig", depthUniform);
			pass.bindTexture("InSampler", deferred.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
			pass.bindTexture("DepthSampler", source, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
			pass.bindTexture("ResolvedSampler", resolved.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
			pass.draw(3, 1, 0, 0);
		}
	}

	private static void blur(
		CommandEncoder encoder, GpuTextureView input, TextureTarget output, @Nullable GpuBuffer uniform, GpuTextureView depth
	) {
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
			pass.bindTexture("DepthSampler", depth, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
			pass.draw(3, 1, 0, 0);
		}
	}

	private static void draw(
		CommandEncoder encoder, RenderPipeline pipeline, String label,
		RenderTarget target, GpuTextureView input, @Nullable GpuBuffer uniform, @Nullable GpuTextureView depth
	) {
		try (RenderPass pass = encoder.createRenderPass(
			() -> label,
			target.getColorTextureView(),
			Optional.empty(),
			null,
			OptionalDouble.empty()
		)) {
			pass.setPipeline(pipeline);
			RenderSystem.bindDefaultUniforms(pass);
			if (uniform != null) pass.setUniform("BloomConfig", uniform);
			pass.bindTexture("InSampler", input, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
			if (depth != null) {
				pass.bindTexture("DepthSampler", depth, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
			}
			pass.draw(3, 1, 0, 0);
		}
	}

	private static int blurSize(int size) {
		return Math.max(1, size / BLUR_DOWNSAMPLE);
	}

	private static void updateUniforms(int width, int height, int sceneWidth, int sceneHeight, float strength) {
		// Without these in the key, a reload changes the settings but never the uniforms.
		final float packRadius = GlowtonePackSettings.bloomRadius();
		final float packIntensity = GlowtonePackSettings.bloomIntensity();
		final float far = SceneDepth.far();
		if (width == uniformWidth
			&& height == uniformHeight
			&& strength == uniformStrength
			&& packRadius == uniformRadius
			&& packIntensity == uniformIntensity
			&& far == uniformFar
		) {
			return;
		}

		closeUniforms();
		uniformWidth = width;
		uniformHeight = height;
		uniformStrength = strength;
		uniformRadius = packRadius;
		uniformIntensity = packIntensity;
		uniformFar = far;

		final float radius = packRadius / BLUR_DOWNSAMPLE;
		horizontalUniform = createUniform("Glowtone bloom blur X", 1F / width, 0F, radius, strength, far);
		verticalUniform = createUniform("Glowtone bloom blur Y", 0F, 1F / height, radius, strength, far);
		compositeUniform = createUniform("Glowtone bloom composite", 0F, 0F, 0F, strength * packIntensity, far);
		depthUniform = createUniform("Glowtone bloom depth", 1F / sceneWidth, 1F / sceneHeight, 0F, 0F, far);
	}

	private static GpuBuffer createUniform(String label, float dirX, float dirY, float radius, float strength, float far) {
		try (MemoryStack stack = MemoryStack.stackPush()) {
			final ByteBuffer data = Std140Builder.onStack(stack, UNIFORM_SIZE)
				.putVec2(dirX, dirY)
				.putFloat(radius)
				.putFloat(strength)
				.putFloat(SceneDepth.NEAR)
				.putFloat(far)
				.putFloat(SceneDepth.reversed() ? 1F : 0F)
				.get();
			return RenderSystem.getDevice().createBuffer(() -> label, GpuBuffer.USAGE_UNIFORM, data);
		}
	}

	private static void closeUniforms() {
		if (horizontalUniform != null) horizontalUniform.close();
		if (verticalUniform != null) verticalUniform.close();
		if (compositeUniform != null) compositeUniform.close();
		if (depthUniform != null) depthUniform.close();
		horizontalUniform = null;
		verticalUniform = null;
		compositeUniform = null;
		depthUniform = null;
	}

	public static void free() {
		if (emissiveTarget != null) {
			emissiveTarget.destroyBuffers();
			emissiveTarget = null;
		}
		if (deferredEmissiveTarget != null) {
			deferredEmissiveTarget.destroyBuffers();
			deferredEmissiveTarget = null;
		}
		deferredDepthView = null;
		sceneDepthViews.clear();
		if (blurTargetA != null) {
			blurTargetA.destroyBuffers();
			blurTargetA = null;
		}
		if (blurTargetB != null) {
			blurTargetB.destroyBuffers();
			blurTargetB = null;
		}
		if (depthTarget != null) {
			depthTarget.destroyBuffers();
			depthTarget = null;
		}
		lastBasePipeline = null;
		lastTwinPipeline = null;
		closeUniforms();
		uniformWidth = 0;
		uniformHeight = 0;
		uniformStrength = -1F;
		uniformRadius = -1F;
		uniformIntensity = -1F;
		uniformFar = -1F;
	}
}
