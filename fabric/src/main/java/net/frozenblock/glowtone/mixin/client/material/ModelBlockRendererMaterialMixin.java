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

package net.frozenblock.glowtone.mixin.client.material;

import net.frozenblock.glowtone.light.BlockLightPropertiesRenderer;
import net.frozenblock.glowtone.material.render.BlockMaterialRenderer;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@ClientOnly
@Mixin(ModelBlockRenderer.class)
public class ModelBlockRendererMaterialMixin {

	@Inject(method = "tesselateBlock", at = @At("HEAD"))
	private void glowtone$beginBlockMaterial(
		BlockQuadOutput output,
		float red,
		float green,
		float blue,
		BlockAndTintGetter level,
		BlockPos pos,
		BlockState blockState,
		BlockStateModel model,
		long seed,
		CallbackInfo info
	) {
		BlockLightPropertiesRenderer.beginBlock(blockState);
		BlockMaterialRenderer.beginBlock(blockState);
	}

	@Inject(method = "tesselateBlock", at = @At("RETURN"))
	private void glowtone$endBlockMaterial(
		BlockQuadOutput output,
		float red,
		float green,
		float blue,
		BlockAndTintGetter level,
		BlockPos pos,
		BlockState blockState,
		BlockStateModel model,
		long seed,
		CallbackInfo info
	) {
		BlockLightPropertiesRenderer.endBlock();
		BlockMaterialRenderer.endBlock();
	}
}
