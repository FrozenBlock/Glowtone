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

import net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionManager;
import net.frozenblock.glowtone.render.GlowtoneSectionColorStore;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(value = RenderSectionManager.class, remap = false)
public class RenderSectionManagerMixin {
	@Inject(method = "onSectionRemoved", at = @At("HEAD"))
	private void glowtone$dropSectionColors(int x, int y, int z, CallbackInfo info) {
		GlowtoneSectionColorStore.remove(x, y, z);
	}

	@Inject(method = "destroy", at = @At("HEAD"))
	private void glowtone$dropAllSectionColors(CallbackInfo info) {
		GlowtoneSectionColorStore.clear();
	}
}
