/*
 * Copyright 2026 FrozenBlock
 * This file is part of Glowtone.
 *
 * This program is free software; you can modify it under
 * the terms of version 1 of the FrozenBlock Modding Oasis License
 * as published by FrozenBlock Modding Oasis.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * FrozenBlock Modding Oasis License for more details.
 *
 * You should have received a copy of the FrozenBlock Modding Oasis License
 * along with this program; if not, see <https://github.com/FrozenBlock/Licenses>.
 */

package net.frozenblock.glowtone.mixin.client.bloom;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.frozenblock.glowtone.bloom.GlowtoneBloom;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@ClientOnly
@Mixin(ItemFeatureRenderer.class)
public class ItemFeatureRendererLightMixin {

	@ModifyExpressionValue(
		method = "prepareMainSubmit",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/feature/ItemFeatureRenderer$Submit;lightCoords()I"
		),
		require = 0
	)
	private int glowtone$dropInheritedEmissiveMarker(int lightCoords) {
		return GlowtoneBloom.unmark(lightCoords);
	}
}
