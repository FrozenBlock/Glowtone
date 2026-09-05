package net.frozenblock.glowtone.light.occlusion.impl;

import net.mehvahdjukaar.candlelight.api.ClientOnly;

@ClientOnly
public interface BlockStateAmbientOcclusionCache {

	default void glowtone$setHasAmbientOcclusion(boolean hasAmbientOcclusion) {
		throw new AssertionError();
	}

	default  boolean glowtone$hasAmbientOcclusion() {
		throw new AssertionError();
	}
}
