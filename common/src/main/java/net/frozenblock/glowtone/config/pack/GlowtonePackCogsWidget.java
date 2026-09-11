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

import net.frozenblock.glowtone.GlowtoneConstants;
import net.frozenblock.glowtone.mixin.client.pack.TransferableSelectionListInvoker;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.packs.TransferableSelectionList;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

@ClientOnly
public final class GlowtonePackCogsWidget extends AbstractWidget {
	private static final Identifier SPRITE = GlowtoneConstants.id("pack/settings");
	private static final Identifier SPRITE_SELECTED = GlowtoneConstants.id("pack/settings_selected");
	private static final Identifier MENU_BACKGROUND = Identifier.withDefaultNamespace("textures/gui/menu_list_background.png");
	private static final Identifier INWORLD_BACKGROUND = Identifier.withDefaultNamespace("textures/gui/inworld_menu_list_background.png");
	private static final int SPRITE_SIZE = 18;
	private static final int PADDING = 2;
	private static final int CELL_WIDTH = SPRITE_SIZE + PADDING * 2;
	private static final int BACKGROUND_TILE = 32;
	private static final int INTERIOR = 0xFF000000;
	private static final int BORDER_FOCUSED = 0xFFFFFFFF;
	private static final int BORDER = 0xFF808080;
	private final TransferableSelectionList list;

	public GlowtonePackCogsWidget(TransferableSelectionList list) {
		super(0, 0, CELL_WIDTH, 0, Component.translatable(GlowtonePackOptionsScreen.TITLE));
		this.list = list;
		this.follow();
	}

	@Override
	protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		this.follow();

		graphics.enableScissor(this.borderLeft(), this.list.getY(), this.getRight(), this.list.getBottom());
		for (TransferableSelectionList.Entry entry : this.list.children()) {
			if (this.declaring(entry)) this.extractCell(graphics, entry, mouseX, mouseY);
		}
		graphics.disableScissor();
	}

	private void extractCell(GuiGraphicsExtractor graphics, TransferableSelectionList.Entry entry, int mouseX, int mouseY) {
		final int left = this.getX();
		final int right = left + CELL_WIDTH;
		final int top = entry.getY();
		final int bottom = top + entry.getHeight();

		final int backgroundLeft = Math.max(left, this.list.getRight());
		if (backgroundLeft < right) {
			graphics.blit(
				RenderPipelines.GUI_TEXTURED,
				Minecraft.getInstance().level == null ? MENU_BACKGROUND : INWORLD_BACKGROUND,
				backgroundLeft, top,
				this.list.getRight() + (backgroundLeft - this.list.getX()),
				this.list.getBottom() + (int) this.list.scrollAmount() + (top - this.list.getY()),
				right - backgroundLeft, bottom - top,
				BACKGROUND_TILE, BACKGROUND_TILE
			);
		}

		if (this.list.getSelected() == entry) {
			final int borderLeft = this.borderLeft();
			graphics.fill(borderLeft, top, right, bottom, this.list.isFocused() ? BORDER_FOCUSED : BORDER);
			graphics.fill(borderLeft, top + 1, right - 1, bottom - 1, INTERIOR);
		}

		final boolean over = mouseX >= left && mouseX < right && mouseY >= top && mouseY < bottom;
		graphics.blitSprite(
			RenderPipelines.GUI_TEXTURED,
			over ? SPRITE_SELECTED : SPRITE,
			left + PADDING, top + (bottom - top - SPRITE_SIZE) / 2,
			SPRITE_SIZE, SPRITE_SIZE
		);
	}

	@Override
	public void onClick(MouseButtonEvent event, boolean doubleClick) {
		final String packId = this.packAt(event.y());
		if (packId == null) return;

		final Minecraft minecraft = Minecraft.getInstance();
		minecraft.gui.setScreen(new GlowtonePackOptionsScreen(minecraft.gui.screen(), packId));
	}

	@Override
	public boolean isMouseOver(double mouseX, double mouseY) {
		return this.active && this.visible
			&& mouseX >= this.getX() && mouseX < this.getRight()
			&& this.packAt(mouseY) != null;
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {}

	private void follow() {
		final int rowRight = this.list.getRowLeft() + this.list.getRowWidth();
		final int scrollbarRight = ((TransferableSelectionListInvoker) this.list).glowtone$scrollBarX() + this.list.scrollbarWidth();
		this.setX(this.list.maxScrollAmount() > 0 ? Math.max(rowRight, scrollbarRight) : rowRight);
		this.setY(this.list.getY());
		this.setHeight(this.list.getHeight());
	}

	private int borderLeft() {
		final int rowRight = this.list.getRowLeft() + this.list.getRowWidth();
		return this.getX() > rowRight ? this.getX() : this.getX() - 1;
	}

	private @Nullable String packAt(double mouseY) {
		if (mouseY < this.list.getY() || mouseY >= this.list.getBottom()) return null;

		for (TransferableSelectionList.Entry entry : this.list.children()) {
			if (this.declaring(entry) && mouseY >= entry.getY() && mouseY < entry.getY() + entry.getHeight()) {
				return ((TransferableSelectionList.PackEntry) entry).getPackId();
			}
		}
		return null;
	}

	private boolean declaring(TransferableSelectionList.Entry entry) {
		return entry instanceof TransferableSelectionList.PackEntry pack
			&& GlowtonePackOptions.declaration(pack.getPackId()) != null;
	}
}
