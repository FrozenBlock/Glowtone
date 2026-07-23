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

package net.frozenblock.glowtone.neoforge.mixin.compat;

import net.frozenblock.glowtone.GlowtoneConstants;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Pseudo
@Mixin(targets = "dev.ryanhcode.sable.render.dynamic_shade.SableDynamicDirectionalShadingPreProcessor", remap = false)
public class SableDynamicShadingPreProcessorMixin {
	@ModifyArg(
		method = "modify",
		at = @At(
			value = "INVOKE",
			target = "Lio/github/ocelot/glslprocessor/api/GlslParser;parseExpression(Ljava/lang/String;)Lio/github/ocelot/glslprocessor/api/node/GlslNode;"
		),
		require = 0
	)
	private String glowtone$exemptEmissiveFromDynamicShading(String expression) {
		if (expression == null || !expression.contains("block_brightness")) {
			return expression;
		}

		if (!GlowtoneConstants.GLOWTONE_SHADING) {
			return expression;
		}

		return expression.replace(
			"SableEnableNormalLighting)",
			"SableEnableNormalLighting * (1.0 - step(240.0, float(UV2.x)) * step(240.0, float(UV2.y))))"
		);
	}
}
