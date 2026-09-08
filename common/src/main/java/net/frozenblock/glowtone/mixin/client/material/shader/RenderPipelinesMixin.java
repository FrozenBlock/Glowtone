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

package net.frozenblock.glowtone.mixin.client.material.shader;

import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import java.util.List;
import java.util.Optional;
import net.frozenblock.glowtone.bloom.EmissiveShaderPatcher;
import net.frozenblock.glowtone.material.MaterialBlockTextures;
import net.frozenblock.glowtone.material.MaterialSamplers;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@ClientOnly
@Mixin(RenderPipeline.Builder.class)
public class RenderPipelinesMixin {

	@Inject(method = "build", at = @At("HEAD"))
	private void glowtone$declareMaterialSamplers(CallbackInfoReturnable<RenderPipeline> info) {
		final RenderPipeline.Builder instance = RenderPipeline.Builder.class.cast(this);
		final Optional<Identifier> fragment = instance.fragmentShader;
		if (fragment == null || fragment.isEmpty() || !EmissiveShaderPatcher.usesMaterialSamplers(fragment.get())) return;

		final Optional<List<BindGroupLayout>> declared = instance.bindGroupLayouts;
		if (declared != null && declared.isPresent() && declared.get().contains(MaterialSamplers.LAYOUT)) return;

		instance.withBindGroupLayout(MaterialSamplers.LAYOUT);
		instance.withBindGroupLayout(MaterialBlockTextures.LAYOUT);
		final Optional<List<BindGroupLayout>> layouts = instance.bindGroupLayouts;
		if (layouts == null || layouts.isEmpty() || !layouts.get().contains(BindGroupLayouts.GLOBALS)) {
			instance.withBindGroupLayout(BindGroupLayouts.GLOBALS);
		}
	}
}
