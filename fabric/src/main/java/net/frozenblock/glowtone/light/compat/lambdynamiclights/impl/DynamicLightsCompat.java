package net.frozenblock.glowtone.light.compat.lambdynamiclights.impl;

import dev.lambdaurora.lambdynlights.LambDynLights;
import dev.lambdaurora.lambdynlights.engine.source.DynamicLightSource;
import dev.lambdaurora.lambdynlights.engine.source.EntityDynamicLightSource;
import net.frozenblock.glowtone.light.color.EmitterColorHelper;
import net.frozenblock.glowtone.light.compat.lambdynamiclights.GlowtoneDynamicLights;
import net.frozenblock.lib.event.api.events.client.ClientTickEvents;
import net.frozenblock.lib.platform.ModLoader;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Set;

@ClientOnly
public final class DynamicLightsCompat implements AbstractDynamicLightsCompat {
	private static final int[] NONE = new int[0];
	private volatile int[] sources = NONE;
	private int[] scratch = NONE;
	private DynamicLightSource[] handles = new DynamicLightSource[0];

	@Override
	public void init() {
		ClientTickEvents.END_LEVEL_TICK.register(minecraft -> {
			try {
				tick();
			} catch (NoSuchFieldException | IllegalAccessException e) { // Only throws if in an IDE, otherwise stays silent
				throw new RuntimeException(e);
			}
		});
	}

	@Override
	public int dynamicLightLevelAt(BlockPos pos) {
		return (int) LambDynLights.get().getDynamicLightLevel(pos);
	}

	@Override
	public int luminanceOf(Object entity) {
		// Entities should always be an instance of EntityDynamicLightSource
		return entity instanceof EntityDynamicLightSource dynamicLightSource ? dynamicLightSource.getLuminance() : 0;
	}

	@Override
	public int[] snapshot() {
		return this.sources;
	}

	@Override
	public boolean any() {
		return this.sources.length > 0;
	}

	@Override
	public boolean anyWithin(int minBlockX, int minBlockY, int minBlockZ, int span) {
		if (!this.any()) return false;

		final int[] sources = this.sources;
		final int maxBlockX = minBlockX + span;
		final int maxBlockY = minBlockY + span;
		final int maxBlockZ = minBlockZ + span;

		for (int index = 0; index < sources.length; index += GlowtoneDynamicLights.STRIDE) {
			final int x = sources[index];
			final int y = sources[index + 1];
			final int z = sources[index + 2];
			// FIXME: is only the min but not max being inclusive intentional?
			if (x >= minBlockX && x < maxBlockX
				&& y >= minBlockY && y < maxBlockY
				&& z >= minBlockZ && z < maxBlockZ
			) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean matches(int[] published, int[] candidate, int count) {
		if (published.length != count) return false;

		for (int index = 0; index < count; index++) {
			if (published[index] != candidate[index]) return false;
		}

		return true;
	}

	@Override
	public int colorOf(Object source) {
		if (!(source instanceof Entity entity)) return EmitterColorHelper.WHITE;
		if (entity instanceof ItemEntity item) return this.colorOfItemStack(item.getItem());

		if (entity instanceof LivingEntity livingEntity) {
			final int heldItemStackColor = this.colorOfItemStack(livingEntity.getMainHandItem());
			if (heldItemStackColor != EmitterColorHelper.WHITE) return heldItemStackColor;

			final int offhandItemStackColor = this.colorOfItemStack(livingEntity.getOffhandItem());
			if (offhandItemStackColor != EmitterColorHelper.WHITE) return offhandItemStackColor;
		}

		return EmitterColorHelper.WHITE;
	}

	@Override
	public int colorOfItemStack(ItemStack itemStack) {
		if (itemStack == null || itemStack.isEmpty()) return EmitterColorHelper.WHITE;

		final Block block = Block.byItem(itemStack.getItem());
		if (block == null) return EmitterColorHelper.WHITE;

		final int color = EmitterColorHelper.rgbFor(block.defaultBlockState());
		return color == EmitterColorHelper.NO_COLOUR ? EmitterColorHelper.WHITE : color;
	}

	private void tick() throws NoSuchFieldException, IllegalAccessException {
		try {
			final Field dynamicLightSources = LambDynLights.class.getDeclaredField("dynamicLightSources");
			dynamicLightSources.setAccessible(true);
			final Set<DynamicLightSource> currentLightSources = (Set<DynamicLightSource>) dynamicLightSources.get(LambDynLights.get());

			if (currentLightSources == null || currentLightSources.isEmpty()) {
				this.sources = NONE;
				return;
			}

			final int size = currentLightSources.size();
			if (this.handles.length < size) this.handles = new DynamicLightSource[Math.max(size, 8)];
			final DynamicLightSource[] snapshot = currentLightSources.toArray(this.handles);

			final int capacity = size * GlowtoneDynamicLights.STRIDE;
			if (this.scratch.length < capacity) this.scratch = new int[Math.max(capacity, GlowtoneDynamicLights.STRIDE * 8)];
			final int[] packed = this.scratch;
			int count = 0;

			for (int index = 0; index < size; index++) {
				final DynamicLightSource source = snapshot[index];
				if (source == null) continue;

				if (!(source instanceof EntityDynamicLightSource entityLightSource)) continue;

				final int luminance = entityLightSource.getLuminance();
				if (luminance <= 0) continue;

				final double x = entityLightSource.getDynamicLightX();
				final double y = entityLightSource.getDynamicLightY();
				final double z = entityLightSource.getDynamicLightZ();

				packed[count] = (int) Math.floor(x);
				packed[count + 1] = (int) Math.floor(y);
				packed[count + 2] = (int) Math.floor(z);
				packed[count + 3] = luminance;
				packed[count + 4] = colorOf(source);
				count += GlowtoneDynamicLights.STRIDE;
			}

			if (!matches(this.sources, packed, count)) this.sources = Arrays.copyOf(packed, count);
		} catch (Exception e) {
			if (ModLoader.isDevelopmentEnvironment()) throw e;
		}
	}
}
