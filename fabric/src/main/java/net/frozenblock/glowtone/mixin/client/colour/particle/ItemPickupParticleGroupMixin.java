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

package net.frozenblock.glowtone.mixin.client.colour.particle;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.frozenblock.glowtone.light.color.render.ChromaFold;
import net.frozenblock.glowtone.light.entity.SmoothEntityLightingHelper;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.particle.ItemPickupParticleGroup;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@ClientOnly
@Mixin(ItemPickupParticleGroup.State.class)
public class ItemPickupParticleGroupMixin {

	@WrapOperation(
		method = "submit",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;submit(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lnet/minecraft/client/renderer/state/level/CameraRenderState;DDDLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;)V"
		)
	)
	private void glowtone$retintPickupItem(
		EntityRenderDispatcher dispatcher,
		EntityRenderState renderState,
		CameraRenderState camera,
		double xOffset,
		double yOffset,
		double zOffset,
		PoseStack poseStack,
		SubmitNodeCollector submitNodeCollector,
		Operation<Void> original
	) {
		final double x = camera.pos.x() + xOffset;
		final double y = camera.pos.y() + yOffset;
		final double z = camera.pos.z() + zOffset;

		final int liveLight = SmoothEntityLightingHelper.worldLightAt(x, y, z, renderState.lightCoords);
		renderState.lightCoords = SmoothEntityLightingHelper.smooth(x, y, z, liveLight);

		final int tint = ChromaFold.resolveEntity(x, y, z, renderState.eyeHeight, liveLight);
		renderState.glowtone$setChromaTint(tint);

		original.call(dispatcher, renderState, camera, xOffset, yOffset, zOffset, poseStack, submitNodeCollector);
	}
}
