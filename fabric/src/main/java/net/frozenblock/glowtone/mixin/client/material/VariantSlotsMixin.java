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

package net.frozenblock.glowtone.mixin.client.material;

import net.frozenblock.glowtone.material.render.BlockTextureSlots;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.block.dispatch.Variant;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@ClientOnly
@Mixin(Variant.class)
public class VariantSlotsMixin {

	@Inject(method = "bake", at = @At("RETURN"))
	private void glowtone$recordTextureSlots(ModelBaker baker, CallbackInfoReturnable<BlockStateModelPart> info) {
		if (!BlockTextureSlots.wanted()) return;

		final ResolvedModel model = baker.getModel(((Variant) (Object) this).modelLocation());
		if (model == null) return;

		final TextureSlots slots = model.getTopTextureSlots();
		for (String slot : BlockTextureSlots.wantedNames()) {
			final Material material = slots.getMaterial(slot);
			if (material == null) continue;

			final Material.Baked baked = baker.materials().get(material, model);
			if (baked != null) BlockTextureSlots.record(slot, baked.sprite());
		}
	}
}
