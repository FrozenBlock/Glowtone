/*
 * Copyright 2025-2026 FrozenBlock
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

package net.frozenblock.glowtone.mixin;

import java.lang.reflect.Method;
import org.jetbrains.annotations.Nullable;

final class GlowtoneMixinModList {

	static boolean isLoaded(String mod) {
		final Boolean fabric = fabric(mod);
		if (fabric != null) return fabric;

		final Boolean neoForge = neoForge(mod);
		if (neoForge != null) return neoForge;

		System.getLogger("Glowtone").log(
			System.Logger.Level.WARNING,
			"Glowtone could not reach a mod list to detect " + mod + "; mixins gated on it will be skipped"
		);
		return false;
	}

	@Nullable
	private static Boolean fabric(String mod) {
		try {
			final Class<?> loader = Class.forName("net.fabricmc.loader.api.FabricLoader");
			final Method getInstance = loader.getMethod("getInstance");
			final Object instance = getInstance.invoke(null);
			if (instance == null) return null;

			return (Boolean) getInstance.getReturnType().getMethod("isModLoaded", String.class).invoke(instance, mod);
		} catch (ReflectiveOperationException | RuntimeException failure) {
			return null;
		}
	}

	@Nullable
	private static Boolean neoForge(String mod) {
		try {
			final Method getCurrent = currentLoader(Class.forName("net.neoforged.fml.loading.FMLLoader"));
			final Object current = getCurrent.invoke(null);
			if (current == null) return null;

			final Method getLoadingModList = getCurrent.getReturnType().getMethod("getLoadingModList");
			final Object loadingModList = getLoadingModList.invoke(current);
			if (loadingModList == null) return null;

			return getLoadingModList.getReturnType().getMethod("getModFileById", String.class).invoke(loadingModList, mod) != null;
		} catch (ReflectiveOperationException | RuntimeException failure) {
			return null;
		}
	}

	private static Method currentLoader(Class<?> fmlLoader) throws NoSuchMethodException {
		try {
			return fmlLoader.getMethod("getCurrentOrNull");
		} catch (NoSuchMethodException ignored) {
			return fmlLoader.getMethod("getCurrent");
		}
	}

	private GlowtoneMixinModList() {}
}
