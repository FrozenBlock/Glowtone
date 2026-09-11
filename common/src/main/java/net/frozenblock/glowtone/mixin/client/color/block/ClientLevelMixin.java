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

package net.frozenblock.glowtone.mixin.client.color.block;

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
	private void glowtone$onColorChanged(BlockPos pos, BlockState oldState, BlockState newState, CallbackInfo info) {
		// Cancel if light & filter colors haven't changed
		if (BlockLightProperties.hasSameColorProperties(newState, oldState, true, true)) return;

		final int sectionX = SectionPos.blockToSectionCoord(pos.getX());
		final int sectionY = SectionPos.blockToSectionCoord(pos.getY());
		final int sectionZ = SectionPos.blockToSectionCoord(pos.getZ());

		final ClientLevel level = ClientLevel.class.cast(this);
		// TODO: this does literally absolutely nothing to fix sky light bruh
		/*
		runSkyLightUpdates: {
			// Check if sky light is enabled
			if (!level.dimensionType().hasSkyLight()) break runSkyLightUpdates;
			// Check if only the filter color has changed
			if (BlockLightProperties.hasSameColorProperties(newState, oldState, false, true)) break runSkyLightUpdates;

			final ChunkAccess chunk = level.getChunk(sectionX, sectionZ, ChunkStatus.FULL, false);
			if (chunk == null) break runSkyLightUpdates;

			// Find the lowest sky light source available, as a Section coordinate
			final int lowestSkyLightSectionY = SectionPos.blockToSectionCoord(
				chunk.getSkyLightSources()
					.getLowestSourceY(
						SectionPos.sectionRelative(pos.getX()),
						SectionPos.sectionRelative(pos.getZ())
					)
			);
			// Cancel if lowest sky light source is at the same or a higher Section
			if (lowestSkyLightSectionY >= sectionY - 1) break runSkyLightUpdates;

			// Set all Sections between the lowest sky light source and the lowest central to-be-updated Section as dirty
			for (int currentSectionY = lowestSkyLightSectionY; currentSectionY < sectionY - 1; currentSectionY++) {
				level.levelExtractor.setSectionDirty(sectionX, currentSectionY, sectionZ);
			}
		}
		 */

		// Set all Sections in a 3x3 area as dirty
		level.setSectionDirtyWithNeighbors(sectionX, sectionY, sectionZ);
	}
}
