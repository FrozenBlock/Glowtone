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

package net.frozenblock.glowtone.render.sodium;

import net.caffeinemc.mods.sodium.client.world.cloned.ChunkRenderContext;
import net.caffeinemc.mods.sodium.client.world.cloned.ClonedChunkSection;
import net.frozenblock.glowtone.render.GlowtoneChromaBake;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.PalettedContainerRO;

import java.util.Arrays;

public final class GlowtoneSodiumFlood {
	private static final int RADIUS = 1;
	private static final int GRID = 3;

	@SuppressWarnings("unchecked")
	private static final ThreadLocal<PalettedContainerRO<BlockState>[]> GRIDS =
		ThreadLocal.withInitial(() -> new PalettedContainerRO[GRID * GRID * GRID]);

	private GlowtoneSodiumFlood() {
	}

	public static void begin(ChunkRenderContext context) {
		final SectionPos origin = context.getOrigin();
		final PalettedContainerRO<BlockState>[] grid = GRIDS.get();
		Arrays.fill(grid, null);

		for (final ClonedChunkSection section : context.getSections()) {
			if (section == null) continue;

			final SectionPos pos = section.getPosition();
			final int x = pos.x() - origin.x() + RADIUS;
			final int y = pos.y() - origin.y() + RADIUS;
			final int z = pos.z() - origin.z() + RADIUS;
			if ((x | y | z) < 0 || x >= GRID || y >= GRID || z >= GRID) continue;

			grid[x + y * GRID + z * GRID * GRID] = section.getBlockData();
		}

		GlowtoneChromaBake.beginSodiumSection(origin, grid);
	}

	public static void end() {
		GlowtoneChromaBake.endSection();
	}
}
