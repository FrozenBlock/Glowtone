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

package net.frozenblock.glowtone.emissive;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.frozenblock.glowtone.GlowtoneConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public final class EmissiveResolver {
	public static final String EMISSIVE_SUFFIX = "_glowtone_emissive";

	private static final Map<TextureAtlasSprite, Optional<TextureAtlasSprite>> OVERLAY_CACHE = new ConcurrentHashMap<>();

	public static void clearCaches() {
		OVERLAY_CACHE.clear();
	}

	@Nullable
	public static EmissiveMetadataSection metadataFor(TextureAtlasSprite sprite) {
		if (sprite.contents() instanceof GlowtoneEmissiveSprite emissive) {
			return emissive.glowtone$emissiveMetadata();
		}
		return null;
	}

	@Nullable
	public static TextureAtlasSprite overlayFor(TextureAtlasSprite base) {
		return OVERLAY_CACHE.computeIfAbsent(base, EmissiveResolver::resolveOverlay).orElse(null);
	}

	private static Optional<TextureAtlasSprite> resolveOverlay(TextureAtlasSprite base) {
		final ResourceLocation name = base.contents().name();
		if (name.getPath().endsWith(EMISSIVE_SUFFIX)) return Optional.empty();

		final Minecraft minecraft = Minecraft.getInstance();
		if (minecraft == null) return Optional.empty();
		final TextureAtlas atlas = minecraft.getModelManager().getAtlas(base.atlasLocation());
		if (atlas == null) return Optional.empty();

		final TextureAtlasSprite candidate = atlas.getSprite(name.withSuffix(EMISSIVE_SUFFIX));
		if (candidate == null || candidate.contents().name().equals(MissingTextureAtlasSprite.getLocation())) return Optional.empty();
		return Optional.of(candidate);
	}

	public static int lightEmissionFor(TextureAtlasSprite sprite) {
		final EmissiveMetadataSection metadata = metadataFor(sprite);
		if (metadata != null) return metadata.lightEmission();
		if (sprite.contents().name().getPath().endsWith(EMISSIVE_SUFFIX)) return 15;
		return 0;
	}

	public static boolean shadeFor(TextureAtlasSprite sprite, boolean defaultShade) {
		final EmissiveMetadataSection metadata = metadataFor(sprite);
		boolean shade = metadata != null ? metadata.shade().orElse(defaultShade) : defaultShade;
		if (GlowtoneConstants.GLOWTONE_SHADING && lightEmissionFor(sprite) == 15) shade = false;
		return shade;
	}

	private EmissiveResolver() {}
}
