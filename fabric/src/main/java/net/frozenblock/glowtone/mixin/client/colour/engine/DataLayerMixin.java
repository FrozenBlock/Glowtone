package net.frozenblock.glowtone.mixin.client.colour.engine;

import java.util.Map;
import net.frozenblock.glowtone.light.color.engine.ColorAndBrightness;
import net.frozenblock.glowtone.light.color.engine.ColoredDataLayer;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.world.level.chunk.DataLayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@ClientOnly
@Mixin(DataLayer.class)
public class DataLayerMixin implements ColoredDataLayer {
	@Shadow
	@Final
	public static int SIZE;

	@Unique
	private Map<ColorAndBrightness, Integer>[] glowtone$colors;

	@Override
	public Map<ColorAndBrightness, Integer>[] glowtone$getColors() {
		if (this.glowtone$colors == null) this.glowtone$colors = new Map[SIZE];
		return this.glowtone$colors;
	}

	// TODO: copy method? idk
}
