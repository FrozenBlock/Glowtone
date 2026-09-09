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

package net.frozenblock.glowtone.render.vertex;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.frozenblock.glowtone.mixin.client.vertex.DefaultVertexFormatAccessor;
import net.frozenblock.glowtone.mixin.client.vertex.LevelRendererAccessor;
import net.frozenblock.glowtone.platform.GlowtonePlatform;
import net.frozenblock.glowtone.render.sodium.vertex.GTSodiumVertexFormat;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.slf4j.Logger;

@ClientOnly
public final class GlowtoneVertexFormats {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final boolean SODIUM = GlowtonePlatform.INSTANCE.isModLoaded("sodium");
	private static final Set<RenderPipeline> PIPELINES = Collections.newSetFromMap(new WeakHashMap<>());

	public static void track(RenderPipeline pipeline) {
		synchronized (PIPELINES) {
			PIPELINES.add(pipeline);
		}
	}

	public static void reportStartup(String name, VertexFormat format, GlowtoneVertexFeatures features) {
		LOGGER.info("Glowtone {} vertex format is {} bytes for {}", name, format.getVertexSize(), features);
	}

	public static boolean reloadNeeded() {
		return !GlowtoneVertexFeatures.current().equals(GlowtoneVertexFeatures.applied());
	}

	public static void apply(GlowtoneVertexFeatures features) {
		if (features.equals(GlowtoneVertexFeatures.applied())) return;

		final Map<VertexFormat, VertexFormat> replaced = new IdentityHashMap<>();
		final VertexFormat block = DefaultVertexFormat.BLOCK;
		final VertexFormat entity = DefaultVertexFormat.ENTITY;
		final VertexFormat tinted = GTDefaultVertexFormat.tinted();
		final VertexFormat newBlock = GTDefaultVertexFormat.rebuildBlock(block, features);
		final VertexFormat newEntity = GTDefaultVertexFormat.rebuildEntity(entity, features);
		final VertexFormat newTinted = GTDefaultVertexFormat.rebuildTinted(features);
		DefaultVertexFormatAccessor.glowtone$setBlock(newBlock);
		DefaultVertexFormatAccessor.glowtone$setEntity(newEntity);
		replaced.put(block, newBlock);
		replaced.put(entity, newEntity);
		replaced.put(tinted, newTinted);

		String terrain = "";
		if (SODIUM) {
			final VertexFormat before = GTSodiumVertexFormat.rebuild(features, replaced);
			terrain = ", sodium terrain " + before.getVertexSize() + " -> " + replaced.get(before).getVertexSize();
		}

		rebind(replaced);
		GlowtoneVertexFeatures.markApplied(features);
		LOGGER.info(
			"Glowtone vertex formats rebuilt for {}: block {} -> {} bytes, entity {} -> {}{}",
			features, block.getVertexSize(), newBlock.getVertexSize(), entity.getVertexSize(), newEntity.getVertexSize(), terrain
		);

		invalidateGeometry();
	}

	private static void rebind(Map<VertexFormat, VertexFormat> replaced) {
		final Set<RenderPipeline> pipelines = Collections.newSetFromMap(new IdentityHashMap<>());
		synchronized (PIPELINES) {
			pipelines.addAll(PIPELINES);
		}
		pipelines.addAll(RenderPipelines.getStaticPipelines());

		for (RenderPipeline pipeline : pipelines) {
			final VertexFormat[] bindings = pipeline.getVertexFormatBindings();
			for (int index = 0; index < bindings.length; index++) {
				final VertexFormat rebuilt = replaced.get(bindings[index]);
				if (rebuilt != null) bindings[index] = rebuilt;
			}
		}
	}

	private static void invalidateGeometry() {
		final Minecraft minecraft = Minecraft.getInstance();
		if (minecraft == null || minecraft.level == null) return;

		final LevelRenderer renderer = minecraft.levelRenderer;
		final SectionRenderDispatcher stale = renderer.sectionRenderDispatcher();
		((LevelRendererAccessor) renderer).glowtone$setSectionRenderDispatcher(null);
		minecraft.levelExtractor.allChanged();
		renderer.invalidateCompiledGeometry(minecraft.level, minecraft.options, minecraft.gameRenderer.mainCamera(), minecraft.getBlockColors());
		if (stale != null) stale.dispose();
	}

	public static VertexFormat.Builder base(VertexFormat current, Set<String> glowtoneNames) {
		final VertexFormat.Builder builder = VertexFormat.builder(current.getStepRate());
		for (VertexFormatElement element : current.getElements()) {
			if (!glowtoneNames.contains(element.name())) builder.addAttribute(element.name(), element.format());
		}
		return builder;
	}

	public static VertexFormat verified(VertexFormat current, VertexFormat rebuilt, Set<String> glowtoneNames) {
		final List<String> moved = new ArrayList<>();
		for (VertexFormatElement element : current.getElements()) {
			if (glowtoneNames.contains(element.name())) continue;
			if (!rebuilt.contains(element.name()) || rebuilt.getElement(element.name()).offset() != element.offset()) {
				moved.add(element.name());
			}
		}
		if (!moved.isEmpty()) {
			throw new IllegalStateException("Glowtone moved vertex attributes " + moved + " while rebuilding " + current);
		}
		return rebuilt;
	}

	private GlowtoneVertexFormats() {}
}
