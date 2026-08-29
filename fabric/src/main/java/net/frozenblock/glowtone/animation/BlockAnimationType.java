package net.frozenblock.glowtone.animation;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.util.StringRepresentable;

@ClientOnly
public enum BlockAnimationType implements StringRepresentable {
	FOLIAGE(1, "foliage"),
	FIRE(2, "fire"),
	LAVA(3, "lava"),
	WATER(4, "water");
	public static final Codec<BlockAnimationType> CODEC = StringRepresentable.fromEnum(BlockAnimationType::values);
	private final int id;
	private final String name;

	BlockAnimationType(int id, String name) {
		this.id = id;
		this.name = name;
	}

	public int id() {
		return this.id;
	}

	@Override
	public String getSerializedName() {
		return this.name;
	}
}
