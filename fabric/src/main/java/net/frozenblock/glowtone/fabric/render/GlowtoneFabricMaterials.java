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

package net.frozenblock.glowtone.fabric.render;

import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.util.TriState;

public final class GlowtoneFabricMaterials {
	public static final RenderMaterial STANDARD;
	public static final RenderMaterial EMISSIVE_SHADED;
	public static final RenderMaterial EMISSIVE_UNSHADED;

	static {
		final Renderer renderer = RendererAccess.INSTANCE.getRenderer();
		STANDARD = renderer.materialFinder().find();
		EMISSIVE_SHADED = renderer.materialFinder().emissive(true).find();
		EMISSIVE_UNSHADED = renderer.materialFinder()
			.emissive(true)
			.disableDiffuse(true)
			.ambientOcclusion(TriState.FALSE)
			.find();
	}

	private GlowtoneFabricMaterials() {}
}
