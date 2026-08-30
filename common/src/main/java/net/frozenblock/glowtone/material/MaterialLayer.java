/*
 * Copyright 2026 FrozenBlock
 * This file is part of Glowtone.
 *
 * This program is free software; you can modify it under
 * the terms of version 1 of the FrozenBlock Modding Oasis License
 * as published by FrozenBlock Modding Oasis.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * FrozenBlock Modding Oasis License for more details.
 *
 * You should have received a copy of the FrozenBlock Modding Oasis License
 * along with this program; if not, see <https://github.com/FrozenBlock/Licenses>.
 */

package net.frozenblock.glowtone.material;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

@ClientOnly
public final class MaterialLayer {
	private static final int CUSTOM = -1;
	private static final int VANILLA_SOLID = 0;
	private static final int VANILLA_CUTOUT = 1;
	private static final int VANILLA_TRANSLUCENT = 2;

	public static final MaterialLayer SOLID = new MaterialLayer(Identifier.withDefaultNamespace("solid"));
	public static final MaterialLayer CUTOUT = new MaterialLayer(Identifier.withDefaultNamespace("cutout"));
	public static final MaterialLayer TRANSLUCENT = new MaterialLayer(Identifier.withDefaultNamespace("translucent"));

	public static final Codec<MaterialLayer> CODEC = Identifier.CODEC.xmap(MaterialLayer::new, MaterialLayer::id);

	private final Identifier id;
	private final int vanillaLayer;

	public MaterialLayer(Identifier id) {
		this.id = id;
		this.vanillaLayer = vanillaLayerFor(id);
	}

	private static int vanillaLayerFor(Identifier id) {
		if (!id.getNamespace().equals(Identifier.DEFAULT_NAMESPACE)) return CUSTOM;

		return switch (id.getPath()) {
			case "solid" -> VANILLA_SOLID;
			case "cutout" -> VANILLA_CUTOUT;
			case "translucent" -> VANILLA_TRANSLUCENT;
			default -> CUSTOM;
		};
	}

	public Identifier id() {
		return this.id;
	}

	public @Nullable ChunkSectionLayer vanilla() {
		return switch (this.vanillaLayer) {
			case VANILLA_SOLID -> ChunkSectionLayer.SOLID;
			case VANILLA_CUTOUT -> ChunkSectionLayer.CUTOUT;
			case VANILLA_TRANSLUCENT -> ChunkSectionLayer.TRANSLUCENT;
			default -> null;
		};
	}

	public boolean custom() {
		return this.vanillaLayer == CUSTOM;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof MaterialLayer layer && this.id.equals(layer.id);
	}

	@Override
	public int hashCode() {
		return this.id.hashCode();
	}

	@Override
	public String toString() {
		return this.id.toString();
	}
}
