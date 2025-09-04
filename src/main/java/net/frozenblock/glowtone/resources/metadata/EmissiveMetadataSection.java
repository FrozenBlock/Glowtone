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

package net.frozenblock.glowtone.resources.metadata;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.util.ExtraCodecs;
import java.util.Optional;

@Environment(EnvType.CLIENT)
public record EmissiveMetadataSection(int lightEmission, Optional<Boolean> shade) {
	public static final Codec<EmissiveMetadataSection> CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
				ExtraCodecs.intRange(0, 15).optionalFieldOf("light_emission", 15).forGetter(EmissiveMetadataSection::lightEmission),
				Codec.BOOL.optionalFieldOf("shade").forGetter(EmissiveMetadataSection::shade)
			)
			.apply(instance, EmissiveMetadataSection::new)
	);
	public static final MetadataSectionType<EmissiveMetadataSection> TYPE = new MetadataSectionType<>("frozenlib_emissive", CODEC);
}
