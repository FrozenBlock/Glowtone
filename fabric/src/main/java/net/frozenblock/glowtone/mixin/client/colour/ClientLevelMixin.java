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

import net.frozenblock.glowtone.light.data.block.BlockLightProperties;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@ClientOnly
@Mixin(ClientLevel.class)
public class ClientLevelMixin {

	@Inject(method = "setBlocksDirty", at = @At("HEAD"))
	private void glowtone$onColourChanged(BlockPos pos, BlockState oldState, BlockState newState, CallbackInfo info) {
		if (BlockLightProperties.hasSameColorProperties(newState, oldState)) return;

		final int sectionX = SectionPos.blockToSectionCoord(pos.getX());
		final int sectionY = SectionPos.blockToSectionCoord(pos.getY());
		final int sectionZ = SectionPos.blockToSectionCoord(pos.getZ());
		ClientLevel.class.cast(this).setSectionRangeDirty(
			sectionX - 1, sectionY - 1, sectionZ - 1,
			sectionX + 1, sectionY + 1, sectionZ + 1
		);
	}
}
