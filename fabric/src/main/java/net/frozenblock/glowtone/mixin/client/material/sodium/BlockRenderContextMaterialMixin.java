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

package net.frozenblock.glowtone.mixin.client.material.sodium;

import net.caffeinemc.mods.sodium.client.render.model.AbstractBlockRenderContext;
import net.frozenblock.glowtone.material.BlockMaterials;
import net.frozenblock.glowtone.material.MaterialCullHelper;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@ClientOnly
@Mixin(value = AbstractBlockRenderContext.class, remap = false)
public class BlockRenderContextMaterialMixin {
	@Shadow
	protected BlockState state;
	@Shadow
	protected BlockAndTintGetter level;
	@Shadow
	protected BlockPos pos;

	@Inject(method = "shouldDrawSide", at = @At("HEAD"), cancellable = true)
	private void glowtone$overrideFaceCulling(Direction facing, CallbackInfoReturnable<Boolean> info) {
		if (!BlockMaterials.anyFaceCulling() || this.state == null || this.level == null || this.pos == null) return;

		final Boolean override = MaterialCullHelper.overrideRenderFace(this.state, this.level.getBlockState(this.pos.relative(facing)));
		if (override != null) info.setReturnValue(override);
	}
}
