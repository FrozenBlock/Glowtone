package net.frozenblock.glowtone.mixin.client.vertex;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.frozenblock.glowtone.render.vertex.GlowtoneVertexFormats;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@ClientOnly
@Mixin(RenderPipeline.Builder.class)
public class RenderPipelineBuilderMixin {
	@Inject(method = "build", at = @At("RETURN"))
	private void glowtone$trackPipeline(CallbackInfoReturnable<RenderPipeline> info) {
		GlowtoneVertexFormats.track(info.getReturnValue());
	}
}
