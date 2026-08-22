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
import net.frozenblock.glowtone.config.ColouredLightingOption;
import net.frozenblock.glowtone.config.EmissivesOption;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.options.VideoSettingsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin(VideoSettingsScreen.class)
public class VideoSettingsScreenMixin {

	@ModifyReturnValue(method = "qualityOptions", at = @At("RETURN"))
	private static OptionInstance<?>[] glowtone$addOptions(OptionInstance<?>[] original, Options options) {
		final List<OptionInstance<?>> afterSmoothLighting = List.of(ColouredLightingOption.get());
		final List<OptionInstance<?>> afterTransparency = List.of(EmissivesOption.get(), BloomOption.get());
		final ArrayList<OptionInstance<?>> withGlowtone = new ArrayList<>(
			original.length + afterSmoothLighting.size() + afterTransparency.size()
		);
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
