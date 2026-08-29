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

package net.frozenblock.glowtone.mixin.client.colour.sodium;

import net.caffeinemc.mods.sodium.client.render.model.AbstractBlockRenderContext;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.frozenblock.glowtone.render.sodium.GlowtoneSodiumContext;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

@Pseudo
@Environment(EnvType.CLIENT)
@Mixin(AbstractBlockRenderContext.class)
public class BlockRenderContextMixin implements GlowtoneSodiumContext {
	@Shadow
	protected BlockAndTintGetter level;
	@Shadow
	protected BlockPos pos;

	@Override
	public BlockAndTintGetter glowtone$level() {
		return this.level;
	}

	@Override
	public BlockPos glowtone$pos() {
		return this.pos;
	}
}
