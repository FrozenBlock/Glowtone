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

package net.frozenblock.glowtone.config.option.ao;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.StringRepresentable;

@Environment(EnvType.CLIENT)
public enum AmbientOcclusionMode implements StringRepresentable {
	OFF("off"),
	FAST("fast"),
	FANCY("fancy");
	public static final AmbientOcclusionMode DEFAULT = FANCY;
	public static final EnumCodec<AmbientOcclusionMode> CODEC = StringRepresentable.fromEnum(AmbientOcclusionMode::values);
	private final String name;

	AmbientOcclusionMode(String name) {
		this.name = name;
	}

	public boolean vanilla() {
		return this == FAST;
	}

	public boolean glowtone() {
		return this == FANCY;
	}

	public String translationKey() {
		return "options.glowtone.ambient_occlusion." + this.name;
	}

	@Override
	public String getSerializedName() {
		return this.name;
	}
}
