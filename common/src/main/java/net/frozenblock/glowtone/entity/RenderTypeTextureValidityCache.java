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

package net.frozenblock.glowtone.entity;

import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import net.frozenblock.glowtone.GlowtoneConstants;
import net.frozenblock.lib.resource.api.ResourceLoaderHelper;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.jetbrains.annotations.ApiStatus;

@ClientOnly
public final class RenderTypeTextureValidityCache {
	private static final Reloader RELOADER = new Reloader();
	private static final Object2BooleanOpenHashMap<Identifier> TEXTURE_VALIDITY_MAP = new Object2BooleanOpenHashMap<>();

	@ApiStatus.Internal
	public static void init() {
		ResourceLoaderHelper.registerReloadListener(PackType.CLIENT_RESOURCES, GlowtoneConstants.id("emissive_render_type_validity"), RELOADER);
	}

	public static boolean getOrComputeValidity(Identifier texture) {
		return TEXTURE_VALIDITY_MAP.computeIfAbsent(
			texture,
			id -> Minecraft.getInstance().getResourceManager().getResource(texture).isPresent()
		);
	}

	private static void clear() {
		TEXTURE_VALIDITY_MAP.clear();
	}

	private static class Reloader implements ResourceManagerReloadListener {
		@Override
		public void onResourceManagerReload(ResourceManager resourceManager) {
			clear();
		}
	}

	private RenderTypeTextureValidityCache() {}
}
