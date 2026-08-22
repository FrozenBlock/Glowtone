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

package net.frozenblock.glowtone.mixin.client.colour;

import com.mojang.blaze3d.vertex.PoseStack;
import net.frozenblock.glowtone.render.GlowtoneChromaFold;
import net.frozenblock.glowtone.render.GlowtoneChromaTinted;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.world.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ItemFeatureRenderer.Submit.class)
public abstract class ItemFeatureRendererSubmitMixin implements GlowtoneChromaTinted {
	@Unique
	private int glowtone$chromaTint;

	@Override
	public int glowtone$chromaTint() {
		return this.glowtone$chromaTint;
	}

	@Override
	public void glowtone$setChromaTint(int tint) {
		this.glowtone$chromaTint = tint;
	}

	@Inject(method = "<init>", at = @At("RETURN"))
	private void glowtone$captureChromaTint(
			PoseStack.Pose pose,
			ItemDisplayContext displayContext,
			int lightCoords,
			int overlayCoords,
			int outlineColor,
			int[] tintLayers,
			List<BakedQuad> quads,
			ItemStackRenderState.FoilType foilType,
			CallbackInfo ci
	) {
		this.glowtone$chromaTint = GlowtoneChromaFold.currentTint();
	}
}
