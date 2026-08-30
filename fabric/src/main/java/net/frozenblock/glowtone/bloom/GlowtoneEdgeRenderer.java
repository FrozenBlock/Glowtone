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

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.OptionalDouble;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.frozenblock.glowtone.GlowtoneConstants;
import net.frozenblock.glowtone.config.option.edge.EdgeHighlightOption;
import net.frozenblock.glowtone.config.pack.GlowtonePackSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryStack;

@Environment(EnvType.CLIENT)
public final class GlowtoneEdgeRenderer {
	private static final int UNIFORM_SIZE = new Std140SizeCalculator()
		.putVec2().putFloat().putFloat().putFloat().putFloat().putFloat().putFloat().putFloat().get();

	private static final float THRESHOLD = 0.02F;

	private static final float REFERENCE_HEIGHT = 1080F;

	private static final float NEAR = 0.05F;
	private static final float MIN_FAR = 64F;

	private static final BindGroupLayout EDGE_BIND_GROUP = BindGroupLayout.builder()
		.withSampler("DepthSampler")
		.withUniform("EdgeConfig", UniformType.UNIFORM_BUFFER)
		.build();

	private static final RenderPipeline EDGE_PIPELINE = RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
		.withLocation(GlowtoneConstants.id("pipeline/edge_highlight"))
		.withVertexShader("core/screenquad")
		.withFragmentShader(GlowtoneConstants.id("post/edge_highlight"))
		.withBindGroupLayout(EDGE_BIND_GROUP)
		.withColorTargetState(new ColorTargetState(BlendFunction.ADDITIVE))
		.build();

	private static @Nullable GpuBuffer uniform;
	private static int uniformWidth;
	private static int uniformHeight;
	private static float uniformSize = -1F;
	private static float uniformStrength = -1F;
	private static float uniformNear = -1F;
	private static float uniformFar = -1F;
	private static float uniformDistance = -1F;

	private GlowtoneEdgeRenderer() {}

	public static boolean isEnabled() {
		return GlowtonePackSettings.highlightSource() == GlowtonePackSettings.Source.POST;
	}

	public static void render(RenderTarget mainTarget) {
		if (!isEnabled()) return;

		final GpuTextureView depth = mainTarget.getDepthTextureView();
		if (depth == null) return;

		final float strength = EdgeHighlightOption.strength() * GlowtonePackSettings.highlightStrength();
		if (strength <= 0F) return;

		final Minecraft minecraft = Minecraft.getInstance();
		final float far = Math.max(minecraft.options.getEffectiveRenderDistance() * 16F, MIN_FAR);

		updateUniform(mainTarget.width, mainTarget.height, strength, NEAR, far);
		if (uniform == null) return;

		final CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
		try (RenderPass pass = encoder.createRenderPass(
			() -> "Glowtone edge highlight",
			mainTarget.getColorTextureView(),
			Optional.empty(),
			null,
			OptionalDouble.empty()
		)) {
			pass.setPipeline(EDGE_PIPELINE);
			RenderSystem.bindDefaultUniforms(pass);
			pass.setUniform("EdgeConfig", uniform);
			pass.bindTexture("DepthSampler", depth, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
			pass.draw(3, 1, 0, 0);
		}
	}

	private static void updateUniform(int width, int height, float strength, float near, float far) {
		final float size = Math.max(
			GlowtonePackSettings.highlightSize() * (height / REFERENCE_HEIGHT), 1F);
		final float distance = GlowtonePackSettings.highlightDistance();
		if (uniform != null
			&& width == uniformWidth
			&& height == uniformHeight
			&& size == uniformSize
			&& strength == uniformStrength
			&& near == uniformNear
			&& far == uniformFar
			&& distance == uniformDistance
		) {
			return;
		}

		free();
		uniformWidth = width;
		uniformHeight = height;
		uniformSize = size;
		uniformStrength = strength;
		uniformNear = near;
		uniformFar = far;
		uniformDistance = distance;

		try (MemoryStack stack = MemoryStack.stackPush()) {
			final ByteBuffer data = Std140Builder.onStack(stack, UNIFORM_SIZE)
				.putVec2(1F / width, 1F / height)
				.putFloat(size)
				.putFloat(strength)
				.putFloat(near)
				.putFloat(far)
				.putFloat(distance)
				.putFloat(THRESHOLD)
				.putFloat(reversedDepth() ? 1F : 0F)
				.get();
			uniform = RenderSystem.getDevice().createBuffer(() -> "Glowtone edge highlight", GpuBuffer.USAGE_UNIFORM, data);
		}
	}

	private static boolean reversedDepth() {
		final CompareOp test = DepthStencilState.DEFAULT.depthTest();
		return test == CompareOp.GREATER_THAN || test == CompareOp.GREATER_THAN_OR_EQUAL;
	}

	public static void free() {
		if (uniform != null) uniform.close();
		uniform = null;
		uniformWidth = 0;
		uniformHeight = 0;
		uniformSize = -1F;
		uniformStrength = -1F;
		uniformNear = -1F;
		uniformFar = -1F;
		uniformDistance = -1F;
	}
}
