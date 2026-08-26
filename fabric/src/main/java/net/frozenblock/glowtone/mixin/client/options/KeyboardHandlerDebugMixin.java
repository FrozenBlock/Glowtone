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

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.frozenblock.glowtone.bloom.GlowtoneEmissiveShaders;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(KeyboardHandler.class)
public class KeyboardHandlerDebugMixin {
	@Unique
	private static final int GLOWTONE_MODIFIER_KEY = GLFW.GLFW_KEY_F4;

	@Unique
	private static final int GLOWTONE_CHORD_KEY = GLFW.GLFW_KEY_G;

	@Unique
	private static final int GLOWTONE_OCCLUSION_KEY = GLFW.GLFW_KEY_O;

	@Unique
	private static final int GLOWTONE_EDGES_KEY = GLFW.GLFW_KEY_E;

	@Inject(method = "handleDebugKeys", at = @At("RETURN"), require = 0)
	private void glowtone$debugHelp(KeyEvent event, CallbackInfoReturnable<Boolean> info) {
		if (event.key() != GLFW.GLFW_KEY_Q || !info.getReturnValueZ()) return;

		final Minecraft minecraft = Minecraft.getInstance();
		if (minecraft == null) return;

		minecraft.showDebugChat(Component.translatable("glowtone.debug.occlusion.help"));
		minecraft.showDebugChat(Component.translatable("glowtone.debug.edge_colour.help"));
	}

	@Inject(
		method = "keyPress(JILnet/minecraft/client/input/KeyEvent;)V",
		at = @At("HEAD"),
		cancellable = true,
		require = 0
	)
	private void glowtone$debugChord(
		long window, int action, KeyEvent event, CallbackInfo info
	) {
		if (action != GLFW.GLFW_PRESS) return;

		final Minecraft minecraft = Minecraft.getInstance();
		if (minecraft == null || minecraft.getWindow() == null) return;
		if (window != minecraft.getWindow().handle()) return;
		if (!InputConstants.isKeyDown(minecraft.getWindow(), GLOWTONE_MODIFIER_KEY)) return;
		if (!InputConstants.isKeyDown(minecraft.getWindow(), GLOWTONE_CHORD_KEY)) return;

		final String view;
		final boolean on;
		if (event.key() == GLOWTONE_OCCLUSION_KEY) {
			view = "glowtone.debug.occlusion.";
			on = GlowtoneEmissiveShaders.toggleAoDebug();
		} else if (event.key() == GLOWTONE_EDGES_KEY) {
			view = "glowtone.debug.edge_colour.";
			on = GlowtoneEmissiveShaders.toggleEdgeDebugColour();
		} else {
			return;
		}

		minecraft.showDebugChat(Component.translatable(view + (on ? "on" : "off")));
		info.cancel();
	}
}
