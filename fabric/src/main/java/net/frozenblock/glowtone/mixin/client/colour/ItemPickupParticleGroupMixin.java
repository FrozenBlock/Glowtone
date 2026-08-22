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

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.frozenblock.glowtone.render.GlowtoneChromaFold;
import net.frozenblock.glowtone.render.GlowtoneChromaTinted;
import net.frozenblock.glowtone.render.GlowtoneEntityLight;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin(targets = "net.minecraft.client.particle.ItemPickupParticleGroup$State")
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
		EntityRenderState state,
		CameraRenderState camera,
		double offsetX,
		double offsetY,
		double offsetZ,
		PoseStack poseStack,
		SubmitNodeCollector collector,
		Operation<Void> original
	) {
		final double x = camera.pos.x() + offsetX;
		final double y = camera.pos.y() + offsetY;
		final double z = camera.pos.z() + offsetZ;

		final int liveLight = GlowtoneEntityLight.worldLightAt(x, y, z, state.lightCoords);
		state.lightCoords = GlowtoneEntityLight.smooth(x, y, z, liveLight);

		final int tint = GlowtoneChromaFold.resolveEntity(x, y, z, state.eyeHeight, liveLight);
		((GlowtoneChromaTinted) state).glowtone$setChromaTint(tint);

		original.call(dispatcher, state, camera, offsetX, offsetY, offsetZ, poseStack, collector);
	}
}
