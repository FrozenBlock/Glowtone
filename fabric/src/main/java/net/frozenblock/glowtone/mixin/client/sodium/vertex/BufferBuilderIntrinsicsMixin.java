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

package net.frozenblock.glowtone.mixin.client.sodium.vertex;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@ClientOnly
@Mixin(value = BufferBuilder.class, priority = 1500)
public class BufferBuilderIntrinsicsMixin {
	@Shadow
	@Final
	private VertexFormat format;

	@Unique
	public boolean glowtone$canUseIntrinsics() {
		return this.format != DefaultVertexFormat.ENTITY && this.format != DefaultVertexFormat.BLOCK;
	}

	@Dynamic
	@ModifyExpressionValue(
		method = "putBakedQuad",
		at = @At(
			value = "FIELD",
			target = "Lcom/mojang/blaze3d/vertex/BufferBuilder;blockFormat:Z",
			opcode = Opcodes.GETFIELD
		)
	)
	private boolean glowtone$skipBlockBakedQuadIntrinsic(boolean original) {
		return false;
	}

	@Dynamic
	@ModifyExpressionValue(
		method = "putBakedQuad",
		at = @At(
			value = "FIELD",
			target = "Lcom/mojang/blaze3d/vertex/BufferBuilder;entityFormat:Z",
			opcode = Opcodes.GETFIELD
		)
	)
	private boolean glowtone$skipEntityBakedQuadIntrinsic(boolean original) {
		return false;
	}
}
