package net.frozenblock.glowtone.light.color.engine;

import java.util.Map;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.world.level.chunk.DataLayer;

@ClientOnly
public interface ColoredDataLayer {

	default Map<ColorAndBrightness, Integer>[] glowtone$getColors() {
		throw new AssertionError();
	}

	default int glowtone$getColors(int x, int y, int z) {
		return this.glowtone$getColors(DataLayer.getIndex(x, y, z));
	}

	private Map<ColorAndBrightness, Integer> glowtone$getColors(int index) {
		final Map<ColorAndBrightness, Integer>[] colors = glowtone$getColors();
		if (colors == null) return Map.of();

		int position = DataLayer.getByteIndex(index);
		return colors[position];
	}

	default void glowtone$addColor(int x, int y, int z, int brightness, int color) {
		this.glowtone$addColor(DataLayer.getIndex(x, y, z), brightness, color);
	}

	private void glowtone$addColor(int index, int brightness, int color) {
		final Map<ColorAndBrightness, Integer>[] colors = glowtone$getColors();
		int position = DataLayer.getByteIndex(index);

		Map<ColorAndBrightness, Integer> map = colors[position];
		final ColorAndBrightness key = new ColorAndBrightness(brightness, color);
		if (map == null) {
			map = new Object2IntOpenHashMap<>();
			map.put(key, 1);
		} else {
			map.put(key, map.getOrDefault(key, 0) + 1);
		}
	}

	default void glowtone$removeColor(int x, int y, int z, int brightness, int color) {
		this.glowtone$removeColor(DataLayer.getIndex(x, y, z), brightness, color);
	}

	private void glowtone$removeColor(int index, int brightness, int color) {
		final Map<ColorAndBrightness, Integer>[] colors = glowtone$getColors();
		int position = DataLayer.getByteIndex(index);

		final Map<ColorAndBrightness, Integer> map = colors[position];
		if (map == null) return;

		final ColorAndBrightness key = new ColorAndBrightness(brightness, color);
		final Integer colorCount = map.get(key);
		if (colorCount == null) return;

		final int resultCount = colorCount - 1;
		if (resultCount <= 0) {
			map.remove(key);
			return;
		}
		map.put(key, resultCount);
	}

	default void glowtone$removeAllColors(int x, int y, int z) {
		this.glowtone$removeAllColors(DataLayer.getIndex(x, y, z));
	}

	private void glowtone$removeAllColors(int index) {
		final Map<ColorAndBrightness, Integer>[] colors = glowtone$getColors();
		int position = DataLayer.getByteIndex(index);

		final Map<ColorAndBrightness, Integer> map = colors[position];
		if (map == null) return;

		map.clear();
	}

}
