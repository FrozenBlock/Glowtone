package net.frozenblock.glowtone.emissive.entity;

import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.frozenblock.glowtone.GlowtoneConstants;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.jetbrains.annotations.ApiStatus;

@ClientOnly
public final class RenderTypeTextureValidityCache {
	private static final Reloader RELOADER = new Reloader();
	private static final Object2BooleanOpenHashMap<Identifier> TEXTURE_VALIDITY_MAP = new Object2BooleanOpenHashMap<>();

	@ApiStatus.Internal
	public static void init() {
		ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(GlowtoneConstants.id("emissive_render_type_validity"), RELOADER);
	}

	public static boolean getOrComputeValidity(Identifier texture) {
		return TEXTURE_VALIDITY_MAP.computeIfAbsent(
			texture,
			id -> Minecraft.getInstance().getResourceManager().getResource(texture).isPresent()
		);
	}

	private static void clear() {
		TEXTURE_VALIDITY_MAP.clear();
	}

	private static class Reloader implements ResourceManagerReloadListener {
		@Override
		public void onResourceManagerReload(ResourceManager resourceManager) {
			clear();
		}
	}

	private RenderTypeTextureValidityCache() {}
}
