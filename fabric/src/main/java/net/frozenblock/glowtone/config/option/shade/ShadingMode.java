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

package net.frozenblock.glowtone.config.option.shade;

import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.util.StringRepresentable;

@ClientOnly
public enum ShadingMode implements StringRepresentable {
	NONE("none"),
	NON_EMISSIVE("non_emissive"),
	ALL("all");
	public static final ShadingMode DEFAULT = NON_EMISSIVE;
	public static final EnumCodec<ShadingMode> CODEC = StringRepresentable.fromEnum(ShadingMode::values);

	private final String name;

	ShadingMode(String name) {
		this.name = name;
	}

	public String id() {
		return this.name;
	}

	public boolean unshadeEmissive() {
		return this != ALL;
	}

	public boolean unshadeAll() {
		return this == NONE;
	}

	public String translationKey() {
		return "options.glowtone.shading." + this.name;
	}

	public static ShadingMode byId(String id) {
		for (ShadingMode mode : values()) {
			if (mode.name.equals(id)) return mode;
		}
		return DEFAULT;
	}

	@Override
	public String getSerializedName() {
		return this.name;
	}
}
