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

package net.frozenblock.glowtone.mixin.client.colour;

import com.mojang.blaze3d.vertex.VertexSorting;
import net.frozenblock.glowtone.render.GlowtoneChromaBake;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.core.SectionPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SectionCompiler.class)
public abstract class SectionCompilerMixin {
	@Inject(method = "compile", at = @At("HEAD"))
	private void glowtone$floodSection(
			SectionPos sectionPos,
			RenderSectionRegion region,
			VertexSorting vertexSorting,
			SectionBufferBuilderPack bufferPack,
			CallbackInfoReturnable<SectionCompiler.Results> cir
	) {
		GlowtoneChromaBake.beginSection(sectionPos, region);
	}

	@Inject(method = "compile", at = @At("RETURN"))
	private void glowtone$releaseSection(
			SectionPos sectionPos,
			RenderSectionRegion region,
			VertexSorting vertexSorting,
			SectionBufferBuilderPack bufferPack,
			CallbackInfoReturnable<SectionCompiler.Results> cir
	) {
		GlowtoneChromaBake.endSection();
	}
}
