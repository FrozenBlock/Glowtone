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

package net.frozenblock.glowtone.config;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public enum EmissivesMode {
	SHADED("shaded", false),
	SHADELESS("shadeless", true);

	public static final EmissivesMode DEFAULT = SHADELESS;

	private final String id;
	private final boolean shadeless;

	EmissivesMode(String id, boolean shadeless) {
		this.id = id;
		this.shadeless = shadeless;
	}

	public String id() {
		return this.id;
	}

	public boolean shadeless() {
		return this.shadeless;
	}

	public String translationKey() {
		return "options.glowtone.emissives." + this.id;
	}

	public static EmissivesMode byId(String id) {
		for (EmissivesMode mode : values()) {
			if (mode.id.equals(id)) return mode;
		}
		return DEFAULT;
	}
}
