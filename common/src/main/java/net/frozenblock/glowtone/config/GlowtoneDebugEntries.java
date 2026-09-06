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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.frozenblock.glowtone.GlowtoneConstants;
import net.frozenblock.glowtone.mixin.client.options.DebugScreenEntriesInvoker;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.debug.DebugEntryNoop;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.components.debug.DebugScreenEntryList;
import net.minecraft.client.gui.components.debug.DebugScreenEntryStatus;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

@ClientOnly
public final class GlowtoneDebugEntries {
	public static final Identifier EDGE_HIGHLIGHT = GlowtoneConstants.id("edge_highlight");
	public static final Identifier AMBIENT_OCCLUSION = GlowtoneConstants.id("ambient_occlusion");
	public static final Identifier EMISSIVE_BUFFER = GlowtoneConstants.id("emissive_buffer");
	private static final List<Identifier> ENTRIES = List.of(EDGE_HIGHLIGHT, AMBIENT_OCCLUSION, EMISSIVE_BUFFER);
	private static final Set<Identifier> NEEDS_RELOAD = Set.of(EDGE_HIGHLIGHT, AMBIENT_OCCLUSION);
	private static final Set<Identifier> ENABLED = Collections.newSetFromMap(new ConcurrentHashMap<>());
	private static boolean registered;
	private static boolean settingOurselves;

	public static synchronized void register() {
		if (registered) return;
		registered = true;

		try {
			for (Identifier entry : ENTRIES) DebugScreenEntriesInvoker.glowtone$register(entry, new DebugEntryNoop());
			seedProfiles();
		} catch (Throwable failure) {
			registered = false;
		}
	}

	private static void seedProfiles() {
		for (Map<Identifier, DebugScreenEntryStatus> profile : DebugScreenEntries.PROFILES.values()) {
			try {
				for (Identifier entry : ENTRIES) profile.putIfAbsent(entry, DebugScreenEntryStatus.NEVER);
			} catch (UnsupportedOperationException immutable) {
				return;
			}
		}
	}

	@Nullable
	private static DebugScreenEntryList list() {
		final Minecraft minecraft = Minecraft.getInstance();
		return minecraft == null ? null : minecraft.debugEntries;
	}

	public static boolean enabled(Identifier id) {
		return ENABLED.contains(id);
	}

	public static boolean toggle(Identifier id) {
		final boolean next = !enabled(id);
		set(id, next);
		mirror(id, next);
		if (NEEDS_RELOAD.contains(id)) GlowtoneReload.request();

		return next;
	}

	private static void set(Identifier id, boolean on) {
		if (on) {
			ENABLED.add(id);
		} else {
			ENABLED.remove(id);
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
		if (!ENTRIES.contains(id)) return;

		final DebugScreenEntryList entries = list();
		if (entries == null) return;

		final boolean now = entries.isCurrentlyEnabled(id);
		if (now == enabled(id)) return;

		set(id, now);
		if (NEEDS_RELOAD.contains(id)) GlowtoneReload.request();
	}

	private GlowtoneDebugEntries() {}
}
