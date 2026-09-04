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

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import net.frozenblock.glowtone.light.color.render.ChromaFold;
import net.frozenblock.glowtone.light.entity.SmoothEntityLightingHelper;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LightLayer;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@ClientOnly
@Mixin(EntityRenderer.class)
public class EntityRendererMixin<T extends Entity, S extends EntityRenderState> {

	@Unique
	private int glowtone$getOriginalSkyLightLevel(Entity entity, BlockPos blockPos) {
		return entity.level().getBrightness(LightLayer.SKY, blockPos);
	}

	@Unique
	protected int glowtone$getOriginalBlockLightLevel(Entity entity, BlockPos blockPos) {
		return entity.level().getBrightness(LightLayer.BLOCK, blockPos);
	}

	@Unique
	public final int glowtone$getOriginalSmoothPackedLightCoords(Entity entity, double x, double y, double z, float partialTickTime) {
		final BlockPos blockPos = BlockPos.containing(entity.getLightProbePosition(partialTickTime));
		final int packed = LightCoordsUtil.pack(this.glowtone$getOriginalSkyLightLevel(entity, blockPos), this.glowtone$getOriginalBlockLightLevel(entity, blockPos));
		return SmoothEntityLightingHelper.smooth(x, y, z, packed);
	}

	@Unique
	public final int glowtone$getOriginalSmoothPackedLightCoords(Entity entity, EntityRenderState renderState, float partialTickTime) {
		return this.glowtone$getOriginalSmoothPackedLightCoords(entity, renderState.x, renderState.y + renderState.eyeHeight * 0.5F, renderState.z, partialTickTime);
	}

	@ModifyReturnValue(
		method = "createRenderState(Lnet/minecraft/world/entity/Entity;F)Lnet/minecraft/client/renderer/entity/state/EntityRenderState;",
		at = @At("RETURN")
	)
	private EntityRenderState glowtone$resolveEntityTint(
		EntityRenderState original,
		Entity entity, float partialTicks
	) {
		original.lightCoords = SmoothEntityLightingHelper.smooth(original.x, original.y + original.eyeHeight * 0.5F, original.z, original.lightCoords);

		final int blockLightTint = ChromaFold.resolveEntityBlockTint(
			original.x,
			original.y,
			original.z,
			original.eyeHeight,
			original.lightCoords
		);
		original.glowtone$setBlockLightTint(blockLightTint);
		if (original.leashStates != null) {
			for (EntityRenderState.LeashState leashState : original.leashStates) {
				leashState.glowtone$setBlockLightTintA(blockLightTint);
			}
		}
		return original;
	}

	@ModifyExpressionValue(
		method = "extractRenderState",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/entity/Leashable;getLeashHolder()Lnet/minecraft/world/entity/Entity;",
			ordinal = 1
		),
		slice = @Slice(
			from = @At(
				value = "INVOKE",
				target = "Lnet/minecraft/world/entity/Leashable;getLeashHolder()Lnet/minecraft/world/entity/Entity;",
				ordinal = 0
			)
		)
	)
	private Entity glowtone$captureRopeEndBlockTint(
		Entity original,
		T entity, final S state, final float partialTicks,
		@Share("glowtone$leashStateEndBlockTint") LocalIntRef leashStateEndBlockTint
	) {
		final double holderX = Mth.lerp(partialTicks, original.xOld, original.getX());
		final double holderY = Mth.lerp(partialTicks, original.yOld, original.getY());
		final double holderZ = Mth.lerp(partialTicks, original.zOld, original.getZ());
		final float holderEyeHeight = original.getEyeHeight();
		final int smoothHolderLightCoords = this.glowtone$getOriginalSmoothPackedLightCoords(entity, holderX, holderY + holderEyeHeight * 0.5F, holderZ, partialTicks);
		leashStateEndBlockTint.set(ChromaFold.resolveEntityBlockTint(holderX, holderY, holderZ, holderEyeHeight, smoothHolderLightCoords));

		return original;
	}

	@Inject(
		method = "extractRenderState",
		at = @At(
			value = "FIELD",
			target = "Lnet/minecraft/client/renderer/entity/state/EntityRenderState$LeashState;endBlockLight:I",
			opcode = Opcodes.PUTFIELD
		),
		slice = @Slice(
			from = @At(
				value = "INVOKE",
				target = "Lnet/minecraft/world/entity/Leashable;getLeashHolder()Lnet/minecraft/world/entity/Entity;",
				ordinal = 0
			)
		)
	)
	private void glowtone$setRopeEndBlockTint(
		T entity, S state, float partialTicks, CallbackInfo info,
		@Local(name = "leashState") EntityRenderState.LeashState leashState,
		@Share("glowtone$leashStateEndBlockTint") LocalIntRef leashStateEndBlockTint
	) {
		leashState.glowtone$setBlockLightTintB(leashStateEndBlockTint.get());
	}
}
