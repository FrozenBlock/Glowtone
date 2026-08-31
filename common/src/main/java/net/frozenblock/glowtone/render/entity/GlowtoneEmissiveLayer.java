/*
 * Copyright 2025-2026 FrozenBlock
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

package net.frozenblock.glowtone.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.frozenblock.glowtone.GlowtoneConstants;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jspecify.annotations.Nullable;

@ClientOnly
public class GlowtoneEmissiveLayer<S extends LivingEntityRenderState, M extends EntityModel<? super S>> extends RenderLayer<S, M> {
	private static final Identifier NO_EMISSIVE_TEXTURE = GlowtoneConstants.id("no_emissive_texture");
	private static final Map<Identifier, Identifier> RESOLVED_TEXTURES = new ConcurrentHashMap<>();

	private final LivingEntityRenderer<?, S, M> renderer;

	public GlowtoneEmissiveLayer(LivingEntityRenderer<?, S, M> renderer) {
		super(renderer);
		this.renderer = renderer;
	}

	public static void clearCache() {
		RESOLVED_TEXTURES.clear();
	}

	@Override
	public void submit(
		PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, S state, float yRot, float xRot
	) {
		if (!GlowtoneConstants.GLOWTONE_EMISSIVES) return;

		final Identifier emissiveTexture = glowtoneEmissiveFor(this.renderer.getTextureLocation(state));
		if (emissiveTexture == null) return;

		submitNodeCollector.order(1).submitModel(
			this.getParentModel(),
			state,
			poseStack,
			RenderTypes.eyes(emissiveTexture),
			lightCoords,
			OverlayTexture.NO_OVERLAY,
			state.outlineColor,
			null
		);
	}

	private static @Nullable Identifier glowtoneEmissiveFor(Identifier baseTexture) {
		final Identifier cached = RESOLVED_TEXTURES.get(baseTexture);
		if (cached != null) return cached == NO_EMISSIVE_TEXTURE ? null : cached;

		final ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
		final Identifier candidate = GlowtoneConstants.withEmissiveSuffix(baseTexture);
		final Identifier resolved = resourceManager.getResource(candidate).isPresent() ? candidate : null;

		RESOLVED_TEXTURES.put(baseTexture, resolved == null ? NO_EMISSIVE_TEXTURE : resolved);

		return resolved;
	}
}
