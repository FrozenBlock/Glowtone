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
public enum AmbientOcclusionMode {
	OFF("off"),
	FAST("fast"),
	FANCY("fancy");

	public static final AmbientOcclusionMode DEFAULT = FANCY;

	private final String id;

	AmbientOcclusionMode(String id) {
		this.id = id;
	}

	public String id() {
		return this.id;
	}

	public boolean vanilla() {
		return this == FAST;
	}

	public boolean glowtone() {
		return this == FANCY;
	}

	public String translationKey() {
		return "options.glowtone.ambient_occlusion." + this.id;
	}

	public static AmbientOcclusionMode byId(String id) {
		for (AmbientOcclusionMode mode : values()) {
			if (mode.id.equals(id)) return mode;
		}
		return DEFAULT;
	}
}
