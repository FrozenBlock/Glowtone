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

package net.frozenblock.glowtone.render.rendertype;

import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import java.util.function.Function;

@ClientOnly
public final class GTRenderTypes {
	public static final String ENTITY_EMISSIVE_OVERLAY_NAME = "glowtone_entity_emissive_overlay";
	private static final Function<Identifier, RenderType> ENTITY_EMISSIVE_OVERLAY = Util.memoize(
		texture -> RenderType.create(
			ENTITY_EMISSIVE_OVERLAY_NAME,
			RenderSetup.builder(RenderPipelines.EYES)
				.withTexture("Sampler0", texture)
				.sortOnUpload()
				.createRenderSetup()
		).glowtone$markEmissive()
	);

	public static RenderType entityEmissiveOverlay(Identifier texture) {
		return ENTITY_EMISSIVE_OVERLAY.apply(texture);
	}

	private GTRenderTypes() {}
}
