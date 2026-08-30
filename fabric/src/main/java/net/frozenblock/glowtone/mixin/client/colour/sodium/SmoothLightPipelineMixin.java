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

package net.frozenblock.glowtone.mixin.client.colour.sodium;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.caffeinemc.mods.sodium.client.model.light.data.LightDataAccess;
import net.caffeinemc.mods.sodium.client.model.light.data.QuadLightData;
import net.caffeinemc.mods.sodium.client.model.light.smooth.SmoothLightPipeline;
import net.caffeinemc.mods.sodium.client.model.quad.ModelQuadView;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.frozenblock.glowtone.render.sodium.GlowtoneSodiumOcclusion;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Environment(EnvType.CLIENT)
@Mixin(value = SmoothLightPipeline.class, remap = false)
public class SmoothLightPipelineMixin {
	@Shadow
	@Final
	private LightDataAccess lightCache;

	@Inject(method = "calculate", at = @At("HEAD"))
	private void glowtone$captureSelfOcclusion(
		ModelQuadView quad,
		BlockPos pos,
		QuadLightData out,
		Direction cullFace,
		Direction lightFace,
		boolean shade,
		boolean enhanced,
		CallbackInfo info
	) {
		GlowtoneSodiumOcclusion.beginQuad(this.lightCache.getLevel(), pos);
	}

	@Inject(method = "applyAmbientLighting", at = @At("HEAD"), require = 0)
	private void glowtone$scaleSelfOcclusion(
		float[] out, Direction face, boolean shade, CallbackInfo info
	) {
		GlowtoneSodiumOcclusion.scaleSelf(out);
	}

	@ModifyExpressionValue(
		method = "applyIrregularFace",
		at = @At(
			value = "INVOKE",
			target = "Lnet/caffeinemc/mods/sodium/client/model/light/smooth/AoFaceData;getBlendedShade([F)F"
		),
		require = 0
	)
	private float glowtone$scaleIrregularSelfOcclusion(float ambientOcclusion) {
		return GlowtoneSodiumOcclusion.scaleSelf(ambientOcclusion);
	}
}
