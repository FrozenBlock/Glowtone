/*
 * Copyright 2025 FrozenBlock
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

package net.frozenblock.glowtone;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

@ApiStatus.Internal
public final class GlowtoneConstants {
	public static final String PROJECT_ID = "Glowtone";
	public static final String MOD_ID = "glowtone";

	public static boolean GLOWTONE_EMISSIVES = false;
	public static boolean GLOWTONE_SHADING = false;

	@Contract("_ -> new")
	@NotNull
	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(GlowtoneConstants.MOD_ID, path);
	}

	@NotNull
	public static String string(String path) {
		return id(path).toString();
	}

	@NotNull
	public static String safeString(String path) {
		return id(path).toString().replace(":", "_");
	}

	private GlowtoneConstants() {}
}
