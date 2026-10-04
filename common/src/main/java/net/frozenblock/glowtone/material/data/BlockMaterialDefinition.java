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

package net.frozenblock.glowtone.material.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.resources.Identifier;
import java.util.Optional;

@ClientOnly
public record BlockMaterialDefinition(Optional<Identifier> parent, BlockMaterial material) {
	public static final Codec<BlockMaterialDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Identifier.CODEC.optionalFieldOf("parent").forGetter(BlockMaterialDefinition::parent),
		BlockMaterial.MAP_CODEC.forGetter(BlockMaterialDefinition::material)
	).apply(instance, BlockMaterialDefinition::new));
}
