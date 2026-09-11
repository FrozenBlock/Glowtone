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

package net.frozenblock.glowtone.config.pack;

import com.mojang.serialization.Codec;
import java.util.Locale;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.StringRepresentable;

@ClientOnly
public enum GlowtonePackImpact implements StringRepresentable {
	LOW,
	MEDIUM,
	HIGH,
	VARIES;

	public static final Codec<GlowtonePackImpact> CODEC = StringRepresentable.fromEnum(GlowtonePackImpact::values);

	@Override
	public String getSerializedName() {
		return this.name().toLowerCase(Locale.ROOT);
	}

	public MutableComponent displayName() {
		return Component.translatable("options.glowtone.packs.impact." + this.getSerializedName());
	}

	public MutableComponent describe() {
		return Component.translatable("options.glowtone.packs.impact", this.displayName());
	}
}
