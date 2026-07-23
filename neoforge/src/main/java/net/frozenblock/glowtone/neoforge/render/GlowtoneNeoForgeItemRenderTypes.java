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

package net.frozenblock.glowtone.neoforge.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import org.jetbrains.annotations.Nullable;

public final class GlowtoneNeoForgeItemRenderTypes {
	@Nullable
	public static ShaderInstance itemEmissiveShader;

	public static final RenderType ITEM_EMISSIVE = RenderType.create(
		"glowtone_item_emissive",
		DefaultVertexFormat.NEW_ENTITY,
		VertexFormat.Mode.QUADS,
		1536,
		false,
		false,
		RenderType.CompositeState.builder()
			.setShaderState(new RenderStateShard.ShaderStateShard(() -> itemEmissiveShader))
			.setTextureState(RenderStateShard.BLOCK_SHEET)
			.setWriteMaskState(RenderStateShard.COLOR_DEPTH_WRITE)
			.setLayeringState(RenderStateShard.POLYGON_OFFSET_LAYERING)
			.createCompositeState(false)
	);

	private GlowtoneNeoForgeItemRenderTypes() {}
}
