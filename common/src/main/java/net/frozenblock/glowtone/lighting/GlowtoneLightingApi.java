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

package net.frozenblock.glowtone.lighting;

import java.util.Set;
import java.util.function.DoubleSupplier;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

@ClientOnly
public final class GlowtoneLightingApi {

	public static void registerFactor(Identifier id, DoubleSupplier supplier) {
		LightingFactors.register(id, supplier);
	}

	public static void setFactor(Identifier id, float value) {
		LightingFactors.register(id, () -> value);
	}

	public static void removeFactor(Identifier id) {
		LightingFactors.unregister(id);
	}

	public static @Nullable Identifier activeProfile() {
		return GlowtoneLighting.activeProfile();
	}

	public static Set<Identifier> profiles() {
		return GlowtoneLighting.profileIds();
	}

	public static void overrideProfile(@Nullable Identifier id) {
		GlowtoneLighting.overrideProfile(id);
	}

	public static void onChange(Runnable listener) {
		GlowtoneLighting.onChange(listener);
	}

	private GlowtoneLightingApi() {}
}
