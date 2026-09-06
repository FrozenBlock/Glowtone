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

package net.frozenblock.glowtone.mixin.client.material.blaze3d;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.frozenblock.glowtone.GlowtoneConstants;
import net.frozenblock.glowtone.material.MaterialSamplers;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;

@ClientOnly
@Mixin(targets = "com.mojang.blaze3d.opengl.GlProgram")
public class GlProgramMixin {

	@WrapWithCondition(
		method = "setupBindGroupLayouts",
		at = @At(
			value = "INVOKE",
			target = "Lorg/slf4j/Logger;warn(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V",
			ordinal = 0
		),
		slice = @Slice(
			from = @At(
				value = "FIELD",
				target = "Lcom/mojang/blaze3d/opengl/GlProgram;uniformsByName:Ljava/util/Map;",
				opcode = Opcodes.GETFIELD,
				ordinal = 0
			)
		)
	)
	private boolean glowtone$muteGlowtoneMaterialTexSamplerWarnings(Logger instance, String s, Object a, Object b) {
		// It seems that the warning about a shader not making use of a sampler only happens with OpenGl.
		return GlowtoneConstants.LOG_GLOWTONE_MATERIAL_TEX_WARNINGS || !b.toString().startsWith(MaterialSamplers.PREFIX);
	}
}
