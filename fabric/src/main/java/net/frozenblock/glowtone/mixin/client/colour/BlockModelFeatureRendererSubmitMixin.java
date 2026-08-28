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
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.feature.BlockModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(BlockModelFeatureRenderer.Submit.class)
public class BlockModelFeatureRendererSubmitMixin implements GlowtoneChromaTinted {
	@Unique
	private int glowtone$chromaTint;

	@Unique
	@Override
	public int glowtone$chromaTint() {
		return this.glowtone$chromaTint;
	}

	@Unique
	@Override
	public void glowtone$setChromaTint(int tint) {
		this.glowtone$chromaTint = tint;
	}

	@Inject(method = "<init>", at = @At("RETURN"))
	private void glowtone$captureChromaTint(
		PoseStack.Pose pose,
		RenderType renderType,
		List<BlockStateModelPart> modelParts,
		int[] tintLayers,
		int lightCoords,
		int overlayCoords,
		int tintColor,
		PoseStack.Pose sheetedDecalPose,
		CallbackInfo info
	) {
		this.glowtone$chromaTint = GlowtoneChromaFold.currentTint();
	}
}
