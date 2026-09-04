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

package net.frozenblock.glowtone.mixin.client.colour.fabric;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.AltModelBlockRendererImpl;
import net.frozenblock.glowtone.light.color.render.ChromaBaker;
import net.frozenblock.glowtone.light.color.render.ChromaBlender;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@ClientOnly
@Mixin(AltModelBlockRendererImpl.class)
public class AltModelBlockRendererImplMixin {

	@ModifyReturnValue(method = "transform", at = @At("RETURN"), require = 0)
	private boolean glowtone$pinFlatQuadColor(boolean original, MutableQuadView quad) {
		if (!original) return original;

		if (!ChromaBaker.buildingSection()) {
			for (int vertex = 0; vertex < 4; vertex++) {
				// FIXME
				//quad.color(vertex, ChromaFold.tintMovingBlockQuadColor(quad.color(vertex)));
			}
		}

		if (!ChromaBlender.isEnabled()) return original;
		if (ChromaBaker.smoothLightingEnabled() && quad.ambientOcclusion().orElse(true)) return original;

		float x = 0F;
		float y = 0F;
		float z = 0F;
		for (int vertex = 0; vertex < 4; vertex++) {
			x += quad.x(vertex);
			y += quad.y(vertex);
			z += quad.z(vertex);
		}
		x *= 0.25F;
		y *= 0.25F;
		z *= 0.25F;

		final Direction face = quad.lightFace();
		if (face != null) {
			x += face.getStepX() * 0.5F;
			y += face.getStepY() * 0.5F;
			z += face.getStepZ() * 0.5F;
		}

		final ChromaBaker.SectionState state = ChromaBaker.state();
		state.beginFlatQuadLocal(x, y, z);
		return original;
	}
}
