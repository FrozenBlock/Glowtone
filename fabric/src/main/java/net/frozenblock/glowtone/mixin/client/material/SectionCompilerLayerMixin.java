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

package net.frozenblock.glowtone.mixin.client.material;

import net.frozenblock.glowtone.material.BlockMaterials;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@ClientOnly
@Mixin(SectionCompiler.class)
public class SectionCompilerLayerMixin {

	@ModifyVariable(method = "getOrBeginLayer", at = @At("HEAD"), argsOnly = true)
	private ChunkSectionLayer glowtone$overrideLayer(ChunkSectionLayer layer) {
		// TODO: this is a new layer. translucents won't work properly, test them! (water especially)
		return BlockMaterials.layer(layer);
	}
}
