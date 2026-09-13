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

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import net.frozenblock.glowtone.GlowtoneConstants;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@ClientOnly
@Mixin(PackRepository.class)
public class PackRepositoryMixin {
	@Unique
	private static final Logger glowtone$LOGGER = LogUtils.getLogger();

	@Unique
	private static final String glowtone$VANILLA = "vanilla";

	@Unique
	private static final String glowtone$PREFIX = GlowtoneConstants.MOD_ID + ":";

	@ModifyReturnValue(method = "rebuildSelected", at = @At("RETURN"))
	private List<Pack> glowtone$liftBuiltinPacksAboveVanilla(List<Pack> selected) {
		final int vanilla = glowtone$indexOfVanilla(selected);
		if (vanilla <= 0) return selected;

		final List<Pack> below = new ArrayList<>();
		for (int index = 0; index < vanilla; index++) {
			final Pack pack = selected.get(index);
			if (pack.getId().startsWith(glowtone$PREFIX)) below.add(pack);
		}
		if (below.isEmpty()) return selected;

		final List<Pack> lifted = new ArrayList<>(selected.size());
		for (Pack pack : selected) {
			if (below.contains(pack)) continue;

			lifted.add(pack);
			if (pack.getId().equals(glowtone$VANILLA)) lifted.addAll(below);
		}

		glowtone$LOGGER.info(
			"Glowtone moved its built-in pack above vanilla; below it, a pack can only ever be overridden: {}",
			below.stream().map(Pack::getId).toList()
		);
		return List.copyOf(lifted);
	}

	@Unique
	private static int glowtone$indexOfVanilla(List<Pack> selected) {
		for (int index = 0; index < selected.size(); index++) {
			if (selected.get(index).getId().equals(glowtone$VANILLA)) return index;
		}
		return -1;
	}
}
