package net.frozenblock.glowtone.mixin.client.colour.data;

import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.blockstates.PropertyValueList;
import net.minecraft.world.level.block.state.properties.Property;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import java.util.List;
import java.util.Map;

@ClientOnly
@Mixin(PropertyDispatch.class)
public interface PropertyDispatchAccessor<V> {

	@Invoker("getDefinedProperties")
	List<Property<?>> glowtone$getDefinedProperties();

	@Invoker("getEntries")
	Map<PropertyValueList, V> glowtone$getEntries();
}
