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

package net.frozenblock.glowtone.mixin.client.material.sodium;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.DefaultMaterials;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.Material;
import net.frozenblock.glowtone.light.BlockLightPropertiesRenderer;
import net.frozenblock.glowtone.material.render.BlockMaterialRenderer;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@ClientOnly
@Mixin(BlockRenderer.class)
public class BlockRendererMaterialMixin {

	@Inject(method = "renderModel", at = @At("HEAD"))
	private void glowtone$beginBlockMaterial(
		CallbackInfo info,
		@Local(argsOnly = true) BlockState state
	) {
		BlockLightPropertiesRenderer.beginBlock(state);
		BlockMaterialRenderer.beginBlock(state);
	}

	@Inject(method = "renderModel", at = @At("RETURN"))
	private void glowtone$endBlockMaterial(CallbackInfo info) {
		BlockLightPropertiesRenderer.endBlock();
		BlockMaterialRenderer.endBlock();
	}

	@ModifyExpressionValue(
		method = "processQuad",
		at = @At(
			value = "INVOKE",
			target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/terrain/material/DefaultMaterials;forChunkLayer(Lnet/minecraft/client/renderer/chunk/ChunkSectionLayer;)Lnet/caffeinemc/mods/sodium/client/render/chunk/terrain/material/Material;"
		)
	)
	private Material glowtone$overrideMaterialLayer(Material material) {
		final ChunkSectionLayer override = BlockMaterialRenderer.overrideLayer();
		return override == null ? material : DefaultMaterials.forChunkLayer(override);
	}
}
