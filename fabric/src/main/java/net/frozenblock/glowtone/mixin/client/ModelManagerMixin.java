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

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.frozenblock.glowtone.config.option.shade.ShadingOption;
import net.frozenblock.glowtone.emissive.particle.GlowtoneParticleEmissives;
import net.frozenblock.glowtone.render.sodium.sprite.GlowtoneSpecialSprites;
import net.frozenblock.glowtone.render.entity.GlowtoneEmissiveLayer;
import net.frozenblock.glowtone.config.GlowtoneConfig;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@ClientOnly
@Mixin(ModelManager.class)
public class ModelManagerMixin {

	@ModifyExpressionValue(
		method = "reload",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/server/packs/resources/PreparableReloadListener$SharedState;resourceManager()Lnet/minecraft/server/packs/resources/ResourceManager;"
		)
	)
	public ResourceManager glowtone$onReload(ResourceManager resourceManager) {
		ShadingOption.applyFlags(GlowtoneConfig.shading());

		GlowtoneEmissiveLayer.clearCache();
		GlowtoneSpecialSprites.clear();
		GlowtoneParticleEmissives.clear();

		return resourceManager;
	}
}
