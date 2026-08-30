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

package net.frozenblock.glowtone.material.data;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.renderer.block.dispatch.VariantSelector;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.StateHolder;
import org.slf4j.Logger;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

@ClientOnly
public record BlockMaterialOverrideDispatcher(Optional<MaterialSelectors> materials) {
	private static final Logger LOGGER = LogUtils.getLogger();
	public static final Codec<BlockMaterialOverrideDispatcher> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		MaterialSelectors.CODEC.optionalFieldOf("variants").forGetter(BlockMaterialOverrideDispatcher::materials)
	).apply(instance, BlockMaterialOverrideDispatcher::new));

	public Map<BlockState, Identifier> instantiate(StateDefinition<Block, BlockState> stateDefinition, Supplier<String> source) {
		final Map<BlockState, Identifier> matchedStates = new IdentityHashMap<>();
		this.materials.ifPresent(selectors -> selectors.instantiate(stateDefinition, source, (state, material) -> {
			final Identifier previousValue = matchedStates.put(state, material);
			if (previousValue != null) throw new IllegalArgumentException("Overlapping material override on state: " + state);
		}));
		return matchedStates;
	}

	public record MaterialSelectors(Map<String, Identifier> materials) {
		public static final Codec<MaterialSelectors> CODEC = ExtraCodecs.nonEmptyMap(Codec.unboundedMap(Codec.STRING, Identifier.CODEC))
			.xmap(MaterialSelectors::new, MaterialSelectors::materials);

		public void instantiate(
			StateDefinition<Block, BlockState> stateDefinition,
			Supplier<String> source,
			BiConsumer<BlockState, Identifier> output
		) {
			this.materials.forEach((selectorString, material) -> {
				try {
					final Predicate<StateHolder<Block, BlockState>> selector = VariantSelector.predicate(stateDefinition, selectorString);
					for (BlockState state : stateDefinition.getPossibleStates()) {
						if (selector.test(state)) output.accept(state, material);
					}
				} catch (Exception e) {
					LOGGER.warn("Exception loading block material override: '{}' for variant: '{}': {}", source.get(), selectorString, e.getMessage());
				}
			});
		}
	}
}
