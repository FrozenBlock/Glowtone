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

package net.frozenblock.glowtone.particle;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.frozenblock.glowtone.GlowtoneConstants;
import net.frozenblock.glowtone.resources.metadata.EmissiveMetadataSection;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.lighting.LightEngine;
import org.jspecify.annotations.Nullable;

@ClientOnly
public final class GlowtoneParticleEmissives {
	public static final Resolved NONE = new Resolved(null, null, 0, true, 0, true);

	private static final Map<TextureAtlasSprite, Resolved> CACHE = new ConcurrentHashMap<>();

	public record Resolved(
		TextureAtlasSprite sprite,
		SingleQuadParticle.Layer layer,
		int baseEmission,
		boolean baseShade,
		int emissiveEmission,
		boolean emissiveShade
	) {
		public boolean present() {
			return this.sprite != null;
		}
	}

	public static Resolved forSprite(TextureAtlasSprite base) {
		return CACHE.computeIfAbsent(base, GlowtoneParticleEmissives::resolve);
	}

	public static void clear() {
		CACHE.clear();
	}

	private static Resolved resolve(TextureAtlasSprite base) {
		final int baseEmission = emissionOf(base);
		final boolean baseShade = shadeOf(base, baseEmission);

		final TextureAtlasSprite candidate = variantOf(base);
		if (candidate == null) return new Resolved(null, null, baseEmission, baseShade, 0, true);

		final int emissiveEmission = emissionOf(candidate);
		return new Resolved(
			candidate,
			SingleQuadParticle.Layer.bySprite(candidate),
			baseEmission,
			baseShade,
			emissiveEmission,
			shadeOf(candidate, emissiveEmission)
		);
	}

	@Nullable
	private static TextureAtlasSprite variantOf(TextureAtlasSprite base) {
		final Identifier candidateLocation = GlowtoneConstants.withEmissiveSuffix(base.contents().name());
		final TextureAtlasSprite candidate = Minecraft.getInstance().getAtlasManager()
			.get(new SpriteId(base.atlasLocation(), candidateLocation));
		if (candidate == null || candidate.contents().name().equals(MissingTextureAtlasSprite.getLocation())) return null;

		return candidate;
	}

	private static int emissionOf(TextureAtlasSprite sprite) {
		final Optional<EmissiveMetadataSection> metadata =
			sprite.contents().getAdditionalMetadata(EmissiveMetadataSection.TYPE);
		if (metadata.isPresent()) return metadata.get().lightEmission();

		return sprite.contents().name().getPath().endsWith(GlowtoneConstants.EMISSIVE_SUFFIX) ? LightEngine.MAX_LEVEL : 0;
	}

	private static boolean shadeOf(TextureAtlasSprite sprite, int emission) {
		final boolean shade = sprite.contents().getAdditionalMetadata(EmissiveMetadataSection.TYPE)
			.flatMap(EmissiveMetadataSection::shade)
			.orElse(true);
		if (GlowtoneConstants.GLOWTONE_SHADING) return shade && emission != LightEngine.MAX_LEVEL;

		return shade;
	}

	private GlowtoneParticleEmissives() {}
}
