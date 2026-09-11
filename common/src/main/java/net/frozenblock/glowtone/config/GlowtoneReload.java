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

import net.frozenblock.glowtone.config.pack.GlowtonePackOptions;
import net.frozenblock.glowtone.config.pack.GlowtonePackOptionsScreen;
import net.frozenblock.glowtone.platform.GlowtonePlatform;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.VideoSettingsScreen;

@ClientOnly
public final class GlowtoneReload {
	private static final String SODIUM_SCREEN = "net.caffeinemc.mods.sodium.client.gui.VideoSettingsScreen";
	private static volatile boolean pending;

	public static void register() {
		GlowtonePlatform.INSTANCE.registerOnTickEnd(_ -> {
			if (pending) request();
			GlowtonePackOptions.flush();
		});
	}

	public static boolean request() {
		final Minecraft minecraft = Minecraft.getInstance();
		if (minecraft == null || minecraft.getResourceManager() == null) return false;
		if (minecraft.gui == null || minecraft.gui.overlay() instanceof LoadingOverlay || settingsOpen()) {
			pending = true;
			return false;
		}

		pending = false;
		minecraft.reloadResourcePacks();
		return true;
	}

	public static boolean settingsOpen() {
		final Minecraft minecraft = Minecraft.getInstance();
		if (minecraft == null || minecraft.gui == null) return false;

		final Screen screen = minecraft.gui.screen();
		if (screen == null) return false;
		if (screen instanceof VideoSettingsScreen || screen instanceof GlowtonePackOptionsScreen) return true;
		return screen.getClass().getName().startsWith(SODIUM_SCREEN);
	}

	private GlowtoneReload() {}
}
