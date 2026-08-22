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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public final class GlowtoneEmitterColors {
	public static final int NO_COLOUR = -1;

	public static final int WHITE = 0xFFFFFF;

	private static final GlowtoneColorTable TABLE = new GlowtoneColorTable(NO_COLOUR, GlowtoneEmitterColors::putBuiltIns);

	private GlowtoneEmitterColors() {
		throw new UnsupportedOperationException("GlowtoneEmitterColors is a static holder.");
	}

	public static int rgbFor(BlockState state) {
		return TABLE.get(state.getBlock());
	}

	public static int rgbForOrWhite(BlockState state) {
		final int rgb = TABLE.get(state.getBlock());
		return rgb == NO_COLOUR ? WHITE : rgb;
	}

	public static int definedCount() {
		return TABLE.size();
	}

	static void applyOverlay(@Nullable Reference2IntMap<Block> overlay, boolean replacesBuiltIns) {
		TABLE.overlay(overlay, replacesBuiltIns);
	}

	private static void putBuiltIns(Reference2IntMap<Block> table) {
		GlowtoneColorTable.put(table, 0xFFB347, Blocks.TORCH, Blocks.WALL_TORCH);
		GlowtoneColorTable.put(table, 0xFFC46B, Blocks.LANTERN);
		GlowtoneColorTable.put(table, 0xFFA246, Blocks.CAMPFIRE);
		GlowtoneColorTable.put(table, 0xFFAE3C, Blocks.JACK_O_LANTERN);
		GlowtoneColorTable.put(table, 0xFF8C2A, Blocks.FIRE);

		GlowtoneColorTable.put(table, 0x7FE6D2, Blocks.COPPER_TORCH, Blocks.COPPER_WALL_TORCH);
		GlowtoneColorTable.putAll(table, 0x7FE6D2, Blocks.COPPER_LANTERN.asList());
		GlowtoneColorTable.putAll(table, 0xFFD9A8, Blocks.COPPER_BULB.asList());

		GlowtoneColorTable.put(table, 0x3FC7D6,
			Blocks.SOUL_TORCH, Blocks.SOUL_WALL_TORCH, Blocks.SOUL_LANTERN,
			Blocks.SOUL_FIRE, Blocks.SOUL_CAMPFIRE
		);

		GlowtoneColorTable.put(table, 0xFF3020, Blocks.REDSTONE_TORCH, Blocks.REDSTONE_WALL_TORCH, Blocks.REDSTONE_BLOCK);
		GlowtoneColorTable.put(table, 0xFF4A38, Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE);
		GlowtoneColorTable.put(table, 0xFFB35C, Blocks.REDSTONE_LAMP);

		GlowtoneColorTable.put(table, 0xFFD98A, Blocks.GLOWSTONE);
		GlowtoneColorTable.put(table, 0xFFAF5E, Blocks.SHROOMLIGHT);
		GlowtoneColorTable.put(table, 0xCFE8FF, Blocks.SEA_LANTERN, Blocks.BEACON);
		GlowtoneColorTable.put(table, 0xA8EFE4, Blocks.CONDUIT);

		GlowtoneColorTable.put(table, 0xFF7A1A, Blocks.LAVA, Blocks.LAVA_CAULDRON);
		GlowtoneColorTable.put(table, 0xFF6A14, Blocks.MAGMA_BLOCK);

		GlowtoneColorTable.put(table, 0xFFF3C0, Blocks.OCHRE_FROGLIGHT);
		GlowtoneColorTable.put(table, 0xCFF5C0, Blocks.VERDANT_FROGLIGHT);
		GlowtoneColorTable.put(table, 0xF8D8EC, Blocks.PEARLESCENT_FROGLIGHT);

		GlowtoneColorTable.put(table, 0xF2E8FF, Blocks.END_ROD);
		GlowtoneColorTable.put(table, 0xC7B6FF, Blocks.END_PORTAL, Blocks.END_GATEWAY);
		GlowtoneColorTable.put(table, 0xBCE8C4, Blocks.END_PORTAL_FRAME);
		GlowtoneColorTable.put(table, 0xC8A2FF, Blocks.ENCHANTING_TABLE);
		GlowtoneColorTable.put(table, 0xB24BFF, Blocks.NETHER_PORTAL);
		GlowtoneColorTable.put(table, 0x9B4DFF, Blocks.CRYING_OBSIDIAN);
		GlowtoneColorTable.put(table, 0xB14DFF, Blocks.RESPAWN_ANCHOR, Blocks.DRAGON_EGG);
		GlowtoneColorTable.put(table, 0xC9A0FF,
				Blocks.AMETHYST_CLUSTER, Blocks.LARGE_AMETHYST_BUD,
				Blocks.MEDIUM_AMETHYST_BUD, Blocks.SMALL_AMETHYST_BUD
		);

		GlowtoneColorTable.put(table, 0xB6E39A, Blocks.GLOW_LICHEN);
		GlowtoneColorTable.put(table, 0xFFCF63, Blocks.CAVE_VINES, Blocks.CAVE_VINES_PLANT);
		GlowtoneColorTable.put(table, 0xA8D66A, Blocks.SEA_PICKLE);
		GlowtoneColorTable.put(table, 0xE8C99A, Blocks.BROWN_MUSHROOM);
		GlowtoneColorTable.put(table, 0xE8E06A, Blocks.FIREFLY_BUSH);

		GlowtoneColorTable.put(table, 0x3DD9C8, Blocks.SCULK_CATALYST, Blocks.SCULK_SHRIEKER);
		GlowtoneColorTable.put(table, 0x35C8E0, Blocks.SCULK_SENSOR, Blocks.CALIBRATED_SCULK_SENSOR);

		GlowtoneColorTable.put(table, 0xFFA040, Blocks.FURNACE, Blocks.SMOKER, Blocks.BLAST_FURNACE);
		GlowtoneColorTable.put(table, 0xFFCF9A, Blocks.BREWING_STAND);

		GlowtoneColorTable.put(table, 0xFFB25E, Blocks.TRIAL_SPAWNER);
		GlowtoneColorTable.put(table, 0x58E0FF, Blocks.VAULT);

		GlowtoneColorTable.put(table, 0xFF7A3A, Blocks.CREAKING_HEART);
	}
}
