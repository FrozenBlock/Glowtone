package net.frozenblock.glowtone.light.color.engine;

import net.mehvahdjukaar.candlelight.api.ClientOnly;
import java.util.Objects;

@ClientOnly
public record ColorAndBrightness(int color, int brightness) {

	@Override
	public boolean equals(Object o) {
		if (o == null || !(o instanceof ColorAndBrightness other)) return false;
		return this.color == other.color && this.brightness == other.brightness;
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.color, this.brightness);
	}
}
