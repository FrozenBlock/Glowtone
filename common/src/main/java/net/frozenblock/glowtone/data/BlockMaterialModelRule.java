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

package net.frozenblock.glowtone.data;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.resources.Identifier;

@ClientOnly
public record BlockMaterialModelRule(List<Identifier> models, Identifier material, Map<String, String> parameters, List<String> target) {
	public static final String RESOURCE_PACK_DIRECTORY = "glowtone/block_material_models";

	private static final Codec<List<Identifier>> MODELS = Codec.either(Identifier.CODEC, Identifier.CODEC.listOf())
		.xmap(
			either -> either.map(List::of, list -> list),
			list -> list.size() == 1 ? Either.left(list.getFirst()) : Either.right(list)
		);

	public static final Codec<BlockMaterialModelRule> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		MODELS.fieldOf("model").forGetter(BlockMaterialModelRule::models),
		Identifier.CODEC.fieldOf("material").forGetter(BlockMaterialModelRule::material),
		Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("parameters", Map.of()).forGetter(BlockMaterialModelRule::parameters),
		Codec.STRING.listOf().optionalFieldOf("target", List.of()).forGetter(BlockMaterialModelRule::target)
	).apply(instance, BlockMaterialModelRule::new));

	public boolean matches(Set<Identifier> chain) {
		for (Identifier model : this.models) {
			if (chain.contains(model)) return true;
		}

		return false;
	}

	public BlockMaterialOverrideDispatcher.Assignment assignment() {
		return new BlockMaterialOverrideDispatcher.Assignment(this.material, this.parameters, this.target);
	}

	public String sortOrder() {
		return this.models + " " + this.material;
	}
}
