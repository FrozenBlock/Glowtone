package net.frozenblock.glowtone.mixin.client.animation;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.frozenblock.glowtone.animation.GlowtoneAnimationChunkSectionLayers;
import net.frozenblock.glowtone.animation.GlowtoneAnimationPipelines;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ChunkSectionLayer.class)
public enum ChunkSectionLayerMixin {
	GLOWTONE_ANIMATION_FOLIAGE(GlowtoneAnimationPipelines.CUTOUT_TERRAIN_FOLIAGE, 4194304, false),
	GLOWTONE_ANIMATION_FIRE(GlowtoneAnimationPipelines.CUTOUT_TERRAIN_FIRE, 4194304, false);

	static {
		GlowtoneAnimationChunkSectionLayers.GLOWTONE_ANIMATION_FOLIAGE = ChunkSectionLayer.class.cast(GLOWTONE_ANIMATION_FOLIAGE);
		GlowtoneAnimationChunkSectionLayers.GLOWTONE_ANIMATION_FIRE = ChunkSectionLayer.class.cast(GLOWTONE_ANIMATION_FIRE);
	}

	@Shadow
	ChunkSectionLayerMixin(RenderPipeline pipeline, int bufferSize, boolean translucent) {
		throw new AssertionError();
	}
}
