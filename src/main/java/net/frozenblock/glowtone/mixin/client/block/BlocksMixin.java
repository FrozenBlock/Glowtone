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

package net.frozenblock.glowtone.mixin.client.block;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.frozenblock.glowtone.GlowtoneConstants;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;
import java.util.function.Predicate;

@Environment(EnvType.CLIENT)
@Mixin(Blocks.class)
public class BlocksMixin {

	@WrapOperation(
		method = "<clinit>",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/block/state/BlockBehaviour$Properties;emissiveRendering(Ljava/util/function/Predicate;)Lnet/minecraft/world/level/block/state/BlockBehaviour$Properties;",
			ordinal = 0
		),
		slice = @Slice(
			from = @At(
				value = "FIELD",
				target = "Lnet/minecraft/references/BlockItemIds;MAGMA_BLOCK:Lnet/minecraft/references/BlockItemId;",
				opcode = Opcodes.GETSTATIC
			)
		)
	)
	private static BlockBehaviour.Properties glowtone$fixedMagmaRendering(
		BlockBehaviour.Properties instance, Predicate<BlockState> emissiveRendering, Operation<BlockBehaviour.Properties> original
	) {
		return original.call(instance, (Predicate<BlockState>) state -> !GlowtoneConstants.GLOWTONE_EMISSIVES);
	}

}
