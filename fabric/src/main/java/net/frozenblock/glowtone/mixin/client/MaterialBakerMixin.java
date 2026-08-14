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

package net.frozenblock.glowtone.mixin.client;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.client.resources.model.sprite.MaterialBaker;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MaterialBaker.class)
public class MaterialBakerMixin {

	@WrapWithCondition(
		method = "lambda$logMissingTextures$0",
		at = @At(
			value = "INVOKE",
			target = "Lorg/slf4j/Logger;warn(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V"
		)
	)
	private static boolean glowtone$ignoreEmissiveLoggingA(Logger instance, String string, Object object1, Object object2) {
		if (object2 instanceof String object2String) return !object2String.endsWith("_glowtone_emissive");
		return true;
	}

	@WrapWithCondition(
		method = "lambda$logMissingTextures$2",
		at = @At(
			value = "INVOKE",
			target = "Lorg/slf4j/Logger;warn(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V"
		)
	)
	private static boolean glowtone$ignoreEmissiveLoggingB(Logger instance, String string, Object object1, Object object2) {
		if (object2 instanceof String object2String) return !object2String.endsWith("_glowtone_emissive");
		return true;
	}
}
