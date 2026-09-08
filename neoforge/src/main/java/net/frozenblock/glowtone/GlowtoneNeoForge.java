package net.frozenblock.glowtone;

import net.frozenblock.glowtone.platform.NeoForgePlatform;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(value = GlowtoneConstants.MOD_ID, dist = Dist.CLIENT)
public final class GlowtoneNeoForge {

	public GlowtoneNeoForge(IEventBus modBus) {
		GlowtoneClient.init();
		modBus.addListener(NeoForgePlatform::registerReloadListeners);
		modBus.addListener(NeoForgePlatform::registerResourcePacks);
	}
}
