package net.frozenblock.glowtone.mixin.client.sodium.vertex;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.Map;
import net.caffeinemc.mods.sodium.client.render.chunk.ShaderChunkRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexType;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@ClientOnly
@Mixin(ShaderChunkRenderer.class)
public class ShaderChunkRendererProgramsMixin {
	@Shadow
	@Final
	private static Map<TerrainRenderPass, RenderPipeline> programs;

	@Shadow
	@Final
	protected VertexFormat vertexFormat;

	@Inject(method = "<init>", at = @At("TAIL"))
	private void glowtone$dropStalePrograms(ChunkVertexType vertexType, CallbackInfo info) {
		for (RenderPipeline pipeline : programs.values()) {
			if (pipeline.getVertexFormatBinding(0) != this.vertexFormat) {
				programs.clear();
				return;
			}
		}
	}
}
