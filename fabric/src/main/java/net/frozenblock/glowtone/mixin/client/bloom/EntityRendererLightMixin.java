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
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import net.frozenblock.glowtone.GlowtoneConstants;
import net.frozenblock.glowtone.bloom.GlowtoneBloom;
import net.frozenblock.glowtone.bloom.GlowtoneBloomRenderer;
import net.frozenblock.glowtone.light.compat.lambdynamiclights.GlowtoneDynamicLights;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LightLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@ClientOnly
@Mixin(EntityRenderer.class)
public class EntityRendererLightMixin {

	@ModifyExpressionValue(
		method = "getPackedLightCoords",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;getBlockLightLevel(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/BlockPos;)I"
		)
	)
	private int glowtone$markAsSelfLit(
		int blockLight,
		@Local(argsOnly = true) Entity entity,
		@Local BlockPos blockPos,
		@Share("glowtone$selfLit") LocalBooleanRef selfLit
	) {
		final int dynamic = Math.max(GlowtoneDynamicLights.get().luminanceOf(entity), GlowtoneDynamicLights.get().dynamicLightLevelAt(blockPos));
		final int ambient = Math.max(entity.level().getBrightness(LightLayer.BLOCK, blockPos), dynamic);

		selfLit.set(entity.isOnFire() || blockLight > ambient);
		return blockLight;
	}

	@ModifyReturnValue(method = "getPackedLightCoords", at = @At("RETURN"))
	private int glowtone$markSelfLitEntity(
		int lightCoords,
		@Share("glowtone$selfLit") LocalBooleanRef selfLit
	) {
		if (!selfLit.get() || !GlowtoneConstants.GLOWTONE_EMISSIVES || !GlowtoneBloomRenderer.isEnabled()) return lightCoords;
		return GlowtoneBloom.mark(lightCoords);
	}
}
