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

import java.util.Collection;
import java.util.LinkedHashSet;
import net.frozenblock.glowtone.emissive.EmissiveMetadataSection;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(SpriteLoader.class)
public class SpriteLoaderMixin {
	@ModifyArg(
		method = "loadAndStitch(Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/resources/ResourceLocation;ILjava/util/concurrent/Executor;Ljava/util/Collection;)Ljava/util/concurrent/CompletableFuture;",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/texture/atlas/SpriteResourceLoader;create(Ljava/util/Collection;)Lnet/minecraft/client/renderer/texture/atlas/SpriteResourceLoader;"
		),
		index = 0
	)
	private Collection<MetadataSectionSerializer<?>> glowtone$includeEmissiveMetadata(Collection<MetadataSectionSerializer<?>> sections) {
		final LinkedHashSet<MetadataSectionSerializer<?>> augmented = new LinkedHashSet<>(sections);
		augmented.add(EmissiveMetadataSection.TYPE);
		return augmented;
	}
}
