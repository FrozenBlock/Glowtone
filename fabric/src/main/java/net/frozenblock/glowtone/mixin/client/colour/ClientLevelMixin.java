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

import net.frozenblock.glowtone.light.color.GlowtoneEmitterColors;
import net.frozenblock.glowtone.light.color.GlowtoneTransmittance;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {
	@Inject(method = "setBlocksDirty", at = @At("HEAD"))
	private void glowtone$onColourChanged(
			BlockPos pos, BlockState oldState, BlockState newState, CallbackInfo ci
	) {
		if (GlowtoneEmitterColors.rgbFor(oldState) == GlowtoneEmitterColors.rgbFor(newState)
				&& GlowtoneTransmittance.filterFor(oldState) == GlowtoneTransmittance.filterFor(newState)) {
			return;
		}

		int sectionX = SectionPos.blockToSectionCoord(pos.getX());
		int sectionY = SectionPos.blockToSectionCoord(pos.getY());
		int sectionZ = SectionPos.blockToSectionCoord(pos.getZ());
		((ClientLevel) (Object) this).setSectionRangeDirty(
				sectionX - 1, sectionY - 1, sectionZ - 1,
				sectionX + 1, sectionY + 1, sectionZ + 1
		);
	}
}
