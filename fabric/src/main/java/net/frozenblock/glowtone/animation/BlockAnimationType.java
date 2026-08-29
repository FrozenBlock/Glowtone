package net.frozenblock.glowtone.animation;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.util.StringRepresentable;

@ClientOnly
public enum BlockAnimationType implements StringRepresentable {
	FOLIAGE("foliage", 1, 2D, 2000D),
	FIRE("fire", 2, 2D, 20000D),
	LAVA("lava", 3, 2D, 2000D),
	WATER("water", 4, 4D, 1000D);
	public static final Codec<BlockAnimationType> CODEC = StringRepresentable.fromEnum(BlockAnimationType::values);
	private final String name;
	private final int animationId;
	private final double positionScale;
	private final double animationTimeScale;

	BlockAnimationType(String name, int animationId, double positionDividend, double animationTimeScale) {
		this.name = name;
		this.animationId = animationId;
		this.positionScale = 1D / positionDividend;
		this.animationTimeScale = animationTimeScale;
	}

	public int animationId() {
		return this.animationId;
	}

	public double positionScale() {
		return this.positionScale;
	}

	public double animationTimeScale() {
		return this.animationTimeScale;
	}

	@Override
	public String getSerializedName() {
		return this.name;
	}
}
