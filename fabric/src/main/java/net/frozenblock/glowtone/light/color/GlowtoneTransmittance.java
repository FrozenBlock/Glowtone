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
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public final class GlowtoneTransmittance {
	private static final BlockAttachmentKey<Integer> ATTACHMENT_KEY = BlockAttachmentKey.create(true, () -> "Color Transmittance");
	public static final int FULLY_TRANSMISSIVE = 0xFFF;

	public static final int MAX_CHANNEL = 0xF;

	private static final GlowtoneColorTable TABLE = new GlowtoneColorTable(FULLY_TRANSMISSIVE, GlowtoneTransmittance::putBuiltIns);

	private GlowtoneTransmittance() {
		throw new UnsupportedOperationException("GlowtoneTransmittance is a static holder.");
	}

	public static void attachAndClearColors() {
		BuiltInRegistries.BLOCK.forEach(block -> {
			final int color = TABLE.get(block);
			if (color != FULLY_TRANSMISSIVE) {
				block.frozenLib$setAttached(ATTACHMENT_KEY, color);
			} else {
				block.frozenLib$removeAttached(ATTACHMENT_KEY);
			}
		});
		TABLE.clear();
	}

	public static int filterFor(BlockState state) {
		return state.getBlock().frozenLib$getAttachedOrDefault(ATTACHMENT_KEY, FULLY_TRANSMISSIVE);
	}

	public static int red(int packed) {
		return (packed >> 8) & MAX_CHANNEL;
	}

	public static int green(int packed) {
		return (packed >> 4) & MAX_CHANNEL;
	}

	public static int blue(int packed) {
		return packed & MAX_CHANNEL;
	}

	public static boolean isNeutral(int packed) {
		return packed == FULLY_TRANSMISSIVE;
	}

	public static int definedCount() {
		return TABLE.size();
	}

	static void applyOverlay(@Nullable Reference2IntMap<Block> overlay, boolean replacesBuiltIns) {
		TABLE.overlay(overlay, replacesBuiltIns);
	}

	private static void putBuiltIns(Reference2IntMap<Block> table) {
		for (DyeColor dye : DyeColor.values()) {
			final int filter = filterForDye(dye);
			GlowtoneColorTable.put(table, filter, Blocks.STAINED_GLASS.pick(dye), Blocks.STAINED_GLASS_PANE.pick(dye));
		}

		GlowtoneColorTable.put(table, FULLY_TRANSMISSIVE, Blocks.GLASS, Blocks.GLASS_PANE);

		GlowtoneColorTable.put(table, 0x111, Blocks.TINTED_GLASS);

		GlowtoneColorTable.put(table, 0xACF, Blocks.WATER);
		GlowtoneColorTable.put(table, 0xDEF, Blocks.ICE, Blocks.PACKED_ICE, Blocks.FROSTED_ICE);
		GlowtoneColorTable.put(table, 0xCDF, Blocks.BLUE_ICE);

		GlowtoneColorTable.put(table, 0xFC6, Blocks.HONEY_BLOCK);
		GlowtoneColorTable.put(table, 0xBFB, Blocks.SLIME_BLOCK);
		GlowtoneColorTable.put(table, 0xC7F, Blocks.NETHER_PORTAL);
	}

	private static int filterForDye(DyeColor dye) {
		return switch (dye) {
			case WHITE -> 0xEEE;
			case ORANGE -> 0xF80;
			case MAGENTA -> 0xF4C;
			case LIGHT_BLUE -> 0x6CF;
			case YELLOW -> 0xFE2;
			case LIME -> 0x9F2;
			case PINK -> 0xF8A;
			case GRAY -> 0x555;
			case LIGHT_GRAY -> 0xAAA;
			case CYAN -> 0x2AB;
			case PURPLE -> 0x82D;
			case BLUE -> 0x24E;
			case BROWN -> 0x852;
			case GREEN -> 0x4A2;
			case RED -> 0xF11;
			case BLACK -> 0x111;
		};
	}
}
