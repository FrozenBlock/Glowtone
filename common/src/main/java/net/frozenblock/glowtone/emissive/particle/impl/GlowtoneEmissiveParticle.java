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

package net.frozenblock.glowtone.emissive.particle.impl;

import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.particle.SingleQuadParticle;

@ClientOnly
public interface GlowtoneEmissiveParticle {

	boolean glowtone$hasEmissiveOverlay();

	SingleQuadParticle.Layer glowtone$emissiveLayer();

	float glowtone$emissiveU0();

	float glowtone$emissiveU1();

	float glowtone$emissiveV0();

	float glowtone$emissiveV1();

	float glowtone$emissiveRCol();

	float glowtone$emissiveGCol();

	float glowtone$emissiveBCol();

	int glowtone$emissiveLightEmission();
}
