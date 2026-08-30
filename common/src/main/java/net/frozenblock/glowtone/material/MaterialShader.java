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

package net.frozenblock.glowtone.material;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.resources.Identifier;
import java.util.Map;
import java.util.Optional;

@ClientOnly
public record MaterialShader(
	Optional<Identifier> fragment,
	Optional<Identifier> vertex,
	Map<String, Identifier> textures,
	Map<String, String> constants
) {
	public static final String RESOURCE_PACK_DIRECTORY = "glowtone/shaders/material";
	public static final String FILE_SUFFIX = ".glsl";

	public static final Codec<MaterialShader> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Identifier.CODEC.optionalFieldOf("fragment").forGetter(MaterialShader::fragment),
		Identifier.CODEC.optionalFieldOf("vertex").forGetter(MaterialShader::vertex),
		Codec.unboundedMap(Codec.STRING, Identifier.CODEC).optionalFieldOf("textures", Map.of()).forGetter(MaterialShader::textures),
		Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("constants", Map.of()).forGetter(MaterialShader::constants)
	).apply(instance, MaterialShader::new));

	public boolean isEmpty() {
		return this.fragment.isEmpty() && this.vertex.isEmpty();
	}
}
