package net.frozenblock.glowtone.animation;

import com.mojang.serialization.Codec;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.util.StringRepresentable;

public enum BlockAnimationType implements StringRepresentable {
	FOLIAGE("foliage", GlowtoneAnimationChunkSectionLayers.GLOWTONE_ANIMATION_FOLIAGE),
	FIRE("fire", GlowtoneAnimationChunkSectionLayers.GLOWTONE_ANIMATION_FIRE);
	public static final Codec<BlockAnimationType> CODEC = StringRepresentable.fromEnum(BlockAnimationType::values);
	private final String name;
	private final ChunkSectionLayer layer;

	BlockAnimationType(String name, ChunkSectionLayer layer) {
		this.name = name;
		this.layer = layer;
	}

	@Override
	public String getSerializedName() {
		return this.name;
	}
}
