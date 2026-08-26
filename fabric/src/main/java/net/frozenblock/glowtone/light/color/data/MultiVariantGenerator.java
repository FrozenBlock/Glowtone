package net.frozenblock.glowtone.light.color.data;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.frozenblock.glowtone.mixin.client.colour.data.PropertyDispatchAccessor;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.blockstates.PropertyValueList;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.Property;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Environment(EnvType.CLIENT)
public final class MultiVariantGenerator implements BlockLightGenerator {
	private final Block block;
	private final List<MultiVariantGenerator.Entry> entries;
	private final Set<Property<?>> seenProperties;

	private MultiVariantGenerator(Block block, List<MultiVariantGenerator.Entry> entries, Set<Property<?>> seenProperties) {
		this.block = block;
		this.entries = entries;
		this.seenProperties = seenProperties;
	}

	private static Set<Property<?>> validateAndExpandProperties(Set<Property<?>> seenProperties, Block block, PropertyDispatch<?> generator) {
		final List<Property<?>> addedProperties = ((PropertyDispatchAccessor) generator).glowtone$getDefinedProperties();
		addedProperties.forEach((property) -> {
			if (block.getStateDefinition().getProperty(property.getName()) != property) {
				throw new IllegalStateException("Property " + property + " is not defined for block " + block);
			} else if (seenProperties.contains(property)) {
				throw new IllegalStateException("Values of property " + property + " already defined for block " + block);
			}
		});

		final Set<Property<?>> newSeenProperties = new HashSet<>(seenProperties);
		newSeenProperties.addAll(addedProperties);
		return newSeenProperties;
	}

	@Override
	public BlockLightDispatcher create() {
		final Map<String, BlockLight> variants = new HashMap<>();
		for (Entry entry : this.entries) variants.put(entry.properties.getKey(), entry.variant);
		return new BlockLightDispatcher(Optional.of(new BlockLightDispatcher.SimpleLightSelectors(variants)));
	}

	@Override
	public Block block() {
		return this.block;
	}

	public static MultiVariantGenerator.Empty dispatch(Block block) {
		return new MultiVariantGenerator.Empty(block);
	}

	public static MultiVariantGenerator dispatch(Block block, BlockLight initialLight) {
		return new MultiVariantGenerator(block, List.of(new MultiVariantGenerator.Entry(PropertyValueList.EMPTY, initialLight)), Set.of());
	}

	public static class Empty {
		private final Block block;

		public Empty(Block block) {
			this.block = block;
		}

		public MultiVariantGenerator with(PropertyDispatch<BlockLight> newStage) {
			final Set<Property<?>> newSeenProperties = MultiVariantGenerator.validateAndExpandProperties(Set.of(), this.block, newStage);
			List<MultiVariantGenerator.Entry> newEntries = ((PropertyDispatchAccessor<BlockLight>) newStage).glowtone$getEntries().entrySet()
				.stream()
				.map(entry -> new MultiVariantGenerator.Entry(entry.getKey(), entry.getValue()))
				.toList();
			return new MultiVariantGenerator(this.block, newEntries, newSeenProperties);
		}
	}

	private record Entry(PropertyValueList properties, BlockLight variant) {}
}
