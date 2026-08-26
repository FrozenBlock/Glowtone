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

package net.frozenblock.glowtone.config;

import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.frozenblock.glowtone.mixin.client.options.DebugScreenEntriesInvoker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.debug.DebugEntryNoop;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.components.debug.DebugScreenEntryList;
import net.minecraft.client.gui.components.debug.DebugScreenEntryStatus;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public final class GlowtoneDebugEntries {
	public static final Identifier EDGE_HIGHLIGHTS =
		Identifier.fromNamespaceAndPath("glowtone", "glowtone_highlights");

	public static final Identifier AMBIENT_OCCLUSION =
		Identifier.fromNamespaceAndPath("glowtone", "glowtone_occlusion");

	private static boolean registered;
	private static boolean settingOurselves;
	private static volatile boolean occlusionOn;
	private static volatile boolean edgeOn;

	private GlowtoneDebugEntries() {}

	public static synchronized void register() {
		if (registered) return;
		registered = true;

		try {
			DebugScreenEntriesInvoker.glowtone$register(EDGE_HIGHLIGHTS, new DebugEntryNoop());
			DebugScreenEntriesInvoker.glowtone$register(AMBIENT_OCCLUSION, new DebugEntryNoop());
			seedProfiles();
		} catch (Throwable failure) {
			registered = false;
		}
	}

	private static void seedProfiles() {
		for (Map<Identifier, DebugScreenEntryStatus> profile : DebugScreenEntries.PROFILES.values()) {
			try {
				profile.putIfAbsent(AMBIENT_OCCLUSION, DebugScreenEntryStatus.NEVER);
				profile.putIfAbsent(EDGE_HIGHLIGHTS, DebugScreenEntryStatus.NEVER);
			} catch (UnsupportedOperationException immutable) {
				return;
			}
		}
	}

	private static @Nullable DebugScreenEntryList list() {
		final Minecraft minecraft = Minecraft.getInstance();
		return minecraft == null ? null : minecraft.debugEntries;
	}

	public static boolean enabled(Identifier id) {
		return AMBIENT_OCCLUSION.equals(id) ? occlusionOn : edgeOn;
	}

	public static boolean toggle(Identifier id) {
		final boolean next = !enabled(id);
		set(id, next);
		mirror(id, next);
		GlowtoneReload.request();

		return next;
	}

	private static void set(Identifier id, boolean on) {
		if (AMBIENT_OCCLUSION.equals(id)) {
			occlusionOn = on;
		} else {
			edgeOn = on;
		}
	}

	private static void mirror(Identifier id, boolean on) {
		final DebugScreenEntryList entries = list();
		if (!registered || entries == null) return;

		settingOurselves = true;
		try {
			entries.setStatus(id, on
				? DebugScreenEntryStatus.ALWAYS_ON : DebugScreenEntryStatus.NEVER);
		} finally {
			settingOurselves = false;
		}
		entries.rebuildCurrentList();
	}

	public static void statusChanged(Identifier id) {
		if (settingOurselves || !registered) return;
		if (!AMBIENT_OCCLUSION.equals(id) && !EDGE_HIGHLIGHTS.equals(id)) return;

		final DebugScreenEntryList entries = list();
		if (entries == null) return;

		final boolean now = entries.isCurrentlyEnabled(id);
		if (now == enabled(id)) return;

		set(id, now);
		GlowtoneReload.request();
	}
}
