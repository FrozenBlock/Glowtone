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

import com.llamalad7.mixinextras.sugar.Local;
import net.frozenblock.glowtone.light.BlockLightPropertiesRenderer;
import net.frozenblock.glowtone.material.BlockMaterials;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Hook the compiler, not ModelBlockRenderer since Indigo replaces it.
@ClientOnly
@Mixin(SectionCompiler.class)
public class SectionCompilerMaterialMixin {

	@Inject(
		method = "compile",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/block/ModelBlockRenderer;tesselateBlock(Lnet/minecraft/client/renderer/block/BlockQuadOutput;FFFLnet/minecraft/client/renderer/block/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/client/renderer/block/dispatch/BlockStateModel;J)V"
		)
	)
	private void glowtone$beginBlockMaterial(CallbackInfoReturnable<?> info, @Local BlockState blockState) {
		BlockLightPropertiesRenderer.beginBlock(blockState);
		BlockMaterials.beginBlock(blockState);
	}

	@Inject(
		method = "compile",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/block/ModelBlockRenderer;tesselateBlock(Lnet/minecraft/client/renderer/block/BlockQuadOutput;FFFLnet/minecraft/client/renderer/block/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/client/renderer/block/dispatch/BlockStateModel;J)V",
			shift = At.Shift.AFTER
		)
	)
	private void glowtone$endBlockMaterial(CallbackInfoReturnable<?> info) {
		BlockLightPropertiesRenderer.endBlock();
		BlockMaterials.endBlock();
	}
}
