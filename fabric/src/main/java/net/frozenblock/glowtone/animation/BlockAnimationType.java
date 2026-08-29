package net.frozenblock.glowtone.animation;

import com.google.common.base.Suppliers;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.serialization.Codec;
import java.util.function.Supplier;
import net.frozenblock.glowtone.GlowtoneConstants;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.util.StringRepresentable;

@ClientOnly
public enum BlockAnimationType implements StringRepresentable {
	FOLIAGE("foliage"),
	FIRE("fire");
	public static final Codec<BlockAnimationType> CODEC = StringRepresentable.fromEnum(BlockAnimationType::values);
	private final String name;
	private final Supplier<RenderPipeline.Snippet> snippet = Suppliers.memoize(() -> RenderPipeline.builder(RenderPipelines.TERRAIN_SNIPPET)
		.withVertexShader(GlowtoneAnimationShaders.createTerrainAnimationShaderId(this.getSerializedName()))
		.buildSnippet()
	);
	private final Supplier<RenderPipeline> solidPipeline = Suppliers.memoize(() -> RenderPipelines.register(
		RenderPipeline.builder(this.snippet.get())
			.withLocation(GlowtoneConstants.id("pipeline/solid_terrain_" + this.getSerializedName()))
			.build()
	));
	private final Supplier<RenderPipeline> cutoutPipeline = Suppliers.memoize(() -> RenderPipelines.register(
		RenderPipeline.builder(this.snippet.get())
			.withLocation(GlowtoneConstants.id("pipeline/cutout_terrain_" + this.getSerializedName()))
			.withShaderDefine("ALPHA_CUTOUT", 0.5F)
			.build()
	));
	private final Supplier<RenderPipeline> translucentPipeline = Suppliers.memoize(() -> RenderPipelines.register(
		RenderPipeline.builder(this.snippet.get())
			.withLocation(GlowtoneConstants.id("pipeline/translucent_terrain_" + this.getSerializedName()))
			.withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
			.withShaderDefine("ALPHA_CUTOUT", 0.1F)
			.build()
	));
	private final Supplier<ChunkSectionLayer> solidLayer = Suppliers.memoize(() -> ChunkSectionLayer.valueOf("GLOWTONE_" + this.name() + "_SOLID"));
	private final Supplier<ChunkSectionLayer> cutoutLayer = Suppliers.memoize(() -> ChunkSectionLayer.valueOf("GLOWTONE_" + this.name() + "_CUTOUT"));
	private final Supplier<ChunkSectionLayer> translucentLayer = Suppliers.memoize(() -> ChunkSectionLayer.valueOf("GLOWTONE_" + this.name() + "_TRANSLUCENT"));

	BlockAnimationType(String name) {
		this.name = name;
	}

	public RenderPipeline solidPipeline() {
		return this.solidPipeline.get();
	}

	public RenderPipeline cutoutPipeline() {
		return this.cutoutPipeline.get();
	}

	public RenderPipeline translucentPipeline() {
		return this.translucentPipeline.get();
	}

	public ChunkSectionLayer getLayerByVanilla(ChunkSectionLayer layer) {
		return switch (layer) {
			case SOLID -> this.solidLayer.get();
			case CUTOUT -> this.cutoutLayer.get();
			case TRANSLUCENT -> this.translucentLayer.get();
			case null, default -> layer;
		};
	}

	public ChunkSectionLayer solidLayer() {
		return this.solidLayer.get();
	}

	public ChunkSectionLayer cutoutLayer() {
		return this.cutoutLayer.get();
	}

	public ChunkSectionLayer translucentLayer() {
		return this.translucentLayer.get();
	}

	@Override
	public String getSerializedName() {
		return this.name;
	}
}
