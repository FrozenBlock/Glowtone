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

package net.frozenblock.glowtone.mixin.client.bloom;

import net.frozenblock.glowtone.config.pack.GlowtonePackSettingsLoader;
import net.frozenblock.glowtone.material.data.BlockMaterialOverrideLoader;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@ClientOnly
@Mixin(value = ShaderManager.class, priority = 990)
public class ShaderManagerPrepareMixin {

	@Inject(method = "prepare", at = @At("HEAD"))
	private void glowtone$loadShaderInputs(ResourceManager manager, ProfilerFiller profiler, CallbackInfoReturnable<Object> info) {
		GlowtonePackSettingsLoader.applyFrom(manager);
		BlockMaterialOverrideLoader.applyShaderSource(manager);
	}
}
