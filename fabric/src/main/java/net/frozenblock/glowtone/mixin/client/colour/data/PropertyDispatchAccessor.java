package net.frozenblock.glowtone.mixin.client.colour.data;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.blockstates.PropertyValueList;
import net.minecraft.world.level.block.state.properties.Property;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import java.util.List;
import java.util.Map;

@Environment(EnvType.CLIENT)
@Mixin(PropertyDispatch.class)
public interface PropertyDispatchAccessor<V> {

	@Invoker("getDefinedProperties")
	default List<Property<?>> glowtone$getDefinedProperties() {
		throw new AssertionError();
	}

	@Invoker("getEntries")
	default Map<PropertyValueList, V> glowtone$getEntries() {
		throw new AssertionError();
	}
}
