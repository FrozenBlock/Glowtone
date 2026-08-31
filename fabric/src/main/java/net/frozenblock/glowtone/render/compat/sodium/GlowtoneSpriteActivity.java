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

package net.frozenblock.glowtone.render.compat.sodium;

import net.frozenblock.glowtone.render.GlowtoneEmissiveSprites;
import net.frozenblock.lib.platform.ModLoader;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

@ClientOnly
public final class GlowtoneSpriteActivity {
	private static final boolean ACTIVE = ModLoader.isModLoaded("sodium");

	public static void markEmissiveSpritesActive() {
		if (!ACTIVE || GlowtoneEmissiveSprites.isEmpty()) return;

		for (TextureAtlasSprite sprite : GlowtoneEmissiveSprites.all()) {
			Holder.mark(sprite);
		}
	}

	private static final class Holder {
		private static void mark(TextureAtlasSprite sprite) {
			net.caffeinemc.mods.sodium.api.texture.SpriteUtil.INSTANCE.markSpriteActive(sprite);
		}

		private Holder() {}
	}

	private GlowtoneSpriteActivity() {}
}
