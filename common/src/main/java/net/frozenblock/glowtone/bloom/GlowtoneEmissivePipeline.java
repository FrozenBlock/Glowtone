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

package net.frozenblock.glowtone.bloom;

import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import java.util.Map;
import java.util.Set;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.resources.Identifier;
import java.util.concurrent.ConcurrentHashMap;

@ClientOnly
public final class GlowtoneEmissivePipeline extends RenderPipeline {
	private static final Set<Identifier> SELF_LIT_PIPELINES = Set.of(
		Identifier.withDefaultNamespace("pipeline/fire_screen_effect"),
		Identifier.withDefaultNamespace("pipeline/celestial"),
		Identifier.withDefaultNamespace("pipeline/stars"),
		Identifier.withDefaultNamespace("pipeline/beacon_beam_opaque"),
		Identifier.withDefaultNamespace("pipeline/beacon_beam_translucent"),
		Identifier.withDefaultNamespace("pipeline/lightning"),
		Identifier.withDefaultNamespace("pipeline/end_portal")
	);

	private static final Map<RenderPipeline, RenderPipeline> TWINS = new ConcurrentHashMap<>();

	private GlowtoneEmissivePipeline(RenderPipeline base, ColorTargetState[] colorTargetStates) {
		super(
			base.getLocation().withSuffix("_glowtone_emissive"),
			base.getVertexShader(),
			base.getFragmentShader(),
			base.getShaderDefines(),
			base.getBindGroupLayouts(),
			colorTargetStates,
			base.getDepthStencilState(),
			base.getPolygonMode(),
			base.isCull(),
			base.getVertexFormatBindings(),
			base.getPrimitiveTopology(),
			base.getSortKey()
		);
	}

	public static RenderPipeline of(RenderPipeline base) {
		if (base instanceof GlowtoneEmissivePipeline) return base;
		return TWINS.computeIfAbsent(base, GlowtoneEmissivePipeline::create);
	}

	private static RenderPipeline create(RenderPipeline base) {
		final ColorTargetState[] original = base.getColorTargetStates();
		if (original.length != 1) return base;

		final ColorTargetState primary = original[0] != null ? original[0] : ColorTargetState.DEFAULT;
		final boolean writesEmissive = EmissiveShaderPatcher.isLitShader(base.getFragmentShader())
			|| SELF_LIT_PIPELINES.contains(base.getLocation());
		final ColorTargetState emissive = writesEmissive
			? primary
			: new ColorTargetState(primary.blendFunction(), primary.format(), ColorTargetState.WRITE_NONE);
		return new GlowtoneEmissivePipeline(base, new ColorTargetState[]{primary, emissive});
	}
}
