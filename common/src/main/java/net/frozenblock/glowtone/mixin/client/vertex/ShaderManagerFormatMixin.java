package net.frozenblock.glowtone.mixin.client.vertex;

import net.frozenblock.glowtone.render.vertex.GlowtoneVertexFeatures;
import net.frozenblock.glowtone.render.vertex.GlowtoneVertexFormats;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.renderer.ShaderManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@ClientOnly
@Mixin(value = ShaderManager.class, priority = 990)
public class ShaderManagerFormatMixin {
	@Inject(
		method = "apply(Lnet/minecraft/client/renderer/ShaderManager$Configs;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V",
		at = @At("HEAD")
	)
	private void glowtone$rebuildVertexFormats(CallbackInfo info) {
		GlowtoneVertexFormats.apply(GlowtoneVertexFeatures.shaders());
	}
}
