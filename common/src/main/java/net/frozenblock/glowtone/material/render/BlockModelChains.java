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

package net.frozenblock.glowtone.material.render;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;

@ClientOnly
public final class BlockModelChains {
	private static final int MAX_DEPTH = 32;

	private static volatile Map<BlockState, Set<Identifier>> chains = Map.of();

	public static void record(Map<BlockState, BlockStateModel.UnbakedRoot> roots, Map<Identifier, ResolvedModel> resolved) {
		final Map<ResolvedModel, Identifier> names = new IdentityHashMap<>(resolved.size());
		resolved.forEach((id, model) -> names.putIfAbsent(model, id));

		final Map<Identifier, Set<Identifier>> byModel = new HashMap<>();
		final Map<BlockStateModel.UnbakedRoot, Set<Identifier>> byRoot = new IdentityHashMap<>();
		final Map<BlockState, Set<Identifier>> recorded = new IdentityHashMap<>(roots.size());

		roots.forEach((state, root) -> recorded.put(state, byRoot.computeIfAbsent(root, unbaked -> {
			final List<Identifier> locations = new ArrayList<>();
			unbaked.resolveDependencies(locations::add);

			final Set<Identifier> chain = new HashSet<>();
			for (Identifier location : locations) {
				chain.addAll(byModel.computeIfAbsent(location, key -> ancestry(key, resolved, names)));
			}

			return Set.copyOf(chain);
		})));

		chains = recorded;
	}

	private static Set<Identifier> ancestry(Identifier location, Map<Identifier, ResolvedModel> resolved, Map<ResolvedModel, Identifier> names) {
		final Set<Identifier> chain = new HashSet<>();
		chain.add(location);

		ResolvedModel model = resolved.get(location);
		for (int depth = 0; model != null && depth < MAX_DEPTH; depth++) {
			final Identifier name = names.get(model);
			if (name != null) chain.add(name);

			model = model.parent();
		}

		return Set.copyOf(chain);
	}

	public static Map<BlockState, Set<Identifier>> chains() {
		return chains;
	}

	public static boolean uses(BlockState state, Identifier model) {
		final Set<Identifier> chain = chains.get(state);
		return chain != null && chain.contains(model);
	}

	public static void clear() {
		chains = Map.of();
	}

	private BlockModelChains() {}
}
