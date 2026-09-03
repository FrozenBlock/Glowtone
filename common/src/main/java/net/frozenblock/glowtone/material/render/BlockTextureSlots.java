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

import com.mojang.logging.LogUtils;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@ClientOnly
public final class BlockTextureSlots {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final Map<String, Slot> SLOTS = new ConcurrentHashMap<>();
	private static final Set<String> CONFLICTS = java.util.concurrent.ConcurrentHashMap.newKeySet();
	private static volatile java.util.Set<String> wanted = java.util.Set.of();

	public static void setWanted(java.util.Set<String> names) {
		wanted = java.util.Set.copyOf(names);
		if (!names.isEmpty()) LOGGER.info("Glowtone will resolve block texture slots {} from block models", wanted);
	}

	public static boolean wanted() {
		return !wanted.isEmpty();
	}

	public static java.util.Set<String> wantedNames() {
		return wanted;
	}

	public record Slot(TextureAtlasSprite sprite, float u0, float u1, float v0, float v1) {
		public static Slot of(TextureAtlasSprite sprite) {
			return new Slot(sprite, sprite.getU0(), sprite.getU1(), sprite.getV0(), sprite.getV1());
		}

		public boolean contains(float u, float v) {
			return u >= this.u0 && u <= this.u1 && v >= this.v0 && v <= this.v1;
		}
	}

	private static final Set<Slot> EMISSIVE_OVERLAYS = java.util.concurrent.ConcurrentHashMap.newKeySet();

	public static void recordEmissiveOverlay(TextureAtlasSprite sprite) {
		EMISSIVE_OVERLAYS.add(Slot.of(sprite));
	}

	public static boolean withinEmissiveOverlay(float u, float v) {
		for (Slot slot : EMISSIVE_OVERLAYS) {
			if (slot.contains(u, v)) return true;
		}

		return false;
	}

	public static void record(String slot, TextureAtlasSprite sprite) {
		final Slot existing = SLOTS.putIfAbsent(slot, Slot.of(sprite));
		if (existing != null) {
			if (existing.sprite() != sprite && CONFLICTS.add(slot)) {
				LOGGER.warn("Block texture slot '{}' is declared as both {} and {}; a material bakes one rectangle, so the first wins",
					slot, existing.sprite().contents().name(), sprite.contents().name());
			}

			return;
		}

		LOGGER.info("Glowtone resolved block texture slot '{}' -> {}", slot, sprite.contents().name());
	}

	@Nullable
	public static Slot get(String slot) {
		return SLOTS.get(slot);
	}

	@Nullable
	public static Map<String, Slot> resolve(Iterable<String> wanted) {
		final Map<String, Slot> resolved = new LinkedHashMap<>();
		for (String slot : wanted) {
			final Slot found = SLOTS.get(slot);
			if (found == null) return null;

			resolved.put(slot, found);
		}

		return resolved;
	}

	public static void clear() {
		SLOTS.clear();
		CONFLICTS.clear();
		EMISSIVE_OVERLAYS.clear();
	}

	private BlockTextureSlots() {}
}
