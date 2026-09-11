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
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.frozenblock.glowtone.mixin.client.material.TextureSlotsAccessor;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.MaterialBaker;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

@ClientOnly
public final class BlockTextureSlots {
	private static volatile Map<BlockState, List<Map<String, Slot>>> models = Map.of();
	private static final Map<Identifier, Slot> EMISSIVE_OVERLAYS = new ConcurrentHashMap<>();

	public record Slot(TextureAtlasSprite sprite, float u0, float u1, float v0, float v1) {
		public static Slot of(TextureAtlasSprite sprite) {
			return new Slot(sprite, sprite.getU0(), sprite.getU1(), sprite.getV0(), sprite.getV1());
		}

		public boolean contains(float u, float v) {
			return u >= this.u0 && u <= this.u1 && v >= this.v0 && v <= this.v1;
		}
	}

	public static void record(
		Map<BlockState, BlockStateModel.UnbakedRoot> roots,
		Map<Identifier, ResolvedModel> resolved,
		MaterialBaker materials
	) {
		final Map<Identifier, Map<String, Slot>> byModel = new HashMap<>();
		final Map<BlockStateModel.UnbakedRoot, List<Map<String, Slot>>> byRoot = new IdentityHashMap<>();
		final Map<BlockState, List<Map<String, Slot>>> recorded = new IdentityHashMap<>(roots.size());

		roots.forEach((state, root) -> recorded.put(state, byRoot.computeIfAbsent(root, unbaked -> {
			final List<Identifier> locations = new ArrayList<>();
			unbaked.resolveDependencies(locations::add);

			final List<Map<String, Slot>> slots = new ArrayList<>(locations.size());
			for (Identifier location : locations) {
				final ResolvedModel model = resolved.get(location);
				if (model == null) continue;

				slots.add(byModel.computeIfAbsent(location, key -> slotsOf(model, materials)));
			}

			return List.copyOf(slots);
		})));

		models = recorded;
	}

	private static Map<String, Slot> slotsOf(ResolvedModel model, MaterialBaker materials) {
		final Map<String, Material> declared = ((TextureSlotsAccessor) (Object) model.getTopTextureSlots()).glowtone$resolvedValues();
		if (declared.isEmpty()) return Map.of();

		final Map<String, Slot> slots = new HashMap<>(declared.size());
		declared.forEach((name, material) -> {
			final Material.Baked baked = materials.get(material, model);
			if (baked == null || baked.sprite().contents().name().equals(MissingTextureAtlasSprite.getLocation())) return;

			slots.put(name, Slot.of(baked.sprite()));
		});

		return Map.copyOf(slots);
	}

	@Nullable
	public static Slot resolve(BlockState state, String name) {
		final List<Map<String, Slot>> candidates = models.get(state);
		if (candidates == null) return null;

		for (Map<String, Slot> slots : candidates) {
			final Slot slot = slots.get(name);
			if (slot != null) return slot;
		}

		return null;
	}

	public static void recordEmissiveOverlay(Identifier base, TextureAtlasSprite sprite) {
		EMISSIVE_OVERLAYS.put(base, Slot.of(sprite));
	}

	@Nullable
	public static Slot overlayOf(Slot slot) {
		return EMISSIVE_OVERLAYS.get(slot.sprite().contents().name());
	}

	public static boolean withinEmissiveOverlay(float u, float v) {
		for (Slot slot : EMISSIVE_OVERLAYS.values()) {
			if (slot.contains(u, v)) return true;
		}

		return false;
	}

	public static void clear() {
		models = Map.of();
		EMISSIVE_OVERLAYS.clear();
	}

	private BlockTextureSlots() {}
}
