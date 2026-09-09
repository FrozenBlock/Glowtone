package net.frozenblock.glowtone.mixin.client.vertex;

import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@ClientOnly
@Mixin(LevelRenderer.class)
public interface LevelRendererAccessor {
	@Accessor("sectionRenderDispatcher")
	void glowtone$setSectionRenderDispatcher(@Nullable SectionRenderDispatcher dispatcher);
}
