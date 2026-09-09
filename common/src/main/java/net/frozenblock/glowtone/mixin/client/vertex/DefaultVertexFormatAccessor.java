package net.frozenblock.glowtone.mixin.client.vertex;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@ClientOnly
@Mixin(DefaultVertexFormat.class)
public interface DefaultVertexFormatAccessor {
	@Mutable
	@Accessor("BLOCK")
	static void glowtone$setBlock(VertexFormat format) {
		throw new AssertionError();
	}

	@Mutable
	@Accessor("ENTITY")
	static void glowtone$setEntity(VertexFormat format) {
		throw new AssertionError();
	}
}
