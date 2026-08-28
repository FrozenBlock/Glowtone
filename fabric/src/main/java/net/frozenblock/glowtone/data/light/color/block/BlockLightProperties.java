package net.frozenblock.glowtone.data.light.color.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.frozenblock.lib.block.api.attachment.BlockAttachmentKey;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.block.state.BlockState;
import java.util.Map;
import java.util.Optional;

@Environment(EnvType.CLIENT)
public record BlockLightProperties(
	Optional<Integer> lightColor,
	Optional<Integer> lightFilterColor
) {
	public static final String RESOURCE_PACK_DIRECTORY_BLOCKS = "glowtone/block_light_properties";
	static final BlockAttachmentKey<Baked> ATTACHMENT_KEY = BlockAttachmentKey.create(true, () -> "Block Light Properties");
	public static final Simple EMPTY = new Simple(new BlockLightProperties(Optional.empty(), Optional.empty()));
	public static final MapCodec<BlockLightProperties> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
		Codec.INT.optionalFieldOf("light_color").forGetter(BlockLightProperties::lightColor),
		Codec.INT.optionalFieldOf("light_filter_color").forGetter(BlockLightProperties::lightFilterColor)
	).apply(instance, BlockLightProperties::createWithFixedColors));
	public static final Codec<BlockLightProperties> CODEC = MAP_CODEC.codec();

	private static BlockLightProperties createWithFixedColors(Optional<Integer> lightColor, Optional<Integer> lightFilterColor) {
		return new BlockLightProperties(lightColor.map(ARGB::transparent), lightFilterColor.map(ARGB::transparent));
	}

	public static BlockLightProperties forBlockState(BlockState state) {
		return state.getBlock().frozenLib$getAttachedOrDefault(ATTACHMENT_KEY, EMPTY).get(state);
	}

	public static BlockLightProperties color(int color) {
		return new BlockLightProperties(Optional.of(color), Optional.empty());
	}

	public static BlockLightProperties color(int red, int green, int blue) {
		return color(ARGB.color(0, red, green, blue));
	}

	public static BlockLightProperties filterColor(int color) {
		return new BlockLightProperties(Optional.empty(), Optional.of(color));
	}

	public static BlockLightProperties filterColor(int red, int green, int blue) {
		return filterColor(ARGB.color(0, red, green, blue));
	}

	public static BlockLightProperties lightAndFilterColor(int lightColor, int filterColor) {
		return new BlockLightProperties(Optional.of(lightColor), Optional.of(filterColor));
	}

	public static BlockLightProperties lightAndFilterColor(int lightRed, int lightGreen, int lightBlue, int filterRed, int filterGreen, int filterBlue) {
		return lightAndFilterColor(ARGB.color(0, lightRed, lightGreen, lightBlue), ARGB.color(0, filterRed, filterGreen, filterBlue));
	}

	public static abstract class Baked {
		abstract BlockLightProperties get(BlockState state);
	}

	public static final class Simple extends Baked {
		private final BlockLightProperties properties;

		public Simple(BlockLightProperties properties) {
			this.properties = properties;
		}

		@Override
		public BlockLightProperties get(BlockState state) {
			return this.properties;
		}
	}

	public static final class MultiVariant extends Baked {
		private final Map<BlockState, BlockLightProperties> map;

		public MultiVariant(Map<BlockState, BlockLightProperties> map) {
			this.map = map;
		}

		@Override
		public BlockLightProperties get(BlockState state) {
			return this.map.get(state);
		}
	}
}
