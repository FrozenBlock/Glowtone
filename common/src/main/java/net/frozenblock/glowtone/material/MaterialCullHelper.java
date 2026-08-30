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

package net.frozenblock.glowtone.material;

import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

@ClientOnly
public final class MaterialCullHelper {

	@Nullable
	public static Boolean overrideRenderFace(BlockState state, BlockState neighbour) {
		if (!BlockMaterials.anyFaceCulling()) return null;

		final BlockMaterials.Assigned rendered = BlockMaterials.anySelfCulling()
			? BlockMaterials.assigned(state)
			: BlockMaterials.UNASSIGNED;
		final CullMode selfMode = rendered.material().cull().selfMode();

		if (!BlockMaterials.anyCastCulling() && selfMode != CullMode.SAME_MATERIAL) {
			return selfMode.shouldRender(state, neighbour, rendered.id(), null);
		}

		final BlockMaterials.Assigned occluder = BlockMaterials.assigned(neighbour);
		final Boolean self = selfMode.shouldRender(state, neighbour, rendered.id(), occluder.id());
		if (self != null) return self;

		return occluder.material().cull().castMode().shouldRender(state, neighbour, rendered.id(), occluder.id());
	}

	public static boolean shouldRenderFace(BlockState state, BlockState neighbour, boolean automatic) {
		final Boolean override = overrideRenderFace(state, neighbour);
		return override == null ? automatic : override;
	}

	private MaterialCullHelper() {}
}
