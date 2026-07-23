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

package net.frozenblock.glowtone.mixin.client;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.frozenblock.glowtone.GlowtoneConstants;
import net.frozenblock.glowtone.emissive.EmissiveResolver;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ModelManager.class)
public class ModelManagerMixin {
	@Inject(method = "reload", at = @At("HEAD"))
	private void glowtone$detectPacks(
		PreparableReloadListener.PreparationBarrier preparationBarrier,
		ResourceManager resourceManager,
		ProfilerFiller preparationsProfiler,
		ProfilerFiller reloadProfiler,
		Executor backgroundExecutor,
		Executor gameExecutor,
		CallbackInfoReturnable<CompletableFuture<Void>> info
	) {
		EmissiveResolver.clearCaches();
		GlowtoneConstants.GLOWTONE_EMISSIVES = resourceManager.getResource(GlowtoneConstants.id("glowtone_emissives.marker.json")).isPresent();
		GlowtoneConstants.GLOWTONE_SHADING = resourceManager.getResource(GlowtoneConstants.id("glowtone_shading.marker.json")).isPresent();
	}
}
