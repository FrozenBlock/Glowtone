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

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.ArrayList;
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

	/**
	 * Captures all sprites (and relevant data) per-model and stores them.
	 * <p>
	 * Sets {@link #models} to: BlockState -> List(Slot Name -> Slot)
	 */
	public static void record(
		Map<BlockState, BlockStateModel.UnbakedRoot> unbakedModels,
		Map<Identifier, ResolvedModel> resolvedModels,
		MaterialBaker materials
	) {
		final Map<Identifier, Map<String, Slot>> byModel = new Object2ObjectOpenHashMap<>();
		final Map<BlockStateModel.UnbakedRoot, List<Map<String, Slot>>> byRoot = new IdentityHashMap<>();
		final Map<BlockState, List<Map<String, Slot>>> recorded = new IdentityHashMap<>(unbakedModels.size());

		unbakedModels.forEach((state, root) -> recorded.put(state, byRoot.computeIfAbsent(root, unbaked -> {
			final List<Identifier> modelIds = new ArrayList<>();
			unbaked.resolveDependencies(modelIds::add);

			final List<Map<String, Slot>> slots = new ArrayList<>(modelIds.size());
			for (Identifier modelId : modelIds) {
				final ResolvedModel model = resolvedModels.get(modelId);
				if (model == null) continue;

				slots.add(byModel.computeIfAbsent(modelId, key -> slotsOf(model, materials)));
			}

			return List.copyOf(slots);
		})));

		models = recorded;
	}

	/**
	 * @return Slot Name -> Slot for the model
	 */
	private static Map<String, Slot> slotsOf(ResolvedModel model, MaterialBaker materials) {
		final Map<String, Material> declared = ((TextureSlotsAccessor) model.getTopTextureSlots()).glowtone$resolvedValues();
		if (declared.isEmpty()) return Map.of();

		final Map<String, Slot> slots = new Object2ObjectOpenHashMap<>(declared.size());
		declared.forEach((name, material) -> {
			final Material.Baked baked = materials.get(material, model);
			if (baked == null || baked.sprite().contents().name().equals(MissingTextureAtlasSprite.getLocation())) return;

			slots.put(name, Slot.of(baked.sprite()));
		});

		return Map.copyOf(slots);
	}

	/**
	 * @return The {@link Slot} used for the {@link BlockState}'s model with the given {@code name}.
	 */
	@Nullable
	public static Slot resolve(BlockState state, String name) {
		final List<Map<String, Slot>> candidates = models.get(state);
		if (candidates == null) return null;

		for (Map<String, Slot> slots : candidates) {
			final Slot slot = slots.get(name);
			// FIXME: One BlockState can have multiple models, as such the Slot may not be accurate.
			if (slot != null) return slot;
		}

		return null;
	}

	/**
	 * Captures the emissive overlay of a texture and stores it.
	 * <p>
	 * Puts in {@link #EMISSIVE_OVERLAYS}: Base Texture ID -> Emissive Overlay
	 */
	public static void recordEmissiveOverlay(Identifier base, TextureAtlasSprite sprite) {
		EMISSIVE_OVERLAYS.put(base, Slot.of(sprite));
	}

	/**
	 * @return The emissive overlay of the given {@link Slot}, in {@link Slot} form,if available.
	 */
	@Nullable
	public static Slot overlayOf(Slot slot) {
		return EMISSIVE_OVERLAYS.get(slot.sprite().contents().name());
	}

	/**
	 * @return Whether the texture at the given UV coordinates is an emissive overlay.
	 */
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


	public record Slot(TextureAtlasSprite sprite, float u0, float u1, float v0, float v1) {

		public static Slot of(TextureAtlasSprite sprite) {
			return new Slot(sprite, sprite.getU0(), sprite.getU1(), sprite.getV0(), sprite.getV1());
		}

		public boolean contains(float u, float v) {
			return u >= this.u0 && u <= this.u1 && v >= this.v0 && v <= this.v1;
		}
	}

	private BlockTextureSlots() {}
}
