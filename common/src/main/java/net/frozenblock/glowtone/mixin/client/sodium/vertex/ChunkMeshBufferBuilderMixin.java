package net.frozenblock.glowtone.mixin.client.sodium.vertex;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.builder.ChunkMeshBufferBuilder;
import net.frozenblock.glowtone.light.color.render.ChromaBaker;
import net.frozenblock.glowtone.render.sodium.vertex.GTSodiumVertexFormat;
import net.frozenblock.glowtone.render.vertex.GlowtoneVertexLayout;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@ClientOnly
@Mixin(ChunkMeshBufferBuilder.class)
public class ChunkMeshBufferBuilderMixin {
	@Unique
	private @Nullable GlowtoneVertexLayout glowtone$layout;

	@ModifyExpressionValue(
		method = "<init>",
		at = @At(
			value = "INVOKE",
			target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/vertex/format/ChunkVertexType;getVertexFormat()Lcom/mojang/blaze3d/vertex/VertexFormat;"
		)
	)
	private VertexFormat glowtone$captureLayout(VertexFormat format) {
		this.glowtone$layout = GTSodiumVertexFormat.layoutOf(format);
		return format;
	}

	@Inject(method = "start", at = @At("HEAD"))
	private void glowtone$publishLayout(int sectionIndex, CallbackInfo info) {
		final GlowtoneVertexLayout layout = this.glowtone$layout;
		ChromaBaker.state().setSodiumLayout(layout != null ? layout : GTSodiumVertexFormat.currentLayout());
	}
}
