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

import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LoadingOverlay;

@Environment(EnvType.CLIENT)
public final class GlowtoneReload {
	private static volatile boolean pending;

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(minecraft -> {
			if (pending) request();
		});
	}

	public static boolean request() {
		final Minecraft minecraft = Minecraft.getInstance();
		if (minecraft == null || minecraft.getResourceManager() == null) return false;
		if (minecraft.gui.overlay() instanceof LoadingOverlay) {
			pending = true;
			return false;
		}

		pending = false;
		minecraft.reloadResourcePacks();
		return true;
	}

	private GlowtoneReload() {}
}
