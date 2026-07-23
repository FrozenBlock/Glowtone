/*
 * Copyright 2025-2026 FrozenBlock
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

import net.frozenblock.glowtone.emissive.EmissiveMetadataSection;
import net.frozenblock.glowtone.emissive.GlowtoneEmissiveSprite;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.server.packs.resources.ResourceMetadata;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SpriteContents.class)
public class SpriteContentsMixin implements GlowtoneEmissiveSprite {
	@Shadow
	@Final
	private ResourceMetadata metadata;

	@Unique
	@Nullable
	private EmissiveMetadataSection glowtone$emissiveMetadata;

	@Inject(method = "<init>", at = @At("TAIL"))
	private void glowtone$captureEmissiveMetadata(CallbackInfo info) {
		this.glowtone$emissiveMetadata = this.metadata.getSection(EmissiveMetadataSection.TYPE).orElse(null);
	}

	@Override
	@Nullable
	public EmissiveMetadataSection glowtone$emissiveMetadata() {
		return this.glowtone$emissiveMetadata;
	}
}
