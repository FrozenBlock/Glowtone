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

package net.frozenblock.glowtone.mixin.client.block;

import net.frozenblock.glowtone.light.impl.BlockLightPropertiesAttachment;
import net.frozenblock.glowtone.light.data.block.BlockLightProperties;
import net.frozenblock.glowtone.data.BlockMaterial;
import net.frozenblock.glowtone.material.impl.BlockMaterialAttachment;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@ClientOnly
@Mixin(Block.class)
public abstract class BlockMixin implements BlockLightPropertiesAttachment, BlockMaterialAttachment {

	@Unique
	private volatile BlockLightProperties.Baked glowtone$lightProperties = BlockLightProperties.EMPTY;

	@Unique
	private volatile BlockMaterial.Baked glowtone$material = BlockMaterial.EMPTY;

	@Unique
	@Override
	public BlockLightProperties.Baked glowtone$getLightProperties() {
		return this.glowtone$lightProperties;
	}

	@Unique
	@Override
	public void glowtone$setLightProperties(BlockLightProperties.Baked properties) {
		this.glowtone$lightProperties = properties;
	}

	@Unique
	@Override
	public BlockMaterial.Baked glowtone$getMaterial() {
		return this.glowtone$material;
	}

	@Unique
	@Override
	public void glowtone$setMaterial(BlockMaterial.Baked material) {
		this.glowtone$material = material;
	}
}
