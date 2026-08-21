package net.frozenblock.glowtone.mixin.client.animation;

import net.frozenblock.glowtone.animation.BlockAnimationType;
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
		if (!Arrays.stream(this.layers).anyMatch(layer -> layer == ChunkSectionLayer.CUTOUT)) return;

		final List<ChunkSectionLayer> newLayers = new ArrayList<>(List.of(this.layers));
		Arrays.stream(BlockAnimationType.values()).forEach(type -> {
			newLayers.add(type.solidLayer());
			newLayers.add(type.cutoutLayer());
			newLayers.add(type.translucentLayer());
		});
		this.layers = newLayers.toArray(new ChunkSectionLayer[0]);
	}
}
