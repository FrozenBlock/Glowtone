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

import java.util.ArrayList;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.frozenblock.glowtone.resources.metadata.EmissiveMetadataSection;
import net.minecraft.client.resources.model.AtlasManager;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(AtlasManager.AtlasConfig.class)
public class AtlasConfigMixin {

	@Mutable
	@Shadow
	@Final
	Set<MetadataSectionType<?>> additionalMetadata;

	@Inject(
		method = "<init>(Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/resources/ResourceLocation;ZLjava/util/Set;)V",
		at = @At("TAIL")
	)
	private void glowtone$addEmissiveMetadataSection(CallbackInfo info) {
		final ArrayList<MetadataSectionType<?>> sections = new ArrayList<>(this.additionalMetadata);
		sections.add(EmissiveMetadataSection.TYPE);
		this.additionalMetadata = Set.copyOf(sections);
	}

}
