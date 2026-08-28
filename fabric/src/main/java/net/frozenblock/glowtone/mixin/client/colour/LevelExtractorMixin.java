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

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.frozenblock.glowtone.light.color.render.ChromaFold;
import net.frozenblock.glowtone.light.entity.SmoothEntityLightingHelper;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelExtractor.class)
public class LevelExtractorMixin {

	@Inject(method = "extract", at = @At("HEAD"))
	private void glowtone$resetTintScopes(DeltaTracker deltaTracker, Camera camera, float deltaPartialTick, CallbackInfo info) {
		ChromaFold.resetScopes();
	}

	@ModifyReturnValue(method = "extractEntity", at = @At("RETURN"))
	private EntityRenderState glowtone$resolveEntityTint(
		EntityRenderState original,
		Entity entity, float partialTickTime
	) {
		original.lightCoords = SmoothEntityLightingHelper.smooth(original.x, original.y + original.eyeHeight * 0.5F, original.z, original.lightCoords);
		original.glowtone$setChromaTint(ChromaFold.resolveEntity(original.x, original.y, original.z, original.eyeHeight, original.lightCoords));
		return original;
	}
}
