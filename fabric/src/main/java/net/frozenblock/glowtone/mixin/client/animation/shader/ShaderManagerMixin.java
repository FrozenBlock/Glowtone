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

package net.frozenblock.glowtone.mixin.client.animation.shader;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.shaders.ShaderType;
import net.frozenblock.glowtone.animation.AnimationShaderPatcher;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@ClientOnly
@Mixin(value = ShaderManager.class, priority = 994) // After Bloom
public class ShaderManagerMixin {

	@ModifyExpressionValue(
		method = "loadShader",
		at = @At(
			value = "INVOKE",
			target = "Ljava/lang/String;join(Ljava/lang/CharSequence;Ljava/lang/Iterable;)Ljava/lang/String;"
		)
	)
	private static String glowtone$patchShaders(
		String source,
		@Local(argsOnly = true) Identifier location,
		@Local(argsOnly = true) ShaderType type
	) {
		final Identifier condensedId = type.idConverter().fileToId(location);

		final String patchedWithAnimation = AnimationShaderPatcher.patchTerrainShader(condensedId, type, source);
		if (patchedWithAnimation != null) {
			//System.out.println(patchedWithAnimation);
			return patchedWithAnimation;
		}

		return source;
	}
}
