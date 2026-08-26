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

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.frozenblock.glowtone.config.EdgeHighlightOption;
import net.frozenblock.glowtone.config.OcclusionStrengthOption;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(targets = "net/minecraft/client/OptionInstance$OptionInstanceSliderButton")
public class AbstractSliderButtonMixin {
	@Inject(method = "onRelease", at = @At("TAIL"), require = 0)
	private void glowtone$flushOnRelease(MouseButtonEvent event, CallbackInfo info) {
		glowtone$flush();
	}

	@Inject(method = "keyPressed", at = @At("RETURN"), require = 0)
	private void glowtone$flushOnKey(KeyEvent event, CallbackInfoReturnable<Boolean> info) {
		glowtone$flush();
	}

	@org.spongepowered.asm.mixin.Unique
	private static void glowtone$flush() {
		EdgeHighlightOption.flush();
		OcclusionStrengthOption.flush();
	}
}
