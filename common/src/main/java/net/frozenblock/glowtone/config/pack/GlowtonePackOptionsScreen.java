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

package net.frozenblock.glowtone.config.pack;

import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

@ClientOnly
public final class GlowtonePackOptionsScreen extends OptionsSubScreen {
	public static final String TITLE = "options.glowtone.packs";
	private final @Nullable String packId;

	public GlowtonePackOptionsScreen(@Nullable Screen parent, @Nullable String packId) {
		super(
			parent,
			Minecraft.getInstance().options,
			packId != null ? GlowtonePackOptions.title(packId) : Component.translatable(TITLE)
		);
		this.packId = packId;
	}

	@Override
	protected void addOptions() {
		if (GlowtonePackOptions.addTo(this.list, this.packId)) return;

		final String failure = GlowtonePackOptions.failure(this.packId);
		this.list.addHeader(failure == null
			? Component.translatable(TITLE + ".none")
			: Component.translatable(TITLE + ".broken", failure).withStyle(ChatFormatting.RED));
	}
}
