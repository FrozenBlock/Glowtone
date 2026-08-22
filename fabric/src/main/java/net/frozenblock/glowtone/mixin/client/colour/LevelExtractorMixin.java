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

import net.frozenblock.glowtone.render.GlowtoneChromaFold;
import net.frozenblock.glowtone.render.GlowtoneEntityLight;
import net.frozenblock.glowtone.render.GlowtoneChromaTinted;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelExtractor.class)
public abstract class LevelExtractorMixin {
	@Inject(method = "extract", at = @At("HEAD"))
	private void glowtone$resetTintScopes(
			DeltaTracker deltaTracker,
			Camera camera,
			float partialTick,
			CallbackInfo ci
	) {
		GlowtoneChromaFold.resetScopes();
	}

	@Inject(method = "extractEntity", at = @At("RETURN"))
	private void glowtone$resolveEntityTint(
			Entity entity,
			float partialTick,
			CallbackInfoReturnable<EntityRenderState> cir
	) {
		EntityRenderState state = cir.getReturnValue();
		state.lightCoords = GlowtoneEntityLight.smooth(
				state.x, state.y + state.eyeHeight * 0.5F, state.z, state.lightCoords
		);
		((GlowtoneChromaTinted) state).glowtone$setChromaTint(
				GlowtoneChromaFold.resolveEntity(state.x, state.y, state.z, state.eyeHeight, state.lightCoords)
		);
	}
}
