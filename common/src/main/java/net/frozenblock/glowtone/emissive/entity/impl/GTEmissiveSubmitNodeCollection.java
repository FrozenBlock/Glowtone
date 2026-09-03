package net.frozenblock.glowtone.emissive.entity.impl;

import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.renderer.feature.phase.TranslucentFeatureRenderPhase;

@ClientOnly
public interface GTEmissiveSubmitNodeCollection {
	default TranslucentFeatureRenderPhase glowtone$emissiveModelOverlays() {
		throw new AssertionError();
	}
}
