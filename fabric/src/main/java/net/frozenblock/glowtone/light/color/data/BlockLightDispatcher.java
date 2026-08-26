package net.frozenblock.glowtone.light.color.data;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.block.dispatch.VariantSelector;
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

@Environment(EnvType.CLIENT)
public record BlockLightDispatcher(Optional<SimpleLightSelectors> simpleLights) {
	private static final Logger LOGGER = LogUtils.getLogger();
	public static final Codec<BlockLightDispatcher> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		SimpleLightSelectors.CODEC.optionalFieldOf("variants").forGetter(BlockLightDispatcher::simpleLights)
	).apply(instance, BlockLightDispatcher::new));

	public Map<BlockState, BlockLight> instantiate(StateDefinition<Block, BlockState> stateDefinition, Supplier<String> source) {
		final Map<BlockState, BlockLight> matchedStates = new IdentityHashMap<>();
		this.simpleLights.ifPresent(selectors -> selectors.instantiate(stateDefinition, source, (state, blockLight) -> {
			final BlockLight previousValue = matchedStates.put(state, blockLight);
			if (previousValue != null) throw new IllegalArgumentException("Overlapping light definition on state: " + state);
		}));
		return matchedStates;
	}

	public record SimpleLightSelectors(Map<String, BlockLight> lights) {
		public static final Codec<SimpleLightSelectors> CODEC = ExtraCodecs.nonEmptyMap(Codec.unboundedMap(Codec.STRING, BlockLight.CODEC))
			.xmap(SimpleLightSelectors::new, SimpleLightSelectors::lights);

		public void instantiate(
			StateDefinition<Block, BlockState> stateDefinition,
			Supplier<String> source,
			BiConsumer<BlockState, BlockLight> output
		) {
			this.lights.forEach((selectorString, blockLight) -> {
				try {
					final Predicate<StateHolder<Block, BlockState>> selector = VariantSelector.predicate(stateDefinition, selectorString);
					for (BlockState state : stateDefinition.getPossibleStates()) {
						if (selector.test(state)) output.accept(state, blockLight);
					}
				} catch (Exception e) {
					LOGGER.warn("Exception loading blockstate light definition: '{}' for variant: '{}': {}", source.get(), selectorString, e.getMessage());
				}
			});
		}
	}
}
