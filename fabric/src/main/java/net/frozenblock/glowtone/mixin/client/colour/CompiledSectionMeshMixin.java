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

import net.frozenblock.glowtone.light.color.render.ChromaBaker;
import net.frozenblock.glowtone.light.color.render.impl.GlowtoneSectionColors;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.renderer.chunk.CompiledSectionMesh;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@ClientOnly
@Mixin(CompiledSectionMesh.class)
public abstract class CompiledSectionMeshMixin implements GlowtoneSectionColors {
	@Unique
	private short @Nullable [] glowtone$colors;

	@Unique
	private short @Nullable [] glowtone$skyHues;

	@Inject(method = "<init>", at = @At("RETURN"))
	private void glowtone$attachColors(CallbackInfo info) {
		this.glowtone$colors = ChromaBaker.takeSectionColors();
		this.glowtone$skyHues = ChromaBaker.takeSectionSkyColors();
	}

	@Unique
	@Override
	public short @Nullable [] glowtone$sectionColors() {
		return this.glowtone$colors;
	}

	@Unique
	@Override
	public short @Nullable [] glowtone$sectionSkyHues() {
		return this.glowtone$skyHues;
	}
}
