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

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.frozenblock.glowtone.config.BloomOption;
import net.frozenblock.glowtone.config.AmbientOcclusionOption;
import net.frozenblock.glowtone.config.ColouredLightingOption;
import net.frozenblock.glowtone.config.OcclusionStrengthOption;
import net.frozenblock.glowtone.config.EdgeHighlightOption;
import net.frozenblock.glowtone.config.ShadingOption;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.screens.options.VideoSettingsScreen;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(VideoSettingsScreen.class)
public class VideoSettingsScreenMixin {
	@Unique
	@Nullable
	private Boolean glowtone$smoothLighting;

	@Inject(method = "tick", at = @At("TAIL"))
	private void glowtone$syncOcclusionLocks(CallbackInfo info) {
		final OptionsList list = ((OptionsSubScreenAccessor) this).glowtone$list();
		if (list == null) return;

		glowtone$setActive(list, AmbientOcclusionOption.get(), AmbientOcclusionOption.available());
		glowtone$setActive(list, OcclusionStrengthOption.get(), OcclusionStrengthOption.available());

		final boolean smooth = AmbientOcclusionOption.smoothLightingEnabled();
		if (this.glowtone$smoothLighting != null && this.glowtone$smoothLighting != smooth) {
			AmbientOcclusionOption.rebuildFromScreen();
		}
		this.glowtone$smoothLighting = smooth;
	}

	@Unique
	private static void glowtone$setActive(OptionsList list, OptionInstance<?> option, boolean active) {
		final AbstractWidget widget = list.findOption(option);
		if (widget != null && widget.active != active) widget.active = active;
	}

	@ModifyReturnValue(method = "qualityOptions", at = @At("RETURN"))
	private static OptionInstance<?>[] glowtone$addOptions(OptionInstance<?>[] original, Options options) {
		final List<OptionInstance<?>> afterSmoothLighting = List.of(AmbientOcclusionOption.get(), OcclusionStrengthOption.get(), ColouredLightingOption.get());
		final List<OptionInstance<?>> afterTransparency = List.of(ShadingOption.get(), BloomOption.get(), EdgeHighlightOption.get());
		final ArrayList<OptionInstance<?>> withGlowtone = new ArrayList<>(original.length + afterSmoothLighting.size() + afterTransparency.size());
		boolean placedColour = false;
		boolean placedRest = false;

		for (OptionInstance<?> option : original) {
			withGlowtone.add(option);
			if (!placedColour && option == options.ambientOcclusion()) {
				withGlowtone.addAll(afterSmoothLighting);
				placedColour = true;
			} else if (!placedRest && option == options.improvedTransparency()) {
				withGlowtone.addAll(afterTransparency);
				placedRest = true;
			}
		}
		if (!placedColour) withGlowtone.addAll(afterSmoothLighting);
		if (!placedRest) withGlowtone.addAll(afterTransparency);

		return withGlowtone.toArray(OptionInstance<?>[]::new);
	}
}
