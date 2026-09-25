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

package net.frozenblock.glowtone.entity.impl;

import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.renderer.rendertype.RenderType;
import java.util.Optional;

@ClientOnly
public interface GTEmissiveRenderType {

	default boolean glowtone$isEmissive() {
		throw new AssertionError();
	}

	default RenderType glowtone$markEmissive() {
		throw new AssertionError();
	}

	default boolean glowtone$isEmissiveResourceValid() {
		throw new AssertionError();
	}

	default Optional<RenderType> glowtone$emissiveRenderType() {
		throw new AssertionError();
	}
}
