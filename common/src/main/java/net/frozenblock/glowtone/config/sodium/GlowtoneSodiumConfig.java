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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.caffeinemc.mods.sodium.api.config.ConfigEntryPoint;
import net.caffeinemc.mods.sodium.api.config.ConfigState;
import net.caffeinemc.mods.sodium.api.config.option.OptionFlag;
import net.caffeinemc.mods.sodium.api.config.option.OptionImpact;
import net.caffeinemc.mods.sodium.api.config.structure.ConfigBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.OptionBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.OptionGroupBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.OptionPageBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.StatefulOptionBuilder;
import net.caffeinemc.mods.sodium.client.config.ConfigManager;
import net.frozenblock.glowtone.GlowtoneConstants;
import net.frozenblock.glowtone.config.GlowtoneConfig;
import net.frozenblock.glowtone.config.option.animation.SmoothAnimationOption;
import net.frozenblock.glowtone.config.option.ao.AmbientOcclusionMode;
import net.frozenblock.glowtone.config.option.ao.AmbientOcclusionOption;
import net.frozenblock.glowtone.config.option.ao.OcclusionStrengthOption;
import net.frozenblock.glowtone.config.option.bloom.BloomOption;
import net.frozenblock.glowtone.config.option.color.ColoredLightingMode;
import net.frozenblock.glowtone.config.option.color.ColoredLightingOption;
import net.frozenblock.glowtone.config.option.edge.EdgeHighlightOption;
import net.frozenblock.glowtone.config.option.shade.ShadingMode;
import net.frozenblock.glowtone.config.option.shade.ShadingOption;
import net.frozenblock.glowtone.config.pack.GlowtonePackChoice;
import net.frozenblock.glowtone.config.pack.GlowtonePackCondition;
import net.frozenblock.glowtone.config.pack.GlowtonePackDeclaration;
import net.frozenblock.glowtone.config.pack.GlowtonePackOptions;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.repository.Pack;
import org.jspecify.annotations.Nullable;

@ClientOnly
public final class GlowtoneSodiumConfig implements ConfigEntryPoint {
	private static final String OFF = "options.off";
	private static final String PACK_VERSION = "options.glowtone.packs.version";
	private static final String PACK_TOOLTIP = "options.glowtone.packs.tooltip";
	private static final String PACK_PAGE = "options.glowtone.packs.page";
	private static @Nullable String registeredPacks;

