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

package net.frozenblock.glowtone.config.option.color;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.StringRepresentable;

@Environment(EnvType.CLIENT)
public enum ColoredLightingMode implements StringRepresentable {
	OFF("off"),
	SUBTLE("subtle"),
	INTENSE("intense");
	public static final StringRepresentable.EnumCodec<ColoredLightingMode> CODEC = StringRepresentable.fromEnum(ColoredLightingMode::values);
	private final String name;

	ColoredLightingMode(String name) {
		this.name = name;
	}

	public String translationKey() {
		return "options.glowtone.colored_lighting." + this.name;
	}

	public boolean enabled() {
		return this != OFF;
	}

	@Override
	public String getSerializedName() {
		return this.name;
	}
}
