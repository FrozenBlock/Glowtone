package net.frozenblock.glowtone.light.data.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.frozenblock.lib.block.api.attachment.BlockAttachmentKey;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.util.ARGB;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.lighting.LightEngine;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import java.util.Map;
import java.util.Optional;

@ClientOnly
public record BlockLightProperties(
	Optional<Integer> lightColor,
	Optional<Integer> lightFilterColor,
	AmbientOcclusion ambientOcclusion,
	Emissive emissive
) {
	public static final String RESOURCE_PACK_DIRECTORY_BLOCKS = "glowtone/block_light_properties";
	static final BlockAttachmentKey<Baked> ATTACHMENT_KEY = BlockAttachmentKey.create(true, () -> "Block Light Properties");

	public record Emissive(Optional<Integer> brightness, Optional<Boolean> bloom) {
		public static final Emissive AUTOMATIC = new Emissive(Optional.empty(), Optional.empty());

		public static final Codec<Emissive> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			ExtraCodecs.intRange(0, LightEngine.MAX_LEVEL).optionalFieldOf("brightness").forGetter(Emissive::brightness),
			Codec.BOOL.optionalFieldOf("bloom").forGetter(Emissive::bloom)
		).apply(instance, Emissive::new));

		public boolean overrides() {
			return this.brightness.isPresent() || this.bloom.isPresent();
		}
	}

	public record AmbientOcclusion(Optional<Boolean> self, Optional<Boolean> cast) {
		public static final AmbientOcclusion AUTOMATIC = new AmbientOcclusion(Optional.empty(), Optional.empty());

		public static final Codec<AmbientOcclusion> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.BOOL.optionalFieldOf("self").forGetter(AmbientOcclusion::self),
			Codec.BOOL.optionalFieldOf("cast").forGetter(AmbientOcclusion::cast)
		).apply(instance, AmbientOcclusion::new));

		public boolean overrides() {
			return this.self.isPresent() || this.cast.isPresent();
		}
	}

	public static final BlockLightProperties NONE = new BlockLightProperties(
		Optional.empty(), Optional.empty(), AmbientOcclusion.AUTOMATIC, Emissive.AUTOMATIC
	);
	public static final Simple EMPTY = new Simple(NONE);

	public static final MapCodec<BlockLightProperties> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
		Codec.INT.optionalFieldOf("light_color").forGetter(BlockLightProperties::lightColor),
		Codec.INT.optionalFieldOf("light_filter_color").forGetter(BlockLightProperties::lightFilterColor),
		AmbientOcclusion.CODEC.optionalFieldOf("ambient_occlusion", AmbientOcclusion.AUTOMATIC).forGetter(BlockLightProperties::ambientOcclusion),
		Emissive.CODEC.optionalFieldOf("emissive", Emissive.AUTOMATIC).forGetter(BlockLightProperties::emissive)
	).apply(instance, BlockLightProperties::createWithFixedColors));
	public static final Codec<BlockLightProperties> CODEC = MAP_CODEC.codec();

	private static BlockLightProperties createWithFixedColors(
		Optional<Integer> lightColor,
		Optional<Integer> lightFilterColor,
		AmbientOcclusion ambientOcclusion,
		Emissive emissive
	) {
		return new BlockLightProperties(
			lightColor.map(ARGB::transparent),
			lightFilterColor.map(ARGB::transparent),
			ambientOcclusion,
			emissive
		);
	}

	private static volatile boolean anyOcclusionScales;
	private static volatile boolean anyEmissive;

	private static final ThreadLocal<BlockLightProperties[]> RENDERED = ThreadLocal.withInitial(() -> new BlockLightProperties[]{NONE});

	public static BlockLightProperties forBlockState(BlockState state) {
		final Baked baked = state.getBlock().frozenLib$getAttachedOrDefault(ATTACHMENT_KEY, EMPTY);
		return baked == null ? NONE : baked.get(state);
	}

	public static boolean anyOcclusionScales() {
		return anyOcclusionScales;
	}

	public static boolean anyEmissive() {
		return anyEmissive;
	}

	public static void beginBlock(BlockState state) {
		if (!anyEmissive) return;
		RENDERED.get()[0] = forBlockState(state);
	}

	public static void endBlock() {
		if (!anyEmissive) return;
		RENDERED.get()[0] = NONE;
	}

	public static int renderBrightness(int baked) {
		return anyEmissive ? RENDERED.get()[0].emissive().brightness().orElse(baked) : baked;
	}

	public static boolean bloom(boolean baked) {
		return anyEmissive ? RENDERED.get()[0].emissive().bloom().orElse(baked) : baked;
	}

	static void setLoadedFeatures(boolean occlusionScales, boolean emissive) {
		anyOcclusionScales = occlusionScales;
		anyEmissive = emissive;
	}

	public boolean overridesOcclusion() {
		return this.ambientOcclusion.overrides();
	}

	public boolean overridesEmissive() {
		return this.emissive.overrides();
	}

	public static BlockLightProperties color(int color) {
		return NONE.withColor(color);
	}

	public static BlockLightProperties color(int red, int green, int blue) {
		return color(ARGB.color(0, red, green, blue));
	}

	public static BlockLightProperties filterColor(int color) {
		return NONE.withFilterColor(color);
	}

	public static BlockLightProperties filterColor(int red, int green, int blue) {
		return filterColor(ARGB.color(0, red, green, blue));
	}

	public static BlockLightProperties lightAndFilterColor(int lightColor, int filterColor) {
		return NONE.withColor(lightColor).withFilterColor(filterColor);
	}

	public static BlockLightProperties lightAndFilterColor(int lightRed, int lightGreen, int lightBlue, int filterRed, int filterGreen, int filterBlue) {
		return lightAndFilterColor(ARGB.color(0, lightRed, lightGreen, lightBlue), ARGB.color(0, filterRed, filterGreen, filterBlue));
	}

	public static BlockLightProperties occlusion(@Nullable Boolean self, @Nullable Boolean cast) {
		return NONE.withOcclusion(self, cast);
	}

	public BlockLightProperties withColor(int color) {
		return new BlockLightProperties(
			Optional.of(ARGB.transparent(color)), this.lightFilterColor, this.ambientOcclusion, this.emissive
		);
	}

	public BlockLightProperties withFilterColor(int color) {
		return new BlockLightProperties(
			this.lightColor, Optional.of(ARGB.transparent(color)), this.ambientOcclusion, this.emissive
		);
	}

	public BlockLightProperties withOcclusion(@Nullable Boolean self, @Nullable Boolean cast) {
		return new BlockLightProperties(
			this.lightColor,
			this.lightFilterColor,
			new AmbientOcclusion(Optional.ofNullable(self), Optional.ofNullable(cast)),
			this.emissive
		);
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
			return this.map.getOrDefault(state, NONE);
		}
	}
}