	@Override
	public void registerConfigLate(ConfigBuilder builder) {
		final OptionGroupBuilder lighting = builder.createOptionGroup()
			.setName(caption("colored_lighting"))
			.addOption(builder.createEnumOption(id("colored_lighting"), ColoredLightingMode.class)
				.setName(caption("colored_lighting"))
				.setTooltip(tooltip("colored_lighting"))
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

		final OptionGroupBuilder smoothAnimation = builder.createOptionGroup()
			.setName(caption("smooth_animation"))
			.addOption(builder.createBooleanOption(id("smooth_animation"))
				.setName(caption("smooth_animation"))
				.setTooltip(tooltip("smooth_animation"))
				.setDefaultValue(SmoothAnimationOption.DEFAULT)
				.setBinding(
					value -> SmoothAnimationOption.get().set(value),
					GlowtoneConfig::smoothAnimation
				)
				.setStorageHandler(GlowtoneSodiumConfig::saved)
				.setImpact(OptionImpact.LOW));

		builder.registerOwnModOptions()
			.addPage(builder.createOptionPage()
				.setName(Component.translatable("options.glowtone.page"))
				.addOptionGroup(lighting)
				.addOptionGroup(occlusion)
				.addOptionGroup(highlights)
				.addOptionGroup(emissives)
				.addOptionGroup(smoothAnimation)
			);

		registerPacks(builder);
	}

	public static void packsChanged() {
		if (GlowtonePackOptions.signature().equals(registeredPacks)) return;

		ConfigManager.registerConfigsLate();
	}

	private static void registerPacks(ConfigBuilder builder) {
		registeredPacks = GlowtonePackOptions.signature();

		final List<Pack> packs = GlowtonePackOptions.selectedDeclaring();
		final Map<String, List<GlowtonePackDeclaration.Setting>> shownByPack = new HashMap<>();
		for (Pack pack : packs) {
			final List<GlowtonePackDeclaration.Setting> shown = GlowtonePackOptions.registered(pack.getId());
			if (!shown.isEmpty()) shownByPack.put(pack.getId(), shown);
		}
		final Set<String> siblings = shownByPack.keySet();

		for (Pack pack : packs) {
			final String packId = pack.getId();
			final List<GlowtonePackDeclaration.Setting> shown = shownByPack.get(packId);
			if (shown == null) continue;

			final GlowtonePackDeclaration declaration = GlowtonePackOptions.declaration(packId);
			final String slug = GlowtonePackOptions.slug(packId);
			final OptionPageBuilder page = builder.createOptionPage().setName(Component.translatable(PACK_PAGE));
			for (GlowtonePackDeclaration.Group group : declaration.groups()) {
				final OptionGroupBuilder options = builder.createOptionGroup();
				boolean any = false;
				for (GlowtonePackDeclaration.Setting setting : group.settings()) {
					if (!shown.contains(setting)) continue;

					options.addOption(packOption(builder, packId, slug, setting, pack.getTitle(), declaration, siblings));
					any = true;
				}
				if (!any) continue;

				GlowtonePackOptions.groupName(packId, group).ifPresent(options::setName);
				page.addOptionGroup(options);
			}

			final String name = pack.getTitle().getString();
			builder.registerModOptions(slug, name.isBlank() ? packId : name, Component.translatable(PACK_VERSION).getString())
				.setNonTintedIcon(GlowtonePackOptions.icon(pack))
				.addPage(page);
		}
	}

	private static OptionBuilder packOption(
		ConfigBuilder builder, String packId, String slug, GlowtonePackDeclaration.Setting setting,
		Component packTitle, GlowtonePackDeclaration declaration, Set<String> siblings
	) {
		final Identifier id = Identifier.fromNamespaceAndPath(slug, setting.id());
		final List<String> values = setting.valueIds();
		final boolean hide = declaration.external(setting);
		final OptionBuilder option;

		if (setting.body() instanceof GlowtonePackDeclaration.Slider slider) {
			option = impact(builder.createIntegerOption(id)
				.setName(GlowtonePackOptions.name(packId, setting))
				.setRange(0, slider.steps() - 1, 1)
				.setValueFormatter(step -> GlowtonePackOptions.valueName(packId, setting, values.get(step)))
				.setDefaultValue(slider.step(setting.defaultValue()))
				.setBinding(
					step -> GlowtonePackOptions.setQuietly(packId, setting, values.get(step)),
					() -> slider.step(GlowtonePackOptions.value(packId, setting))
				)
				.setStorageHandler(GlowtoneSodiumConfig::saved)
				.setTooltip(step -> packTooltip(packId, setting, values.get(step), packTitle))
				.setControlHiddenWhenDisabled(hide)
				.setFlags(OptionFlag.REQUIRES_ASSET_RELOAD), setting);
		} else if (setting.isToggle()) {
			option = impact(builder.createBooleanOption(id)
				.setName(GlowtonePackOptions.name(packId, setting))
				.setDefaultValue(Boolean.parseBoolean(setting.defaultValue()))
				.setBinding(
					value -> GlowtonePackOptions.setQuietly(packId, setting, String.valueOf(value)),
					() -> Boolean.parseBoolean(GlowtonePackOptions.value(packId, setting))
				)
				.setStorageHandler(GlowtoneSodiumConfig::saved)
				.setTooltip(value -> packTooltip(packId, setting, String.valueOf(value), packTitle))
				.setControlHiddenWhenDisabled(hide)
				.setFlags(OptionFlag.REQUIRES_ASSET_RELOAD), setting);
		} else {
			option = impact(builder.createEnumOption(id, GlowtonePackChoice.class)
				.setName(GlowtonePackOptions.name(packId, setting))
				.setAllowedValues(GlowtonePackChoice.first(values.size()))
				.setElementNameProvider(slot -> GlowtonePackOptions.valueName(packId, setting, values.get(slot.ordinal())))
				.setDefaultValue(GlowtonePackChoice.of(values.indexOf(setting.defaultValue())))
				.setBinding(
					slot -> GlowtonePackOptions.setQuietly(packId, setting, values.get(slot.ordinal())),
					() -> GlowtonePackChoice.of(values.indexOf(GlowtonePackOptions.value(packId, setting)))
				)
				.setStorageHandler(GlowtoneSodiumConfig::saved)
				.setTooltip(slot -> packTooltip(packId, setting, values.get(slot.ordinal()), packTitle))
				.setControlHiddenWhenDisabled(hide)
				.setFlags(OptionFlag.REQUIRES_ASSET_RELOAD), setting);
		}

		if (setting.requires().equals(GlowtonePackCondition.ALWAYS)) return option;

		final List<Identifier> watched = new ArrayList<>();
		for (String dependency : declaration.dependencies(setting)) {
			if (!visible(packId, dependency)) continue;

			watched.add(Identifier.fromNamespaceAndPath(slug, dependency));
		}
		for (GlowtonePackCondition.Reference reference : declaration.foreign(setting)) {
			final String other = reference.pack().orElseThrow();
			if (!siblings.contains(other) || !visible(other, reference.setting())) continue;

			watched.add(Identifier.fromNamespaceAndPath(GlowtonePackOptions.slug(other), reference.setting()));
		}
		return option.setEnabledProvider(
			state -> declaration.satisfied(setting, other -> read(state, slug, other), environment(state, siblings)),
			watched.toArray(Identifier[]::new)
		);
	}

	private static boolean visible(String packId, String settingId) {
		for (GlowtonePackDeclaration.Setting setting : GlowtonePackOptions.registered(packId)) {
			if (setting.id().equals(settingId)) return true;
		}
		return false;
	}

	private static GlowtonePackDeclaration.Environment environment(ConfigState state, Set<String> siblings) {
		return new GlowtonePackDeclaration.Environment() {
			@Override
			public String stored(String packId, String settingId) {
				if (siblings.contains(packId) && visible(packId, settingId)) {
					final GlowtonePackDeclaration other = GlowtonePackOptions.declaration(packId);
					if (other != null) {
						for (GlowtonePackDeclaration.Setting setting : other.settings()) {
							if (setting.id().equals(settingId)) return read(state, GlowtonePackOptions.slug(packId), setting);
						}
					}
				}
				return GlowtonePackOptions.stored(packId, settingId);
			}

			@Override
			public boolean enabled(String packId) {
				return GlowtonePackOptions.selected(packId);
			}
		};
	}

	private static <V> StatefulOptionBuilder<V> impact(StatefulOptionBuilder<V> option, GlowtonePackDeclaration.Setting setting) {
		return setting.impact()
			.map(impact -> option.setImpact(OptionImpact.valueOf(impact.name())))
			.orElse(option);
	}

	private static Component packTooltip(String packId, GlowtonePackDeclaration.Setting setting, String value, Component packTitle) {
		return GlowtonePackOptions.describe(packId, setting, value).orElseGet(() -> Component.translatable(PACK_TOOLTIP, packTitle));
	}

	private static String read(ConfigState state, String slug, GlowtonePackDeclaration.Setting setting) {
		final Identifier id = Identifier.fromNamespaceAndPath(slug, setting.id());
		if (setting.body() instanceof GlowtonePackDeclaration.Slider slider) {
			return slider.value(state.readIntOption(id));
		}
		if (setting.isToggle()) return String.valueOf(state.readBooleanOption(id));

		return setting.valueIds().get(state.readEnumOption(id, GlowtonePackChoice.class).ordinal());
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
