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

package net.frozenblock.glowtone.mixin.client.emissive.animation;

import net.frozenblock.glowtone.GlowtoneConstants;
import net.frozenblock.glowtone.render.GlowtoneEmissiveSprites;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@ClientOnly
@Mixin(TextureAtlasSprite.class)
public class TextureAtlasSpriteMixin {

	@Inject(method = "<init>", at = @At("TAIL"))
	private void glowtone$registerEmissiveSprite(
		Identifier atlasLocation,
		SpriteContents contents,
		int atlasWidth,
		int atlasHeight,
		int x,
		int y,
		int mipLevel,
		CallbackInfo info
	) {
		if (!contents.isAnimated() || !contents.name().getPath().endsWith(GlowtoneConstants.EMISSIVE_SUFFIX)) return;

		GlowtoneEmissiveSprites.register((TextureAtlasSprite) (Object) this);
	}
}
