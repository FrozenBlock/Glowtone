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

package net.frozenblock.glowtone.light.edge;

import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableQuadView;
import net.minecraft.core.Direction;

public record FabricMutableQuad(MutableQuadView quad) implements GlowtoneMutableQuad {
	@Override
	public float x(int vertex) {
		return this.quad.x(vertex);
	}

	@Override
	public float y(int vertex) {
		return this.quad.y(vertex);
	}

	@Override
	public float z(int vertex) {
		return this.quad.z(vertex);
	}

	@Override
	public Direction lightFace() {
		return this.quad.lightFace();
	}

	@Override
	public int color(int vertex) {
		return this.quad.color(vertex);
	}

	@Override
	public void setColor(int vertex, int color) {
		this.quad.color(vertex, color);
	}
}
