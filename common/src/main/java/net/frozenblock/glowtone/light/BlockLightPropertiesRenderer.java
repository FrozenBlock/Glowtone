package net.frozenblock.glowtone.light;

import net.frozenblock.glowtone.light.data.block.BlockLightProperties;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.world.level.block.state.BlockState;

@ClientOnly
public class BlockLightPropertiesRenderer {
	private static final ThreadLocal<BlockLightProperties[]> RENDERED = ThreadLocal.withInitial(() -> new BlockLightProperties[]{BlockLightProperties.NONE});
	private static volatile boolean anyOcclusionScales;
	private static volatile boolean anyEmissive;

	public static boolean anyOcclusionScales() {
		return anyOcclusionScales;
	}

	public static boolean anyEmissive() {
		return anyEmissive;
	}

	public static void beginBlock(BlockState state) {
		if (!anyEmissive) return;
		RENDERED.get()[0] = BlockLightProperties.forBlockState(state);
	}

	public static void endBlock() {
		if (!anyEmissive) return;
		RENDERED.get()[0] = BlockLightProperties.NONE;
	}

	public static int renderBrightness(int baked) {
		return anyEmissive ? RENDERED.get()[0].emissive().brightness().orElse(baked) : baked;
	}

	public static boolean bloom(boolean baked) {
		return anyEmissive ? RENDERED.get()[0].emissive().bloom().orElse(baked) : baked;
	}

	public static void setLoadedFeatures(boolean occlusionScales, boolean emissive) {
		anyOcclusionScales = occlusionScales;
		anyEmissive = emissive;
	}
}
