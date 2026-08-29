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

package net.frozenblock.glowtone.mixin.client.shader;

import com.google.common.collect.ImmutableMap;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.shaders.ShaderType;
<<<<<<< HEAD:fabric/src/main/java/net/frozenblock/glowtone/mixin/client/shader/ShaderManagerMixin.java
import net.frozenblock.glowtone.animation.GlowtoneAnimationShaders;
=======
>>>>>>> 26.2:fabric/src/main/java/net/frozenblock/glowtone/mixin/client/bloom/ShaderManagerMixin.java
import net.frozenblock.glowtone.bloom.GlowtoneEmissiveShaders;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import java.util.Map;

@ClientOnly
@Mixin(value = ShaderManager.class, priority = 995)
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
		@Local(argsOnly = true) ShaderType type,
		@Local(argsOnly = true) ImmutableMap.Builder<ShaderManager.ShaderSourceKey, String> output
	) {
		final Identifier condensedId = type.idConverter().fileToId(location);

		final Map<Identifier, String> animationShaders = GlowtoneAnimationShaders.createNewTerrainShaders(condensedId, type, source);
		if (animationShaders != null) {
			animationShaders.forEach((animationId, animationSource) -> {
				output.put(
					new ShaderManager.ShaderSourceKey(animationId, type),
					GlowtoneEmissiveShaders.patch(animationId, type, animationSource)
				);
			});
		}

		return GlowtoneEmissiveShaders.patch(condensedId, type, source);
	}
}
