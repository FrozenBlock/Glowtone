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

package net.frozenblock.glowtone.mixin.client.pack;

import com.llamalad7.mixinextras.sugar.Local;
import net.frozenblock.glowtone.config.pack.GlowtonePackOptions;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.pack.PackFormat;
import net.minecraft.server.packs.repository.Pack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@ClientOnly
@Mixin(Pack.class)
public class PackMixin {
	@Shadow
	@Final
	private PackLocationInfo location;

	@Inject(
		method = "readPackMetadata",
		at = @At(value = "NEW", target = "net/minecraft/server/packs/repository/Pack$Metadata")
	)
	private static void glowtone$readDeclaredSettings(
		PackLocationInfo location, Pack.ResourcesSupplier resources, PackFormat format, PackType type,
		CallbackInfoReturnable<Pack.Metadata> info,
		@Local PackResources opened
	) {
		if (type == PackType.CLIENT_RESOURCES) GlowtonePackOptions.declare(location.id(), resources, opened);
	}

	@ModifyArg(
		method = "open",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/server/packs/repository/Pack$ResourcesSupplier;openFull(Lnet/minecraft/server/packs/PackLocationInfo;Lnet/minecraft/server/packs/repository/Pack$Metadata;)Lnet/minecraft/server/packs/PackResources;"
		),
		index = 1
	)
	private Pack.Metadata glowtone$mountSettingOverlays(Pack.Metadata metadata) {
		return GlowtonePackOptions.withSubpacks(this.location.id(), metadata);
	}
}
