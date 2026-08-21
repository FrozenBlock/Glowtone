package net.frozenblock.glowtone.mixin.client.foliage;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.frozenblock.glowtone.foliage.GlowtoneFoliageChunkSectionLayers;
import net.frozenblock.glowtone.foliage.GlowtoneFoliagePipelines;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ChunkSectionLayer.class)
public enum ChunkSectionLayerMixin {
	GLOWTONE_FOLIAGE(GlowtoneFoliagePipelines.CUTOUT_TERRAIN, 4194304, false);

	static {
		GlowtoneFoliageChunkSectionLayers.GLOWTONE_FOLIAGE = (ChunkSectionLayer) (Object) GLOWTONE_FOLIAGE;
	}

	@Shadow
	ChunkSectionLayerMixin(RenderPipeline pipeline, int bufferSize, boolean translucent) {
		throw new AssertionError();
	}
}
