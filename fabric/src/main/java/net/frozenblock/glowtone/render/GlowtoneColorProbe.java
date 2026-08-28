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

package net.frozenblock.glowtone.render;

import net.frozenblock.glowtone.light.GlowtoneRegionFlood;
import net.frozenblock.glowtone.mixin.client.colour.ViewAreaInvoker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.SectionMesh;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.SectionPos;
import org.jspecify.annotations.Nullable;

public final class GlowtoneColorProbe {
	private static final GlowtoneColorProbe INSTANCE = new GlowtoneColorProbe();
	private static final int WHITE_RGB = 0xFFFFFF;
	private static final int CACHE_SLOTS = 8;

	private final long[] cachedSections = new long[CACHE_SLOTS];
	private final short[] @Nullable [] cachedColors = new short[CACHE_SLOTS][];
	private final short[] @Nullable [] cachedSkyHues = new short[CACHE_SLOTS][];
	private final boolean[] cacheValid = new boolean[CACHE_SLOTS];
	private int lastSlot;

	public static GlowtoneColorProbe get() {
		return INSTANCE;
	}

	public void invalidate() {
		java.util.Arrays.fill(this.cacheValid, false);
		java.util.Arrays.fill(this.cachedColors, null);
		java.util.Arrays.fill(this.cachedSkyHues, null);
	}

	public int getPackedLevels(int worldX, int worldY, int worldZ) {
		final int slot = this.cache(worldX, worldY, worldZ);
		short[] colors = this.cachedColors[slot];
		if (colors == null) return 0;

		return colors[GlowtoneRegionFlood.entityCellIndex(worldX, worldY, worldZ)] & 0xFFFF;
	}

	public int getSkyRgb(int worldX, int worldY, int worldZ) {
		final int slot = this.cache(worldX, worldY, worldZ);
		short[] hues = this.cachedSkyHues[slot];
		if (hues == null) return WHITE_RGB;

		return GlowtoneRegionFlood.skyHueToRgb(hues[GlowtoneRegionFlood.entityCellIndex(worldX, worldY, worldZ)] & 0xFFFF);
	}

	private int cache(int worldX, int worldY, int worldZ) {
		final long sectionNode = SectionPos.asLong(worldX >> 4, worldY >> 4, worldZ >> 4);

		final int last = this.lastSlot;
		if (this.cacheValid[last] && this.cachedSections[last] == sectionNode) return last;

		for (int slot = 0; slot < CACHE_SLOTS; slot++) {
			if (this.cacheValid[slot] && this.cachedSections[slot] == sectionNode) {
				this.lastSlot = slot;
				return slot;
			}
		}

		final int slot = Math.floorMod(Long.hashCode(sectionNode), CACHE_SLOTS);
		final GlowtoneSectionColors mesh = lookup(sectionNode);
		this.cachedSections[slot] = sectionNode;
		if (mesh != null) {
			this.cachedColors[slot] = mesh.glowtone$sectionColors();
			this.cachedSkyHues[slot] = mesh.glowtone$sectionSkyHues();
		} else {
			this.cachedColors[slot] = GlowtoneSectionColorStore.colors(sectionNode);
			this.cachedSkyHues[slot] = GlowtoneSectionColorStore.skyHues(sectionNode);
		}
		this.cacheValid[slot] = true;
		this.lastSlot = slot;
		return slot;
	}

	@Nullable
	private static GlowtoneSectionColors lookup(long sectionNode) {
		final Minecraft minecraft = Minecraft.getInstance();
		if (minecraft == null) return null;

		final LevelRenderer levelRenderer = minecraft.levelRenderer;
		if (levelRenderer == null) return null;

		final ViewArea viewArea = levelRenderer.viewArea();
		if (viewArea == null) return null;

		final SectionRenderDispatcher.RenderSection section = ((ViewAreaInvoker) viewArea).glowtone$getRenderSection(sectionNode);
		if (section == null) return null;

		final SectionMesh mesh = section.sectionMesh.get();
		return mesh instanceof GlowtoneSectionColors colors ? colors : null;
	}

	private GlowtoneColorProbe() {}
}
