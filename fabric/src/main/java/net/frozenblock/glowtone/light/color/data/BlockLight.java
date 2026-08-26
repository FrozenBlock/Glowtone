package net.frozenblock.glowtone.light.color.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.block.state.BlockState;
import java.util.Map;
import java.util.Optional;

@Environment(EnvType.CLIENT)
public record BlockLight(Optional<Integer> light, Optional<Integer> transmittance) {
	public static final MapCodec<BlockLight> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
		Codec.INT.optionalFieldOf("light_color").forGetter(BlockLight::light),
		Codec.INT.optionalFieldOf("transmittance").forGetter(BlockLight::transmittance)
	).apply(instance, BlockLight::new));
	public static final Codec<BlockLight> CODEC = MAP_CODEC.codec();

	public static BlockLight light(int color) {
		return new BlockLight(Optional.of(color), Optional.empty());
	}

	public static BlockLight light(int red, int green, int blue) {
		return light(ARGB.color(red, green, blue));
	}

	public static BlockLight transmittance(int color) {
		return new BlockLight(Optional.empty(), Optional.of(color));
	}

	public static BlockLight transmittance(int red, int green, int blue) {
		return transmittance(ARGB.color(red, green, blue));
	}

	public static abstract class Baked {
		abstract BlockLight get(BlockState state);
	}

	public static final class Simple extends Baked {
		private final BlockLight blockLight;

		public Simple(BlockLight blockLight) {
			this.blockLight = blockLight;
		}

		@Override
		public BlockLight get(BlockState state) {
			return blockLight;
		}
	}

	public static final class MultiVariant extends Baked {
		private final Map<BlockState, BlockLight> blockLightMap;

		public MultiVariant(Map<BlockState, BlockLight> blockLightMap) {
			this.blockLightMap = blockLightMap;
		}

		@Override
		public BlockLight get(BlockState state) {
			return this.blockLightMap.get(state);
		}
	}
}
