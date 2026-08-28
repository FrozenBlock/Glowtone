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

package net.frozenblock.glowtone.mixin.client.colour.sodium;

import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderCache;
import net.caffeinemc.mods.sodium.client.world.cloned.ChunkRenderContext;
import net.frozenblock.glowtone.render.sodium.GlowtoneSodiumFlood;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.Inject;

@Pseudo
@Mixin(value = BlockRenderCache.class, remap = false)
public class BlockRenderCacheMixin {
	@Inject(method = "init", at = @At("TAIL"))
	private void glowtone$bindFlood(ChunkRenderContext context, CallbackInfo info) {
		GlowtoneSodiumFlood.begin(context);
	}

	@Inject(method = "cleanup", at = @At("HEAD"))
	private void glowtone$releaseFlood(CallbackInfo info) {
		GlowtoneSodiumFlood.end();
	}
}
