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

import com.mojang.blaze3d.vertex.PoseStack;
import net.frozenblock.glowtone.render.GlowtoneChromaFold;
import net.frozenblock.glowtone.render.GlowtoneEntityLight;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {
	@ModifyVariable(method = "submitHandsWithItems", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private int glowtone$smoothHandLight(int lightCoords, float partialTick, PoseStack poseStack,
			SubmitNodeCollector collector, LocalPlayer player) {
		Vec3 probe = player.getLightProbePosition(partialTick);
		return GlowtoneEntityLight.smooth(probe.x, probe.y, probe.z, lightCoords);
	}

	@Inject(method = "submitHandsWithItems", at = @At("HEAD"))
	private void glowtone$pushHandTint(
			float partialTick,
			PoseStack poseStack,
			SubmitNodeCollector collector,
			LocalPlayer player,
			int lightCoords,
			CallbackInfo ci
	) {
		Vec3 probe = player.getLightProbePosition(partialTick);
		int smoothed = GlowtoneEntityLight.smooth(probe.x, probe.y, probe.z, lightCoords);
		GlowtoneChromaFold.pushTint(GlowtoneChromaFold.resolveHand(probe.x, probe.y, probe.z, smoothed));
	}

	@Inject(method = "submitHandsWithItems", at = @At("RETURN"))
	private void glowtone$popHandTint(
			float partialTick,
			PoseStack poseStack,
			SubmitNodeCollector collector,
			LocalPlayer player,
			int lightCoords,
			CallbackInfo ci
	) {
		GlowtoneChromaFold.popTint();
	}
}
