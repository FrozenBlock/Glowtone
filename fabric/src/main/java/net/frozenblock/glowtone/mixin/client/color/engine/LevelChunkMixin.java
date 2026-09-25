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

package net.frozenblock.glowtone.mixin.client.color.engine;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.frozenblock.glowtone.light.data.block.BlockLightProperties;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@ClientOnly
@Mixin(LevelChunk.class)
public class LevelChunkMixin {

	@Shadow
	@Final
	private Level level;

	@ModifyExpressionValue(
		method = "setBlockState",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/lighting/LightEngine;hasDifferentLightProperties(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;)Z"
		)
	)
	public boolean glowtone$countLightColorsAsDifferingProperties(
		boolean original,
		@Local(argsOnly = true) BlockState state,
		@Local(name = "oldState") BlockState oldState
	) {
		if (original || !(this.level instanceof ClientLevel)) return original;
		return !BlockLightProperties.hasSameColorProperties(state, oldState, true, true);
	}
}
