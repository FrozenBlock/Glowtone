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

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.block.BlockModelLighter;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Environment(EnvType.CLIENT)
@Mixin(BlockModelLighter.class)
public class BlockModelLighterAmbientMixin {
	@ModifyArg(
		method = "prepareQuadAmbientOcclusion(Lnet/minecraft/client/renderer/block/BlockAndTintGetter;"
			+ "Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;"
			+ "Lnet/minecraft/client/resources/model/geometry/BakedQuad;"
			+ "Lcom/mojang/blaze3d/vertex/QuadInstance;)V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/util/ARGB;gray(F)I"),
		index = 0,
		require = 0
	)
	private static float glowtone$clampCorner(float occlusion) {
		return Mth.clamp(occlusion, 0F, 1F);
	}
}
