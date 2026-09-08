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

package net.frozenblock.glowtone.light.edge.impl;

import net.minecraft.core.Direction;
import com.mojang.blaze3d.vertex.QuadInstance;
import net.minecraft.client.resources.model.geometry.BakedQuad;

public record NeoForgeMutableQuad(BakedQuad quad, QuadInstance instance) implements GlowtoneMutableQuad {
	@Override
	public float x(int vertex) {
		return this.quad.position(vertex).x();
	}

	@Override
	public float y(int vertex) {
		return this.quad.position(vertex).y();
	}

	@Override
	public float z(int vertex) {
		return this.quad.position(vertex).z();
	}

	@Override
	public Direction lightFace() {
		return this.quad.direction();
	}

	@Override
	public int color(int vertex) {
		return this.instance.getColor(vertex);
	}

	@Override
	public void setColor(int vertex, int color) {
		this.instance.setColor(vertex, color);
	}
}
