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

package net.frozenblock.glowtone.config.pack;

import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;

@ClientOnly
public final class GlowtonePackSlider extends AbstractSliderButton {
	private final String packId;
	private final GlowtonePackDeclaration.Setting setting;
	private final GlowtonePackDeclaration.Slider slider;

	GlowtonePackSlider(String packId, GlowtonePackDeclaration.Setting setting, GlowtonePackDeclaration.Slider slider) {
		super(0, 0, 150, 20, CommonComponents.EMPTY, 0D);
		this.packId = packId;
		this.setting = setting;
		this.slider = slider;
		this.value = this.fraction(slider.step(GlowtonePackOptions.value(packId, setting)));
		this.updateMessage();
	}

	@Override
	protected void updateMessage() {
		final String value = this.current();
		this.setMessage(CommonComponents.optionNameValue(GlowtonePackOptions.name(this.packId, this.setting), GlowtonePackOptions.valueName(this.packId, this.setting, value)));
		this.setTooltip(GlowtonePackOptions.describe(this.packId, this.setting, value).map(Tooltip::create).orElse(null));
	}

	@Override
	protected void applyValue() {
		GlowtonePackOptions.stage(this.packId, this.setting, this.current());
	}

	@Override
	public void onRelease(MouseButtonEvent event) {
		super.onRelease(event);
		GlowtonePackOptions.commit(this.packId);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (!super.keyPressed(event)) return false;

		GlowtonePackOptions.commit(this.packId);
		return true;
	}

	public void sync() {
		this.value = this.fraction(this.slider.step(GlowtonePackOptions.value(this.packId, this.setting)));
		this.updateMessage();
	}

	private String current() {
		return this.slider.value(Math.round((float) this.value * (this.slider.steps() - 1)));
	}

	private double fraction(int step) {
		return (double) step / (this.slider.steps() - 1);
	}
}
