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

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.frozenblock.glowtone.light.color.render.ChromaFold;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@ClientOnly
@Mixin(SingleQuadParticle.class)
public abstract class SingleQuadParticleMixin extends Particle {

	protected SingleQuadParticleMixin(ClientLevel level, double x, double y, double z) {
		super(level, x, y, z);
	}

	@WrapOperation(
		method = "extractRotatedQuad(Lnet/minecraft/client/renderer/state/level/QuadParticleRenderState;Lorg/joml/Quaternionf;FFFF)V",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/state/level/QuadParticleRenderState;add(Lnet/minecraft/client/particle/SingleQuadParticle$Layer;FFFFFFFFFFFFII)V"
		)
	)
	private void glowtone$tintParticle(
		QuadParticleRenderState instance,
		SingleQuadParticle.Layer layer,
		float x,
		float y,
		float z,
		float xRot,
		float yRot,
		float zRot,
		float wRot,
		float scale,
		float u0,
		float u1,
		float v0,
		float v1,
		int color,
		int lightCoords,
		Operation<Void> original
	) {
		final Vec3 cameraPos = Minecraft.getInstance().gameRenderer.mainCamera().position();
		// FIXME: i don't support self-emission! colored lightColor tints me even when im glowing :(
		color = ChromaFold.tintParticleColor(color, lightCoords, x + cameraPos.x, y + cameraPos.y, z + cameraPos.z);
		original.call(instance, layer, x, y, z, xRot, yRot, zRot, wRot, scale, u0, u1, v0, v1, color, lightCoords);
	}
}
