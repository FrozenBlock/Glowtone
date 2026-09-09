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

package net.frozenblock.glowtone.render.vertex;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import org.jspecify.annotations.Nullable;

@ClientOnly
public record GlowtoneVertexLayout(int vertexSize, int chroma, int skyChroma, int edge, int edgeMask, int contact0, int flags) {
	public static final int ABSENT = -1;
	public static final GlowtoneVertexLayout NONE = new GlowtoneVertexLayout(0, ABSENT, ABSENT, ABSENT, ABSENT, ABSENT, ABSENT);

	public record Names(
		String chroma, String skyChroma,
		String edge, String edgeMask,
		String contact0, String contact1, String contact2, String contact3,
		@Nullable String flags
	) {}

	public static GlowtoneVertexLayout of(VertexFormat format, Names names) {
		final int contact0 = offsetOf(format, names.contact0());
		if (contact0 != ABSENT) {
			final int contact1 = offsetOf(format, names.contact1());
			final int contact2 = offsetOf(format, names.contact2());
			final int contact3 = offsetOf(format, names.contact3());
			if (contact1 != contact0 + 4 || contact2 != contact0 + 8 || contact3 != contact0 + 12) {
				throw new IllegalStateException("Glowtone contact attributes are not contiguous in " + format);
			}
		}

		return new GlowtoneVertexLayout(
			format.getVertexSize(),
			offsetOf(format, names.chroma()),
			offsetOf(format, names.skyChroma()),
			offsetOf(format, names.edge()),
			offsetOf(format, names.edgeMask()),
			contact0,
			offsetOf(format, names.flags())
		);
	}

	private static int offsetOf(VertexFormat format, @Nullable String name) {
		return name != null && format.contains(name) ? format.getElement(name).offset() : ABSENT;
	}

	public boolean isEmpty() {
		return !this.hasChroma() && !this.hasEdges() && !this.hasFlags();
	}

	public boolean hasChroma() {
		return this.chroma != ABSENT;
	}

	public boolean hasEdges() {
		return this.edge != ABSENT;
	}

	public boolean hasFlags() {
		return this.flags != ABSENT;
	}

	public int contact(int index) {
		return this.contact0 + index * 4;
	}
}
