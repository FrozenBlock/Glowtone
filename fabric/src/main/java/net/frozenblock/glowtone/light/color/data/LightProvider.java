package net.frozenblock.glowtone.light.color.data;

import com.google.common.collect.Maps;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
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

@Environment(EnvType.CLIENT)
// TODO: potential neo and fabric datagen support?
public abstract class LightProvider implements DataProvider {
	private final PackOutput.PathProvider pathProvider;
	public final String modId;

	public LightProvider(PackOutput.PathProvider pathProvider, String modId) {
		this.pathProvider = pathProvider;
		this.modId = modId;
	}

	@ApiStatus.Internal
	public LightProvider(PackOutput.PathProvider pathProvider) {
		this(pathProvider, Identifier.DEFAULT_NAMESPACE);
	}

	public abstract void generateBlockLights(BlockLightGenerators blockLights);

	@Override
	public CompletableFuture<?> run(CachedOutput cache) {
		final BlockStateGeneratorCollector blockStateGenerators = new BlockStateGeneratorCollector();
		generateBlockLights(new BlockLightGenerators(blockStateGenerators));
		return CompletableFuture.allOf(blockStateGenerators.save(cache, this.pathProvider));
	}

	@Override
	public String getName() {
		return "Light Definitions - " + this.modId;
	}

	private static class BlockStateGeneratorCollector implements Consumer<BlockLightGenerator> {
		private final Map<Block, BlockLightGenerator> generators = new HashMap<>();

		public BlockStateGeneratorCollector() {}

		@Override
		public void accept(BlockLightGenerator generator) {
			final Block block = generator.block();
			final BlockLightGenerator prev = this.generators.put(block, generator);
			if (prev != null) throw new IllegalStateException("Duplicate blockstate light definition for " + block);
		}

		public CompletableFuture<?> save(CachedOutput cache, PackOutput.PathProvider pathProvider) {
			final Map<Block, BlockLightDispatcher> definitions = Maps.transformValues(this.generators, BlockLightGenerator::create);
			final Function<Block, Path> pathGetter = block -> pathProvider.json(block.builtInRegistryHolder().key().identifier());
			return DataProvider.saveAll(cache, BlockLightDispatcher.CODEC, pathGetter, definitions);
		}
	}
}
