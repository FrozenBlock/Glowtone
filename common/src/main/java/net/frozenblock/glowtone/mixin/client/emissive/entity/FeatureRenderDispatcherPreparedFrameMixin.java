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

package net.frozenblock.glowtone.mixin.client.emissive.entity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.feature.FeatureFrameContext;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.feature.phase.FeatureRenderPhase;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;

@ClientOnly
@Mixin(FeatureRenderDispatcher.PreparedFrame.class)
public abstract class FeatureRenderDispatcherPreparedFrameMixin {

	@Shadow
	protected abstract void executePhase(FeatureRenderPhase<?> phase, FeatureFrameContext context);

	@WrapOperation(
		method = "executeTranslucent",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher$PreparedFrame;executePhase(Lnet/minecraft/client/renderer/feature/phase/FeatureRenderPhase;Lnet/minecraft/client/renderer/feature/FeatureFrameContext;)V",
			ordinal = 0
		),
		slice = @Slice(
			from = @At(
				value = "FIELD",
				target = "Lnet/minecraft/client/renderer/SubmitNodeCollection;translucentModels:Lnet/minecraft/client/renderer/feature/phase/TranslucentFeatureRenderPhase;",
				opcode = Opcodes.GETFIELD
			)
		)
	)
	public void glowtone$submitEmissiveModelOverlays(
		FeatureRenderDispatcher.PreparedFrame instance, FeatureRenderPhase<?> phase, FeatureFrameContext context, Operation<Void> original,
		@Local(name = "collection") SubmitNodeCollection collection
	) {
		original.call(instance, phase, context);
		this.executePhase(collection.glowtone$emissiveModelOverlays(), context);
	}
}
