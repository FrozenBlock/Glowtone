package net.frozenblock.glowtone.platform;

import java.util.ServiceLoader;

public final class GlowtonePlatform {
	public static final CommonPlatform INSTANCE = load(CommonPlatform.class);

	private static <T> T load(Class<T> clazz) {
		return ServiceLoader.load(clazz, clazz.getClassLoader())
			.findFirst()
			.orElseThrow(() -> new RuntimeException("No implementation found for " + clazz.getName()));
	}

	private GlowtonePlatform() {}
}
