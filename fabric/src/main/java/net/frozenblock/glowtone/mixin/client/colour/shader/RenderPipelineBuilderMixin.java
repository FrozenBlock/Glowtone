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

package net.frozenblock.glowtone.mixin.client.colour.shader;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.frozenblock.glowtone.bloom.GlowtoneEmissiveShaders;
import net.frozenblock.glowtone.render.GlowtoneVertexFormats;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Environment(EnvType.CLIENT)
@Mixin(RenderPipeline.Builder.class)
public abstract class RenderPipelineBuilderMixin {
	@Unique
	private static final Identifier OPAQUE_TERRAIN = Identifier.withDefaultNamespace("pipeline/solid_terrain");

	@Unique
	private static final Identifier CUTOUT_TERRAIN = Identifier.withDefaultNamespace("pipeline/cutout_terrain");

	@Unique
	private static final Identifier TRANSLUCENT_TERRAIN = Identifier.withDefaultNamespace("pipeline/translucent_terrain");

	@Shadow
	private Optional<Identifier> fragmentShader;

	@Shadow
	private Optional<Identifier> location;

	@Shadow
	public abstract RenderPipeline.Builder withVertexBinding(int bindingIndex, VertexFormat vertexFormat);

	@Shadow
	public abstract RenderPipeline.Builder withShaderDefine(String key);

	@Inject(method = "build", at = @At("HEAD"))
	private void glowtone$useExtendedBlockFormat(CallbackInfoReturnable<RenderPipeline> info) {
		if (this.fragmentShader.isEmpty() || !"core/terrain".equals(this.fragmentShader.get().getPath())) return;

		this.withVertexBinding(0, GlowtoneVertexFormats.EXTENDED_BLOCK);
		if (this.location.isEmpty()) return;

		final Identifier pipeline = this.location.get();
		if (OPAQUE_TERRAIN.equals(pipeline) || CUTOUT_TERRAIN.equals(pipeline)) {
			this.withShaderDefine(GlowtoneEmissiveShaders.SHADED_TERRAIN_DEFINE);
			this.withShaderDefine(GlowtoneEmissiveShaders.OPAQUE_TERRAIN_DEFINE);
		} else if (TRANSLUCENT_TERRAIN.equals(pipeline)) {
			this.withShaderDefine(GlowtoneEmissiveShaders.SHADED_TERRAIN_DEFINE);
			this.withShaderDefine(GlowtoneEmissiveShaders.TRANSLUCENT_TERRAIN_DEFINE);
		}
	}
}
