package net.frozenblock.glowtone.mixin.client.foliage;

import net.frozenblock.glowtone.foliage.GlowtoneFoliageChunkSectionLayers;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Mixin(ChunkSectionLayerGroup.class)
public class ChunkSectionLayerGroupMixin {

	@Mutable
	@Shadow
	@Final
	private ChunkSectionLayer[] layers;

	@Inject(method = "<init>", at = @At("TAIL"))
	public void glowtone$addFoliageLayer(String layers, int par2, ChunkSectionLayer[] par3, CallbackInfo info) {
		if (!Arrays.stream(par3).anyMatch(layer -> layer == ChunkSectionLayer.CUTOUT)) return;
		GlowtoneFoliageChunkSectionLayers.init();
		final List<ChunkSectionLayer> newLayers = new ArrayList<>(List.of(this.layers));
		newLayers.add(GlowtoneFoliageChunkSectionLayers.GLOWTONE_FOLIAGE);
		this.layers = newLayers.toArray(new ChunkSectionLayer[0]);
	}
}
