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

package net.frozenblock.glowtone.resources.metadata;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.frozenblock.glowtone.animation.BlockAnimationType;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.server.packs.metadata.MetadataSectionType;

@ClientOnly
public record AnimationMetadataSection(BlockAnimationType animationType) {
	public static final Codec<AnimationMetadataSection> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		BlockAnimationType.CODEC.fieldOf("animation").forGetter(AnimationMetadataSection::animationType)
	).apply(instance, AnimationMetadataSection::new));
	public static final MetadataSectionType<AnimationMetadataSection> TYPE = new MetadataSectionType<>("glowtone_block_animation", CODEC);
}
