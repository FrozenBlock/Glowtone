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

package net.frozenblock.glowtone.light;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.frozenblock.glowtone.light.color.GlowtoneEmitterColors;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;

@Environment(EnvType.CLIENT)
public final class GlowtoneDynamicLights {
	public static final int STRIDE = 5;

	private static final int[] NONE = new int[0];

	private static volatile int[] sources = NONE;
	private static boolean unavailable;
	private static int[] scratch = NONE;
	private static Object[] handles = new Object[0];

	private static Object instance;
	private static Field sourcesField;
	private static Method getX;
	private static Method getY;
	private static Method getZ;
	private static Method getLuminance;
	private static Method dynamicLevelAt;
	private static boolean levelUnavailable;

	private GlowtoneDynamicLights() {}

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(minecraft -> refresh());
	}

	public static int levelAt(BlockPos pos) {
		if (levelUnavailable) return 0;

		try {
			if (dynamicLevelAt == null) {
				final Class<?> lights = Class.forName("dev.lambdaurora.lambdynlights.LambDynLights");
				if (instance == null) instance = lights.getField("INSTANCE").get(null);
				dynamicLevelAt = lights.getMethod("getDynamicLightLevel", BlockPos.class);
			}
			if (instance == null) return 0;
			return (int) ((Number) dynamicLevelAt.invoke(instance, pos)).doubleValue();
		} catch (Throwable failure) {
			levelUnavailable = true;
			return 0;
		}
	}

	public static int luminanceOf(Object entity) {
		if (levelUnavailable) return 0;

		try {
			if (getLuminance == null) {
				getLuminance = Class
					.forName("dev.lambdaurora.lambdynlights.engine.source.EntityDynamicLightSource")
					.getMethod("getLuminance");
			}
			return ((Number) getLuminance.invoke(entity)).intValue();
		} catch (Throwable failure) {
			return 0;
		}
	}

	public static int[] snapshot() {
		return sources;
	}

	public static boolean any() {
		return sources.length != 0;
	}

	public static boolean anyWithin(int minBlockX, int minBlockY, int minBlockZ, int span) {
		final int[] dynamic = sources;
		if (dynamic.length == 0) return false;

		final int maxBlockX = minBlockX + span;
		final int maxBlockY = minBlockY + span;
		final int maxBlockZ = minBlockZ + span;

		for (int index = 0; index < dynamic.length; index += STRIDE) {
			final int x = dynamic[index];
			final int y = dynamic[index + 1];
			final int z = dynamic[index + 2];
			if (x >= minBlockX && x < maxBlockX
				&& y >= minBlockY && y < maxBlockY
				&& z >= minBlockZ && z < maxBlockZ
			) {
				return true;
			}
		}
		return false;
	}

	private static void refresh() {
		if (unavailable) return;

		try {
			if (!bind()) return;

			final Collection<?> live = (Collection<?>) sourcesField.get(instance);
			if (live == null || live.isEmpty()) {
				sources = NONE;
				return;
			}

			final int size = live.size();
			if (handles.length < size) handles = new Object[Math.max(size, 8)];
			final Object[] snapshot = live.toArray(handles);

			final int capacity = size * STRIDE;
			if (scratch.length < capacity) scratch = new int[Math.max(capacity, STRIDE * 8)];
			final int[] packed = scratch;
			int count = 0;

			for (int index = 0; index < size; index++) {
				final Object source = snapshot[index];
				if (source == null) continue;

				final int luminance = ((Number) getLuminance.invoke(source)).intValue();
				if (luminance <= 0) continue;

				final double x = ((Number) getX.invoke(source)).doubleValue();
				final double y = ((Number) getY.invoke(source)).doubleValue();
				final double z = ((Number) getZ.invoke(source)).doubleValue();

				packed[count] = (int) Math.floor(x);
				packed[count + 1] = (int) Math.floor(y);
				packed[count + 2] = (int) Math.floor(z);
				packed[count + 3] = luminance;
				packed[count + 4] = colourOf(source);
				count += STRIDE;
			}

			if (!matches(sources, packed, count)) {
				sources = java.util.Arrays.copyOf(packed, count);
			}
		} catch (Throwable failure) {
			unavailable = true;
			sources = NONE;
		}
	}

	private static boolean matches(int[] published, int[] candidate, int count) {
		if (published.length != count) return false;

		for (int index = 0; index < count; index++) {
			if (published[index] != candidate[index]) return false;
		}
		return true;
	}

	private static boolean bind() throws Exception {
		if (sourcesField != null) return true;

		final Class<?> lights = Class.forName("dev.lambdaurora.lambdynlights.LambDynLights");
		instance = lights.getField("INSTANCE").get(null);
		if (instance == null) return false;

		final Field field = lights.getDeclaredField("dynamicLightSources");
		field.setAccessible(true);

		final Class<?> source =
			Class.forName("dev.lambdaurora.lambdynlights.engine.source.EntityDynamicLightSource");
		getX = source.getMethod("getDynamicLightX");
		getY = source.getMethod("getDynamicLightY");
		getZ = source.getMethod("getDynamicLightZ");
		getLuminance = source.getMethod("getLuminance");

		sourcesField = field;
		return true;
	}

	private static int colourOf(Object source) {
		if (!(source instanceof Entity entity)) return GlowtoneEmitterColors.WHITE;

		if (entity instanceof ItemEntity item) return colourOfStack(item.getItem());

		if (entity instanceof LivingEntity living) {
			final int held = colourOfStack(living.getMainHandItem());
			if (held != GlowtoneEmitterColors.WHITE) return held;

			final int offhand = colourOfStack(living.getOffhandItem());
			if (offhand != GlowtoneEmitterColors.WHITE) return offhand;
		}

		return GlowtoneEmitterColors.WHITE;
	}

	private static int colourOfStack(ItemStack stack) {
		if (stack == null || stack.isEmpty()) return GlowtoneEmitterColors.WHITE;

		final Block block = Block.byItem(stack.getItem());
		if (block == null) return GlowtoneEmitterColors.WHITE;

		final int rgb = GlowtoneEmitterColors.rgbFor(block.defaultBlockState());
		return rgb == GlowtoneEmitterColors.NO_COLOUR ? GlowtoneEmitterColors.WHITE : rgb;
	}
}
