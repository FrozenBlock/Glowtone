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

package net.frozenblock.glowtone.render.vertex;

import net.frozenblock.glowtone.bloom.EmissiveShaderPatcher;
import net.frozenblock.glowtone.config.GlowtoneConfig;
import net.frozenblock.glowtone.material.MaterialShaderPatcher;
import net.frozenblock.glowtone.platform.GlowtonePlatform;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import org.jspecify.annotations.Nullable;

@ClientOnly
public record GlowtoneVertexFeatures(boolean chroma, boolean edges, boolean flags) {
	private static final boolean SODIUM = GlowtonePlatform.INSTANCE.isModLoaded("sodium");
	private static volatile @Nullable GlowtoneVertexFeatures shaders;
	private static volatile @Nullable GlowtoneVertexFeatures applied;

	public static GlowtoneVertexFeatures current() {
		return new GlowtoneVertexFeatures(
			GlowtoneConfig.coloredLighting().enabled() || MaterialShaderPatcher.anyQuadOffset(),
			EmissiveShaderPatcher.edgeDataWanted(),
			SODIUM && (GlowtoneConfig.bloomEnabled() || MaterialShaderPatcher.any())
		);
	}

	public static GlowtoneVertexFeatures beginReload() {
		final GlowtoneVertexFeatures features = current();
		shaders = features;
		return features;
	}

	public static GlowtoneVertexFeatures shaders() {
		final GlowtoneVertexFeatures features = shaders;
		return features != null ? features : startup();
	}

	public static GlowtoneVertexFeatures applied() {
		final GlowtoneVertexFeatures features = applied;
		return features != null ? features : startup();
	}

	public static synchronized GlowtoneVertexFeatures startup() {
		GlowtoneVertexFeatures features = applied;
		if (features == null) {
			features = current();
			applied = features;
			if (shaders == null) shaders = features;
		}
		return features;
	}

	static void markApplied(GlowtoneVertexFeatures features) {
		applied = features;
	}
}
