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

package net.frozenblock.glowtone.mixin.client.colour.shader;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.shaders.ShaderType;
import net.frozenblock.glowtone.light.color.ColorShaderPatcher;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@ClientOnly
@Mixin(value = ShaderManager.class, priority = 996)
public class ShaderManagerMixin {

	@ModifyExpressionValue(
		method = "loadShader",
		at = @At(
			value = "INVOKE",
			target = "Ljava/lang/String;join(Ljava/lang/CharSequence;Ljava/lang/Iterable;)Ljava/lang/String;"
		)
	)
	private static String glowtone$patchColourShaderTerrain(
		String source,
		@Local(argsOnly = true) Identifier location,
		@Local(argsOnly = true) ShaderType type
	) {
		final Identifier id = type.idConverter().fileToId(location);
		source = ColorShaderPatcher.patchTerrainShader(id, type, source);
		source = ColorShaderPatcher.patchEntityShader(id, type, source);
		source = ColorShaderPatcher.patchItemShader(id, type, source);
		source = ColorShaderPatcher.patchLeashShader(id, type, source);
		return source;
	}
}
