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

package net.frozenblock.glowtone.mixin.client.options;

import com.llamalad7.mixinextras.sugar.Local;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.frozenblock.glowtone.config.BloomOption;
import net.minecraft.client.GraphicsPreset;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(GraphicsPreset.class)
public class GraphicsPresetMixin {

	@Inject(method = "apply", at = @At("TAIL"))
	private void glowtone$applyBloomPreset(
		Minecraft minecraft,
		CallbackInfo info,
		@Local(name = "screen") @Nullable OptionsSubScreen screen
	) {
		if (screen == null) return;

		final Integer bloom = switch (GraphicsPreset.class.cast(this)) {
			case FAST -> BloomOption.MIN;
			case FANCY, FABULOUS -> BloomOption.PRESET_DEFAULT;
			case CUSTOM -> null;
		};
		if (bloom == null) return;

		final OptionInstance<Integer> option = BloomOption.get();
		if (option.get().intValue() == bloom.intValue()) return;

		option.set(bloom);
		if (screen != null) screen.resetOption(option);
	}
}
