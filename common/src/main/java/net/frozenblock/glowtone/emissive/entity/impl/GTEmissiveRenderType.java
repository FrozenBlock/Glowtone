package net.frozenblock.glowtone.emissive.entity.impl;

import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.renderer.rendertype.RenderType;
import java.util.Optional;

@ClientOnly
public interface GTEmissiveRenderType {

	default boolean glowtone$isEmissive() {
		throw new AssertionError();
	}

	default void glowtone$markEmissive() {
		throw new AssertionError();
	}

	default boolean glowtone$isEmissiveResourceValid() {
		throw new AssertionError();
	}

	default Optional<RenderType> glowtone$emissiveRenderType() {
		throw new AssertionError();
	}
}
