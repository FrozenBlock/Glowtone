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

package net.frozenblock.glowtone.light.data;

import com.google.common.collect.Maps;
import net.frozenblock.glowtone.light.data.block.BlockLightProperties;
import net.frozenblock.glowtone.light.data.block.BlockLightPropertiesDispatcher;
import net.frozenblock.glowtone.light.data.block.BlockLightPropertiesGenerator;
import net.frozenblock.glowtone.light.data.block.BlockLightPropertiesGenerators;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.ApiStatus;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

@ClientOnly
public abstract class LightPropertiesProvider implements DataProvider {
	private final PackOutput.PathProvider pathProvider;
	public final String modId;

	public LightPropertiesProvider(PackOutput output, String modId) {
		this.pathProvider = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, BlockLightProperties.RESOURCE_PACK_DIRECTORY_BLOCKS);
		this.modId = modId;
	}

	@ApiStatus.Internal
	public LightPropertiesProvider(PackOutput output) {
		this(output, Identifier.DEFAULT_NAMESPACE);
	}

	public abstract void generateBlockLights(BlockLightPropertiesGenerators blockLights);

	@Override
	public CompletableFuture<?> run(CachedOutput cache) {

		final BlockStateGeneratorCollector blockStateGenerators = new BlockStateGeneratorCollector();
		generateBlockLights(new BlockLightPropertiesGenerators(blockStateGenerators));
		return CompletableFuture.allOf(blockStateGenerators.save(cache, this.pathProvider));
	}

	@Override
	public String getName() {
		return "Light Properties - " + this.modId;
	}

	private static class BlockStateGeneratorCollector implements Consumer<BlockLightPropertiesGenerator> {
		private final Map<Block, BlockLightPropertiesGenerator> generators = new HashMap<>();

		public BlockStateGeneratorCollector() {}

		@Override
		public void accept(BlockLightPropertiesGenerator generator) {
			final Block block = generator.block();
			final BlockLightPropertiesGenerator prev = this.generators.put(block, generator);
			if (prev != null) throw new IllegalStateException("Duplicate blockstate lightColor definition for " + block);
		}

		public CompletableFuture<?> save(CachedOutput cache, PackOutput.PathProvider pathProvider) {
			final Map<Block, BlockLightPropertiesDispatcher> definitions = Maps.transformValues(this.generators, BlockLightPropertiesGenerator::create);
			final Function<Block, Path> pathGetter = block -> pathProvider.json(block.builtInRegistryHolder().key().identifier());
			return DataProvider.saveAll(cache, BlockLightPropertiesDispatcher.CODEC, pathGetter, definitions);
		}
	}
}
