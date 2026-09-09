package net.frozenblock.glowtone.mixin.client.sodium.vertex;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.impl.CompactChunkVertex;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

@Pseudo
@ClientOnly
@Mixin(CompactChunkVertex.class)
public interface CompactChunkVertexAccessor {
	@Accessor("VERTEX_FORMAT")
	static VertexFormat glowtone$vertexFormat() {
		throw new AssertionError();
	}

	@Mutable
	@Accessor("VERTEX_FORMAT")
	static void glowtone$setVertexFormat(VertexFormat format) {
		throw new AssertionError();
	}
}
