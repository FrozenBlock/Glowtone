/*
 * Copyright 2025 FrozenBlock
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

package net.frozenblock.glowtone.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import java.util.Arrays;
import com.mojang.blaze3d.platform.NativeImage;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.frozenblock.glowtone.GlowtoneConstants;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.atlas.SpriteResourceLoader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(SpriteResourceLoader.class)
public interface SpriteResourceLoaderMixin {

	@Inject(
		method = "method_52851",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/server/packs/resources/ResourceMetadata;getSection(Lnet/minecraft/server/packs/metadata/MetadataSectionType;)Ljava/util/Optional;"
		),
		cancellable = true
	)
	private static void glowtone$discardEmptyOverlays(
		CallbackInfoReturnable<SpriteContents> info,
		@Local(argsOnly = true) ResourceLocation id,
		@Local NativeImage image
	) {
		if (!GlowtoneConstants.GLOWTONE_EMISSIVES) return;
		if (image == null) return;
		if (!id.getPath().endsWith("_glowtone_emissive")) return;

		try {
			final int[] pixels = image.getPixels();
			if (Arrays.stream(pixels).allMatch(pixel -> ARGB.alpha(pixel) == 0)) info.setReturnValue(null);
		} catch (Exception ignored) {}
	}

}
