package net.frozenblock.glowtone.light.color.data;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.Property;
import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
public class BlockLightGenerators {
	public final Consumer<BlockLightGenerator> blockStateOutput;

	public BlockLightGenerators(Consumer<BlockLightGenerator> blockStateOutput) {
		this.blockStateOutput = blockStateOutput;
	}

	public static MultiVariantGenerator createSimpleBlock(Block block, BlockLight light) {
		return MultiVariantGenerator.dispatch(block, light);
	}

	public void createTrivialBlock(BlockLight light, Block... blocks) {
		for (Block block : blocks) this.blockStateOutput.accept(createSimpleBlock(block, light));
	}

	public static <T1 extends Comparable<T1>> PropertyDispatch.C1<BlockLight, T1> initial(Property<T1> property1) {
		return new PropertyDispatch.C1<>(property1);
	}

	public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>> PropertyDispatch.C2<BlockLight, T1, T2> initial(Property<T1> property1, Property<T2> property2) {
		return new PropertyDispatch.C2<>(property1, property2);
	}

	public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>> PropertyDispatch.C3<BlockLight, T1, T2, T3> initial(
		Property<T1> property1,
		Property<T2> property2,
		Property<T3> property3
	) {
		return new PropertyDispatch.C3<>(property1, property2, property3);
	}

	public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>, T4 extends Comparable<T4>> PropertyDispatch.C4<BlockLight, T1, T2, T3, T4> initial(
		Property<T1> property1,
		Property<T2> property2,
		Property<T3> property3,
		Property<T4> property4
	) {
		return new PropertyDispatch.C4<>(property1, property2, property3, property4);
	}

	public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>, T4 extends Comparable<T4>, T5 extends Comparable<T5>> PropertyDispatch.C5<BlockLight, T1, T2, T3, T4, T5> initial(
		Property<T1> property1,
		Property<T2> property2,
		Property<T3> property3,
		Property<T4> property4,
		Property<T5> property5
	) {
		return new PropertyDispatch.C5<>(property1, property2, property3, property4, property5);
	}
}
