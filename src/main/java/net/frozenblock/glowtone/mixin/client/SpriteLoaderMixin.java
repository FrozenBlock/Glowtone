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

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.frozenblock.glowtone.resources.metadata.EmissiveMetadataSection;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.ArrayList;
import java.util.Set;

@Environment(EnvType.CLIENT)
@Mixin(SpriteLoader.class)
public class SpriteLoaderMixin {

	@Mutable
	@Shadow
	@Final
	public static Set<MetadataSectionType<?>> DEFAULT_METADATA_SECTIONS;

	@Inject(method = "<clinit>", at = @At("TAIL"))
	private static void glowtone$addEmissiveMetadataSection(CallbackInfo info) {
		ArrayList<MetadataSectionType<?>> sections = new ArrayList<>(DEFAULT_METADATA_SECTIONS);
		sections.add(EmissiveMetadataSection.TYPE);
		DEFAULT_METADATA_SECTIONS = Set.copyOf(sections);
	}

}
