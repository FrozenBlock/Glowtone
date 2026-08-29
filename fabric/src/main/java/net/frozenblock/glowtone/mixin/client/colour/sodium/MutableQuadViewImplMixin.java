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

import net.caffeinemc.mods.sodium.client.render.model.MutableQuadViewImpl;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.frozenblock.glowtone.bloom.GlowtoneBloom;
import net.frozenblock.glowtone.render.sodium.GlowtoneSodiumQuad;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Environment(EnvType.CLIENT)
@Mixin(MutableQuadViewImpl.class)
public class MutableQuadViewImplMixin implements GlowtoneSodiumQuad {
	@Unique
	private boolean glowtone$emissive;

	@Unique
	@Override
	public boolean glowtone$emissive() {
		return this.glowtone$emissive;
	}

	@Unique
	@Override
	public void glowtone$setEmissive(boolean emissive) {
		this.glowtone$emissive = emissive;
	}

	@Inject(method = "fromBakedQuad", at = @At("RETURN"))
	private void glowtone$captureEmissive(BakedQuad quad, CallbackInfoReturnable<MutableQuadViewImpl> info) {
		this.glowtone$emissive = GlowtoneBloom.isEmissiveQuad(quad);
	}
}
