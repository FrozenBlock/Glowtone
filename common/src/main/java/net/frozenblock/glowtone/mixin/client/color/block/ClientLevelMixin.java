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

import net.frozenblock.glowtone.light.SkyTintColumns;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@ClientOnly
@Mixin(ClientLevel.class)
public class ClientLevelMixin {

	@Inject(method = "onChunkLoaded", at = @At("TAIL"))
	private void glowtone$indexSkyTint(ChunkPos pos, CallbackInfo info) {
		final LevelChunk chunk = ClientLevel.class.cast(this).getChunkSource().getChunk(pos.x(), pos.z(), ChunkStatus.FULL, false);
		if (chunk != null) SkyTintColumns.onChunkLoaded(chunk);
	}

	@Inject(method = "unload", at = @At("HEAD"))
	private void glowtone$dropSkyTint(LevelChunk levelChunk, CallbackInfo info) {
		SkyTintColumns.onChunkUnloaded(levelChunk.getPos());
	}
}
