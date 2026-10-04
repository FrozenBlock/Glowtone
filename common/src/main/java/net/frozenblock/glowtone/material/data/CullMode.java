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

package net.frozenblock.glowtone.material.data;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

@ClientOnly
public enum CullMode implements StringRepresentable {
	AUTO("auto"),
	NEVER("never"),
	ALWAYS("always"),
	SAME_BLOCK("same_block"),
	SAME_MATERIAL("same_material");
	public static final Codec<CullMode> CODEC = StringRepresentable.fromEnum(CullMode::values);

	private final String name;

	CullMode(String name) {
		this.name = name;
	}

	@Override
	public String getSerializedName() {
		return this.name;
	}

	public boolean decides() {
		return this != AUTO;
	}

	@Nullable
	public Boolean shouldRender(BlockState rendered, BlockState neighbour, @Nullable Identifier renderedMaterial, @Nullable Identifier neighbourMaterial) {
		return switch (this) {
			case AUTO -> null;
			case NEVER -> Boolean.TRUE;
			case ALWAYS -> Boolean.FALSE;
			case SAME_BLOCK -> rendered.getBlock() == neighbour.getBlock() ? Boolean.FALSE : null;
			case SAME_MATERIAL -> renderedMaterial != null && renderedMaterial.equals(neighbourMaterial) ? Boolean.FALSE : null;
		};
	}
}
