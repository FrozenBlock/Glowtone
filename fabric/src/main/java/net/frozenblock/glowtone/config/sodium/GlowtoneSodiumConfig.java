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

package net.frozenblock.glowtone.config.sodium;

import net.caffeinemc.mods.sodium.api.config.ConfigEntryPoint;
import net.caffeinemc.mods.sodium.api.config.option.OptionFlag;
import net.caffeinemc.mods.sodium.api.config.option.OptionImpact;
import net.caffeinemc.mods.sodium.api.config.structure.ConfigBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.OptionGroupBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.frozenblock.glowtone.GlowtoneConstants;
import net.frozenblock.glowtone.config.option.ao.AmbientOcclusionMode;
import net.frozenblock.glowtone.config.option.ao.AmbientOcclusionOption;
import net.frozenblock.glowtone.config.option.bloom.BloomOption;
import net.frozenblock.glowtone.config.option.color.ColoredLightingMode;
import net.frozenblock.glowtone.config.option.color.ColoredLightingOption;
import net.frozenblock.glowtone.config.option.edge.EdgeHighlightOption;
import net.frozenblock.glowtone.config.GlowtoneConfig;
import net.frozenblock.glowtone.config.option.ao.OcclusionStrengthOption;
import net.frozenblock.glowtone.config.option.shade.ShadingMode;
import net.frozenblock.glowtone.config.option.shade.ShadingOption;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

@Environment(EnvType.CLIENT)
public final class GlowtoneSodiumConfig implements ConfigEntryPoint {
	private static final String OFF = "options.off";

	@Override
	public void registerConfigLate(ConfigBuilder builder) {
		final OptionGroupBuilder lighting = builder.createOptionGroup()
			.setName(caption("coloured_lighting"))
			.addOption(builder.createEnumOption(id("coloured_lighting"), ColoredLightingMode.class)
				.setName(caption("coloured_lighting"))
				.setTooltip(tooltip("coloured_lighting"))
				.setElementNameProvider(mode -> Component.translatable(mode.translationKey()))
				.setDefaultValue(ColoredLightingMode.SUBTLE)
				.setBinding(
					mode -> ColoredLightingOption.get().set(mode),
					GlowtoneConfig::coloredLighting
				)
				.setStorageHandler(GlowtoneSodiumConfig::saved)
				.setImpact(OptionImpact.MEDIUM)
				.setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD));

		final OptionGroupBuilder occlusion = builder.createOptionGroup()
			.setName(caption("ambient_occlusion"))
			.addOption(builder.createEnumOption(id("ambient_occlusion"), AmbientOcclusionMode.class)
				.setName(caption("ambient_occlusion"))
				.setTooltip(tooltip("ambient_occlusion"))
				.setElementNameProvider(mode -> Component.translatable(mode.translationKey()))
				.setDefaultValue(AmbientOcclusionMode.FANCY)
				.setBinding(
					mode -> AmbientOcclusionOption.get().set(mode),
					GlowtoneConfig::ambientOcclusion
				)
				.setStorageHandler(GlowtoneSodiumConfig::saved)
				.setImpact(OptionImpact.LOW)
				.setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD))
			.addOption(builder.createIntegerOption(id("occlusion_strength"))
				.setName(caption("occlusion_strength"))
				.setTooltip(tooltip("occlusion_strength"))
				.setRange(OcclusionStrengthOption.MIN, OcclusionStrengthOption.MAX, 1)
				.setValueFormatter(GlowtoneSodiumConfig::percent)
				.setDefaultValue(OcclusionStrengthOption.VANILLA)
				.setBinding(
					value -> OcclusionStrengthOption.get().set(value),
					GlowtoneConfig::occlusionStrength
				)
				.setStorageHandler(OcclusionStrengthOption::flush)
				.setImpact(OptionImpact.LOW)
				.setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD));

		final OptionGroupBuilder highlights = builder.createOptionGroup()
			.setName(caption("edge_highlight"))
			.addOption(builder.createIntegerOption(id("edge_highlight"))
				.setName(caption("edge_highlight"))
				.setTooltip(tooltip("edge_highlight"))
				.setRange(EdgeHighlightOption.MIN, EdgeHighlightOption.MAX, 1)
				.setValueFormatter(GlowtoneSodiumConfig::percent)
				.setDefaultValue(EdgeHighlightOption.DEFAULT)
				.setBinding(
					value -> EdgeHighlightOption.get().set(value),
					GlowtoneConfig::edgeHighlight
				)
				.setStorageHandler(EdgeHighlightOption::flush)
				.setImpact(OptionImpact.LOW)
				.setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD));

		final OptionGroupBuilder emissives = builder.createOptionGroup()
			.setName(caption("emissives"))

			.addOption(builder.createIntegerOption(id("bloom"))
				.setName(caption("bloom"))
				.setTooltip(tooltip("bloom"))
				.setRange(BloomOption.MIN, BloomOption.MAX, 1)
				.setValueFormatter(GlowtoneSodiumConfig::percent)
				.setDefaultValue(BloomOption.PRESET_DEFAULT)
				.setBinding(
					value -> BloomOption.get().set(value),
					GlowtoneConfig::bloom
				)
				.setStorageHandler(GlowtoneSodiumConfig::saved)
				.setImpact(OptionImpact.MEDIUM)
				.setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD))
			.addOption(builder.createEnumOption(id("shading"), ShadingMode.class)
				.setName(caption("shading"))
				.setTooltip(tooltip("shading"))
				.setElementNameProvider(mode -> Component.translatable(mode.translationKey()))
				.setDefaultValue(ShadingMode.DEFAULT)
				.setBinding(
					mode -> ShadingOption.get().set(mode),
					GlowtoneConfig::shading
				)
				.setStorageHandler(GlowtoneSodiumConfig::saved)
				.setImpact(OptionImpact.LOW)
				.setFlags(OptionFlag.REQUIRES_ASSET_RELOAD));

		builder.registerOwnModOptions()
			.addPage(builder.createOptionPage()
				.setName(Component.translatable("options.glowtone.page"))
				.addOptionGroup(lighting)
				.addOptionGroup(occlusion)
				.addOptionGroup(highlights)
				.addOptionGroup(emissives));
	}

	private static void saved() {}

	private static Component percent(int value) {
		return value == 0
			? Component.translatable(OFF)
			: Component.literal(value + "%");
	}

	private static Component caption(String key) {
		return Component.translatable("options.glowtone." + key);
	}

	private static Component tooltip(String key) {
		return Component.translatable("options.glowtone." + key + ".tooltip");
	}

	private static Identifier id(String path) {
		return GlowtoneConstants.id(path);
	}
}
