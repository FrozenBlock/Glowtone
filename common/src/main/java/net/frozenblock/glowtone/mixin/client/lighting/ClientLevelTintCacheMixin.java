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

package net.frozenblock.glowtone.mixin.client.lighting;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.frozenblock.glowtone.lighting.WorldLightCurves;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.color.block.BlockTintCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.ColorResolver;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@ClientOnly
@Mixin(ClientLevel.class)
public class ClientLevelTintCacheMixin {

	@Shadow
	@Final
	private Object2ObjectArrayMap<ColorResolver, BlockTintCache> tintCaches;

	@Inject(method = "<init>", at = @At("TAIL"))
	private void glowtone$addLightCurveTintCache(CallbackInfo info) {
		final BlockTintCache[] caches = WorldLightCurves.newCaches(ClientLevel.class.cast(this));
		for (int index = 0; index < caches.length; index++) {
			this.tintCaches.put(WorldLightCurves.RESOLVERS[index], caches[index]);
		}
	}
}
