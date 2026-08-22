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
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

final class GlowtoneColorTable {
	private final int missingValue;
	private final Consumer<Reference2IntMap<Block>> builtIns;

	private volatile @Nullable Reference2IntMap<Block> resolved;
	private @Nullable Reference2IntMap<Block> overlay;
	private boolean overlayReplacesBuiltIns;

	GlowtoneColorTable(int missingValue, Consumer<Reference2IntMap<Block>> builtIns) {
		this.missingValue = missingValue;
		this.builtIns = builtIns;
	}

	int get(Block block) {
		var table = this.resolved;
		if (table == null) table = this.resolve();
		return table.getInt(block);
	}

	synchronized void overlay(@Nullable Reference2IntMap<Block> overlay, boolean replacesBuiltIns) {
		this.overlay = overlay != null && !overlay.isEmpty() ? overlay : null;
		this.overlayReplacesBuiltIns = replacesBuiltIns;
		this.resolved = null;
	}

	int size() {
		var table = this.resolved;
		if (table == null) table = this.resolve();
		return table.size();
	}

	private synchronized Reference2IntMap<Block> resolve() {
		var existing = this.resolved;
		if (existing != null) return existing;

		var table = new Reference2IntOpenHashMap<Block>();
		table.defaultReturnValue(this.missingValue);

		if (!this.overlayReplacesBuiltIns) this.builtIns.accept(table);

		var currentOverlay = this.overlay;
		if (currentOverlay != null) {
			for (var entry : currentOverlay.reference2IntEntrySet()) {
				table.put(entry.getKey(), entry.getIntValue());
			}
		}

		table.trim();
		this.resolved = table;
		return table;
	}

	static void put(Reference2IntMap<Block> table, int packed, Block... blocks) {
		for (var block : blocks) {
			if (block != null) table.put(block, packed);
		}
	}

	static void putAll(Reference2IntMap<Block> table, int packed, Iterable<Block> blocks) {
		for (var block : blocks) {
			if (block != null) table.put(block, packed);
		}
	}
}
