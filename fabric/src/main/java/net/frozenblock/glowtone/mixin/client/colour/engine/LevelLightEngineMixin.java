/*
 * Copyright 2025-2026 FrozenBlock
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

package net.frozenblock.glowtone.mixin.client.colour.engine;

import net.frozenblock.glowtone.light.color.render.GlowtoneColorWindowCache;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@ClientOnly
@Mixin(LevelLightEngine.class)
public class LevelLightEngineMixin {

	@Shadow
	@Final
	protected LevelHeightAccessor levelHeightAccessor;

	@Unique
	private boolean glowtone$isClient() {
		return this.levelHeightAccessor instanceof ClientLevel;
	}

	@Inject(method = "checkBlock", at = @At("HEAD"))
	private void glowtone$invalidateOnBlockChange(BlockPos pos, CallbackInfo info) {
		if (!this.glowtone$isClient()) return;

		GlowtoneColorWindowCache.invalidateAround(
			SectionPos.blockToSectionCoord(pos.getX()),
			SectionPos.blockToSectionCoord(pos.getY()),
			SectionPos.blockToSectionCoord(pos.getZ())
		);
	}

	@Inject(method = "queueSectionData", at = @At("HEAD"))
	private void glowtone$invalidateOnLightData(
		LightLayer layer, SectionPos sectionPos, DataLayer data, CallbackInfo info
	) {
		if (layer != LightLayer.BLOCK || !this.glowtone$isClient()) return;

		GlowtoneColorWindowCache.invalidateAround(sectionPos.x(), sectionPos.y(), sectionPos.z());
	}
}
