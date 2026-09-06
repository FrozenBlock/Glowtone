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
import net.frozenblock.glowtone.config.option.animation.SmoothAnimationOption;
import net.frozenblock.glowtone.config.option.ao.AmbientOcclusionMode;
import net.frozenblock.glowtone.config.option.ao.AmbientOcclusionOption;
import net.frozenblock.glowtone.config.option.bloom.BloomOption;
import net.frozenblock.glowtone.config.option.color.ColoredLightingMode;
import net.frozenblock.glowtone.config.option.color.ColoredLightingOption;
import net.frozenblock.glowtone.config.option.edge.EdgeHighlightOption;
import net.frozenblock.glowtone.config.option.ao.OcclusionStrengthOption;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.GraphicsPreset;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@ClientOnly
@Mixin(GraphicsPreset.class)
public class GraphicsPresetMixin {

	@Inject(method = "apply", at = @At("TAIL"))
	private void glowtone$applyColouredLightingPreset(
		Minecraft minecraft,
		CallbackInfo info,
		@Local(name = "screen") @Nullable OptionsSubScreen screen
	) {
		if (screen == null) return;

		final ColoredLightingMode mode = switch (GraphicsPreset.class.cast(this)) {
			case FAST -> ColoredLightingMode.OFF;
			case FANCY, FABULOUS -> ColoredLightingMode.SUBTLE;
			case CUSTOM -> null;
		};
		if (mode == null) return;

		final OptionInstance<ColoredLightingMode> option = ColoredLightingOption.get();
		if (option.get() == mode) return;

		option.set(mode);
		screen.resetOption(option);
	}

	@Inject(method = "apply", at = @At("TAIL"))
	private void glowtone$applyAmbientOcclusionPreset(
		Minecraft minecraft,
		CallbackInfo info,
		@Local(name = "screen") @Nullable OptionsSubScreen screen
	) {
		if (screen == null) return;

		final AmbientOcclusionMode mode = switch (GraphicsPreset.class.cast(this)) {
			case FAST -> AmbientOcclusionMode.FAST;
			case FANCY, FABULOUS -> AmbientOcclusionMode.FANCY;
			case CUSTOM -> null;
		};
		if (mode == null) return;

		final OptionInstance<Integer> strength = OcclusionStrengthOption.get();
		if (strength.get().intValue() != OcclusionStrengthOption.VANILLA) {
			strength.set(OcclusionStrengthOption.VANILLA);
			screen.resetOption(strength);
		}

		final OptionInstance<AmbientOcclusionMode> option = AmbientOcclusionOption.get();
		if (option.get() != mode) {
			option.set(mode);
			screen.resetOption(option);
		}

		OcclusionStrengthOption.flush();
	}

	@Inject(method = "apply", at = @At("TAIL"))
	private void glowtone$applyEdgeHighlightPreset(
		Minecraft minecraft,
		CallbackInfo info,
		@Local(name = "screen") @Nullable OptionsSubScreen screen
	) {
		if (screen == null) return;

		final Integer highlight = switch (GraphicsPreset.class.cast(this)) {
			case FAST -> EdgeHighlightOption.MIN;
			case FANCY, FABULOUS -> EdgeHighlightOption.DEFAULT;
			case CUSTOM -> null;
		};
		if (highlight == null) return;

		final OptionInstance<Integer> option = EdgeHighlightOption.get();
		if (option.get().intValue() == highlight.intValue()) return;

		option.set(highlight);
		screen.resetOption(option);
		EdgeHighlightOption.flush();
	}

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
		screen.resetOption(option);
	}

	@Inject(method = "apply", at = @At("TAIL"))
	private void glowtone$applySmoothAnimationPreset(
		Minecraft minecraft,
		CallbackInfo info,
		@Local(name = "screen") @Nullable OptionsSubScreen screen
	) {
		if (screen == null) return;

		final Boolean smoothAnimation = switch (GraphicsPreset.class.cast(this)) {
			case FAST -> false;
			case FANCY, FABULOUS -> SmoothAnimationOption.DEFAULT;
			case CUSTOM -> null;
		};
		if (smoothAnimation == null) return;

		final OptionInstance<Boolean> option = SmoothAnimationOption.get();
		if (option.get().booleanValue() == smoothAnimation.booleanValue()) return;

		option.set(smoothAnimation);
		screen.resetOption(option);
	}
}
