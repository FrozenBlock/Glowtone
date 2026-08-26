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

package net.frozenblock.glowtone.light.color;

import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import net.frozenblock.lib.block.api.attachment.BlockAttachmentKey;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public final class GlowtoneEmitterColors {
	private static final BlockAttachmentKey<Integer> ATTACHMENT_KEY = BlockAttachmentKey.create(true, () -> "Color Emission");
	public static final int NO_COLOUR = -1;

	public static final int WHITE = 0xFFFFFF;

	public static int rgbFor(BlockState state) {
		return state.getBlock().frozenLib$getAttachedOrDefault(ATTACHMENT_KEY, NO_COLOUR);
	}

	public static int rgbForOrWhite(BlockState state) {
		return state.getBlock().frozenLib$getAttachedOrDefault(ATTACHMENT_KEY, WHITE);
	}
