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

package net.frozenblock.glowtone.mixin.client.colour.entity;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.mojang.blaze3d.vertex.PoseStack;
import net.frozenblock.glowtone.light.color.render.ChromaFold;
import net.frozenblock.glowtone.light.entity.SmoothEntityLightingHelper;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@ClientOnly
@Mixin(ItemInHandRenderer.class)
public class ItemInHandRendererMixin {

	@Inject(method = "submitHandsWithItems", at = @At("HEAD"))
	private void glowtone$smoothAndTintedHandLight(
		float frameInterp, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, LocalPlayer player, int lightCoords, CallbackInfo info,
		@Local(argsOnly = true, ordinal = 0) LocalIntRef lightCoordsRef,
		@Share("glowtone$pushedTint") LocalBooleanRef pushedTint
	) {
		final Vec3 probe = player.getLightProbePosition(frameInterp);
		lightCoordsRef.set(SmoothEntityLightingHelper.smooth(probe.x, probe.y, probe.z, lightCoords));
		ChromaFold.pushSubmitTint(ChromaFold.resolveHand(probe.x, probe.y, probe.z, lightCoordsRef.get()));
		pushedTint.set(true);
	}

	@Inject(method = "submitHandsWithItems", at = @At("RETURN"))
	private void glowtone$popHandTint(
		CallbackInfo info,
		@Share("glowtone$pushedTint") LocalBooleanRef pushedTint
	) {
		if (pushedTint.get()) ChromaFold.popSubmitTint();
	}
}
