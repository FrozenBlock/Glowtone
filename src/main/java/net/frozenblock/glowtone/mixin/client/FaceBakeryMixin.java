/*
 * Copyright 2025 FrozenBlock
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

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.frozenblock.glowtone.GlowtoneConstants;
import net.frozenblock.glowtone.resources.metadata.EmissiveMetadataSection;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.Optional;

@Environment(EnvType.CLIENT)
@Mixin(FaceBakery.class)
public class FaceBakeryMixin {

	@Inject(method = "bakeQuad", at = @At("HEAD"))
	private static void glowtone$bakeWithEmission(
		CallbackInfoReturnable<BakedQuad> info,
		@Local(argsOnly = true) TextureAtlasSprite sprite,
		@Local(argsOnly = true) LocalBooleanRef shade,
		@Local(argsOnly = true) LocalIntRef lightEmission
	) {
		if (GlowtoneConstants.GLOWTONE_EMISSIVES) {
			final SpriteContents contents = sprite.contents();

			final Optional<EmissiveMetadataSection> optionalEmissiveMetadata = contents.getAdditionalMetadata(EmissiveMetadataSection.TYPE);
			if (optionalEmissiveMetadata.isPresent()) {
				EmissiveMetadataSection emissiveMetadata = optionalEmissiveMetadata.get();
				shade.set(emissiveMetadata.shade().orElse(shade.get()));
				lightEmission.set(emissiveMetadata.lightEmission());
			} else {
				lightEmission.set(contents.name().getPath().endsWith("_glowtone_emissive") ? 15 : lightEmission.get());
			}
		}

		if (GlowtoneConstants.GLOWTONE_SHADING) shade.set(shade.get() && lightEmission.get() != 15);
	}

}
