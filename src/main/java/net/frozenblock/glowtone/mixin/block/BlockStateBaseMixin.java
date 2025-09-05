/*
 * Copyright 2025 FrozenBlock
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

package net.frozenblock.glowtone.mixin.block;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.frozenblock.glowtone.GlowtoneConstants;
import net.minecraft.world.level.block.RedStoneOreBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BlockBehaviour.BlockStateBase.class)
public class BlockStateBaseMixin {

	@ModifyReturnValue(method = "getLightEmission", at = @At("RETURN"))
	public int glowtone$newRedstoneOreLightEmission(int lightEmission) {
		if (lightEmission == 0) return lightEmission;
		if (BlockBehaviour.BlockStateBase.class.cast(this).getBlock() instanceof RedStoneOreBlock && GlowtoneConstants.GLOWTONE_EMISSIVES) return 0;
		return lightEmission;
	}

}
