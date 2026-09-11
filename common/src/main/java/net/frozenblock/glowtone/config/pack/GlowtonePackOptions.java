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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.frozenblock.glowtone.GlowtoneConstants;
import net.minecraft.ChatFormatting;
import net.frozenblock.glowtone.config.GlowtoneReload;
import net.frozenblock.glowtone.config.sodium.GlowtoneSodiumConfig;
import net.frozenblock.glowtone.mixin.client.pack.FileResourcesSupplierAccessor;
import net.frozenblock.glowtone.mixin.client.pack.PathResourcesSupplierAccessor;
import net.frozenblock.glowtone.platform.GlowtonePlatform;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.packs.FilePackResources;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.util.GsonHelper;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@ClientOnly
public final class GlowtonePackOptions {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final String EXTENSION = ".settings";
	private static final String SLUG_PREFIX = "glowtone_pack_";
	private static final boolean SODIUM = GlowtonePlatform.INSTANCE.isModLoaded("sodium");
	private static final Map<String, GlowtonePackDeclaration> DECLARATIONS = new ConcurrentHashMap<>();
	private static final Map<String, Path> FILES = new ConcurrentHashMap<>();
	private static final Map<String, Map<String, String>> VALUES = new ConcurrentHashMap<>();
	private static final Map<String, Map<String, String>> ACTIVE = new ConcurrentHashMap<>();
	private static final Map<String, Identifier> ICONS = new ConcurrentHashMap<>();
	private static final Map<String, Map<String, String>> TRANSLATIONS = new ConcurrentHashMap<>();
	private static final Map<String, String> FAILURES = new ConcurrentHashMap<>();
	private static final List<Runnable> WIDGET_REFRESH = new ArrayList<>();
	private static boolean registerPending;
	public static final GlowtonePackDeclaration.Environment ENVIRONMENT = new GlowtonePackDeclaration.Environment() {
		@Override
		public @Nullable String stored(String packId, String settingId) {
			return GlowtonePackOptions.stored(packId, settingId);
		}

		@Override
		public boolean enabled(String packId) {
			return selected(packId);
		}
	};
	private static final Identifier DEFAULT_ICON = Identifier.withDefaultNamespace("textures/misc/unknown_pack.png");

	public static void declare(String packId, Pack.ResourcesSupplier supplier, PackResources resources) {
		invalidate();
		try {
			final GlowtonePackDeclaration declaration = resources.getMetadataSection(GlowtonePackDeclaration.TYPE);
			if (declaration == null || declaration.settings().isEmpty()) {
				DECLARATIONS.remove(packId);
				return;
			}

			DECLARATIONS.put(packId, declaration);
			FAILURES.remove(packId);
			TRANSLATIONS.remove(packId);
			FILES.put(packId, settingsFile(supplier, packId));
			VALUES.remove(packId);
			if (!Files.exists(FILES.get(packId))) save(packId);
			LOGGER.info("Glowtone pack {} declares {} settings, stored in {}", packId, declaration.settings().size(), FILES.get(packId));
		} catch (IOException | RuntimeException failure) {
			DECLARATIONS.remove(packId);
			FAILURES.put(packId, message(failure));
			LOGGER.error("Glowtone could not read the settings declared by pack {}: {}", packId, message(failure));
		}
	}

	public static @Nullable GlowtonePackDeclaration declaration(String packId) {
		return DECLARATIONS.get(packId);
	}

	public static @Nullable String failure(String packId) {
		return FAILURES.get(packId);
	}

	private static String message(Throwable failure) {
		final String message = failure.getMessage();
		return message == null || message.isBlank() ? failure.getClass().getSimpleName() : message;
	}

	public static Identifier icon(Pack pack) {
		return ICONS.computeIfAbsent(pack.getId(), id -> loadIcon(pack));
	}

	private static Identifier loadIcon(Pack pack) {
		try (PackResources resources = pack.open()) {
			final IoSupplier<InputStream> supplier = resources.getRootResource("pack.png");
			if (supplier == null) return DEFAULT_ICON;

			final Identifier location = GlowtoneConstants.id("pack/" + sanitise(pack.getId()));
			try (InputStream stream = supplier.get()) {
				Minecraft.getInstance().getTextureManager()
					.register(location, new DynamicTexture(location::toString, NativeImage.read(stream)));
			}
			return location;
		} catch (IOException | RuntimeException failure) {
			LOGGER.warn("Glowtone could not read the icon of pack {}", pack.getId(), failure);
			return DEFAULT_ICON;
		}
	}

	public static Pack.Metadata withSubpacks(String packId, Pack.Metadata metadata) {
		final GlowtonePackDeclaration declaration = DECLARATIONS.get(packId);
		if (declaration == null) return metadata;

		final List<String> overlays = new ArrayList<>(metadata.overlays());
		final List<String> mounted = new ArrayList<>();
		for (String directory : declaration.activeSubpacks(active(packId, declaration), ENVIRONMENT)) {
			if (overlays.contains(directory)) continue;

			overlays.add(directory);
			mounted.add(directory);
		}
		if (mounted.isEmpty()) return metadata;

		LOGGER.info("Glowtone mounting subpacks {} for pack {}", mounted, packId);
		return new Pack.Metadata(metadata.description(), metadata.compatibility(), metadata.requestedFeatures(), List.copyOf(overlays));
	}

	public static Map<String, String> active(String packId, GlowtonePackDeclaration declaration) {
		return ACTIVE.computeIfAbsent(packId, id -> Collections.unmodifiableMap(
			declaration.active(setting -> value(id, setting), ENVIRONMENT)
		));
	}

	static void invalidate() {
		ACTIVE.clear();
	}

	public static boolean hidden(String packId, GlowtonePackDeclaration.Setting setting) {
		final GlowtonePackDeclaration declaration = DECLARATIONS.get(packId);
		return declaration != null && declaration.external(setting);
	}

	public static boolean active(String packId, GlowtonePackDeclaration.Setting setting) {
		final GlowtonePackDeclaration declaration = DECLARATIONS.get(packId);
		return declaration == null || active(packId, declaration).containsKey(setting.id());
	}

	public static @Nullable String stored(String packId, String settingId) {
		final GlowtonePackDeclaration declaration = DECLARATIONS.get(packId);
		if (declaration != null) {
			final GlowtonePackDeclaration.Setting setting = declaration.setting(settingId);
			if (setting != null) return value(packId, setting);
		}
		return values(packId).get(settingId);
	}

	public static @Nullable String anywhere(String settingId) {
		for (Map.Entry<String, GlowtonePackDeclaration> declaring : DECLARATIONS.entrySet()) {
			final GlowtonePackDeclaration.Setting setting = declaring.getValue().setting(settingId);
			if (setting != null) return value(declaring.getKey(), setting);
		}
		return null;
	}

	public static boolean selected(String packId) {
		for (Pack pack : Minecraft.getInstance().getResourcePackRepository().getSelectedPacks()) {
			if (pack.getId().equals(packId)) return true;
		}
		return false;
	}

	public static String value(String packId, GlowtonePackDeclaration.Setting setting) {
		final String stored = values(packId).get(setting.id());
		return stored != null && setting.accepts(stored) ? stored : setting.defaultValue();
	}

	public static void set(String packId, GlowtonePackDeclaration.Setting setting, String value) {
		if (setQuietly(packId, setting, value)) GlowtoneReload.request();
	}

	public static boolean setQuietly(String packId, GlowtonePackDeclaration.Setting setting, String value) {
		if (value.equals(value(packId, setting))) return false;

		values(packId).put(setting.id(), value);
		invalidate();
		resetUnmet(packId);
		save(packId);
		GlowtonePackApi.changed();
		return true;
	}

	public static boolean resetUnmet(String packId) {
		final GlowtonePackDeclaration declaration = DECLARATIONS.get(packId);
		if (declaration == null) return false;

		final Map<String, String> active = active(packId, declaration);
		final Map<String, String> stored = values(packId);
		boolean reset = false;
		for (GlowtonePackDeclaration.Setting setting : declaration.settings()) {
			if (active.containsKey(setting.id())) continue;

			final String current = stored.get(setting.id());
			if (current == null || current.equals(setting.defaultValue())) continue;

			stored.put(setting.id(), setting.defaultValue());
			reset = true;
		}
		if (reset) invalidate();
		return reset;
	}

	public static void flush() {
		if (!registerPending || GlowtoneReload.settingsOpen()) return;

		registerPending = false;
		if (SODIUM) GlowtoneSodiumConfig.packsChanged();
	}

	public static Component title(String packId) {
		final Pack pack = Minecraft.getInstance().getResourcePackRepository().getPack(packId);
		return pack != null ? pack.getTitle() : Component.literal(packId);
	}

	public static String slug(String packId) {
		return SLUG_PREFIX + sanitise(packId) + "_" + Integer.toHexString(packId.hashCode());
	}

	public static List<Pack> selectedDeclaring() {
		final List<Pack> packs = new ArrayList<>();
		for (Pack pack : Minecraft.getInstance().getResourcePackRepository().getSelectedPacks()) {
			if (DECLARATIONS.containsKey(pack.getId())) packs.add(pack);
		}
		return packs;
	}

	public static String signature() {
		final StringBuilder builder = new StringBuilder();
		for (Pack pack : selectedDeclaring()) {
			builder.append(pack.getId()).append('\n');
			for (GlowtonePackDeclaration.Setting setting : registered(pack.getId())) {
				builder.append('\t').append(setting.id()).append('\n');
			}
		}
		return builder.toString();
	}

	public static List<GlowtonePackDeclaration.Setting> registered(String packId) {
		final GlowtonePackDeclaration declaration = DECLARATIONS.get(packId);
		if (declaration == null) return List.of();

		final Map<String, String> active = active(packId, declaration);
		final List<GlowtonePackDeclaration.Setting> settings = new ArrayList<>();
		for (GlowtonePackDeclaration.Setting setting : declaration.settings()) {
			if (declaration.external(setting) && !active.containsKey(setting.id())) continue;

			settings.add(setting);
		}
		return settings;
	}

	public static Component name(String packId, GlowtonePackDeclaration.Setting setting) {
		return translate(packId, setting.name(), setting.id());
	}

	public static Component valueName(String packId, GlowtonePackDeclaration.Setting setting, String value) {
		return translate(packId, setting.valueKey(value), value);
	}

	public static Optional<Component> describe(String packId, GlowtonePackDeclaration.Setting setting, String value) {
		return setting.tooltipKey(value).map(key -> translate(packId, Optional.of(key), key));
	}

	public static Optional<Component> groupName(String packId, GlowtonePackDeclaration.Group group) {
		return group.category().map(key -> translate(packId, Optional.of(key), key));
	}

	public static Component translate(String packId, Optional<String> key, String fallback) {
		if (key.isEmpty()) return GlowtonePackDeclaration.fallbackName(Optional.empty(), fallback);

		final String id = key.get();
		if (Language.getInstance().has(id)) return Component.translatable(id);

		final String own = translations(packId).get(id);
		return own != null ? Component.literal(own) : GlowtonePackDeclaration.fallbackName(key, fallback);
	}

	private static Map<String, String> translations(String packId) {
		return TRANSLATIONS.computeIfAbsent(packId, GlowtonePackOptions::readTranslations);
	}

	private static Map<String, String> readTranslations(String packId) {
		final Map<String, String> loaded = new LinkedHashMap<>();
		final Pack pack = Minecraft.getInstance().getResourcePackRepository().getPack(packId);
		if (pack == null) return loaded;

		final String code = Minecraft.getInstance().options.languageCode;
		try (PackResources resources = pack.open()) {
			for (String namespace : resources.getNamespaces(PackType.CLIENT_RESOURCES)) {
				readLanguage(resources, namespace, "en_us", loaded);
				if (!"en_us".equals(code)) readLanguage(resources, namespace, code, loaded);
			}
		} catch (RuntimeException failure) {
			LOGGER.warn("Glowtone could not read the translations of pack {}", packId, failure);
		}
		return loaded;
	}

	private static void readLanguage(PackResources resources, String namespace, String code, Map<String, String> into) {
		final IoSupplier<InputStream> supplier = resources.getResource(
			PackType.CLIENT_RESOURCES,
			Identifier.fromNamespaceAndPath(namespace, "lang/" + code + ".json")
		);
		if (supplier == null) return;

		try (Reader reader = new InputStreamReader(supplier.get(), StandardCharsets.UTF_8)) {
			for (Map.Entry<String, JsonElement> entry : GsonHelper.parse(reader).entrySet()) {
				if (entry.getValue().isJsonPrimitive()) into.put(entry.getKey(), entry.getValue().getAsString());
			}
		} catch (IOException | RuntimeException failure) {
			LOGGER.warn("Glowtone could not read {} translations from {}", code, namespace, failure);
		}
	}

	public static boolean addTo(OptionsList list, @Nullable String onlyPackId) {
		forgetWidgets();
		if (onlyPackId != null) {
			final GlowtonePackDeclaration declaration = DECLARATIONS.get(onlyPackId);
			if (declaration == null || declaration.settings().isEmpty()) return false;

			addGroups(list, onlyPackId, declaration);
			return true;
		}

		boolean added = false;
		for (Pack pack : selectedDeclaring()) {
			final GlowtonePackDeclaration declaration = DECLARATIONS.get(pack.getId());
			if (declaration.settings().isEmpty()) continue;

			list.addHeader(pack.getTitle());
			addGroups(list, pack.getId(), declaration);
			added = true;
		}
		return added;
	}

	private static void addGroups(OptionsList list, String packId, GlowtonePackDeclaration declaration) {
		boolean first = true;
		for (GlowtonePackDeclaration.Group group : declaration.groups()) {
			final List<AbstractWidget> widgets = widgets(packId, group.settings());
			if (widgets.isEmpty()) continue;

			final Optional<Component> name = groupName(packId, group);
			if (name.isPresent()) {
				list.addHeader(name.get());
			} else if (!first) {
				list.addHeader(CommonComponents.EMPTY);
			}

			first = false;
			list.addSmall(widgets);
		}
	}

	public static void afterReload() {
		for (String packId : DECLARATIONS.keySet()) {
			if (resetUnmet(packId)) save(packId);
		}
		GlowtonePackApi.changed();
		registerPending = true;
		flush();
	}

	private static List<AbstractWidget> widgets(String packId, List<GlowtonePackDeclaration.Setting> settings) {
		final List<AbstractWidget> widgets = new ArrayList<>(settings.size());
		for (GlowtonePackDeclaration.Setting setting : settings) {
			final boolean active = active(packId, setting);
			if (!active && hidden(packId, setting)) continue;

			final AbstractWidget widget = widget(packId, setting);
			widget.active = active;
			widgets.add(widget);
		}
		return widgets;
	}

	public static void refreshWidgets() {
		for (Runnable refresh : WIDGET_REFRESH) refresh.run();
	}

	public static void forgetWidgets() {
		WIDGET_REFRESH.clear();
	}

	private static AbstractWidget widget(String packId, GlowtonePackDeclaration.Setting setting) {
		final Component name = name(packId, setting);
		if (setting.body() instanceof GlowtonePackDeclaration.Slider slider) {
			final GlowtonePackSlider widget = new GlowtonePackSlider(packId, setting, slider);
			WIDGET_REFRESH.add(() -> {
				widget.active = active(packId, setting);
				widget.sync();
			});
			return widget;
		}

		if (setting.isToggle()) {
			final CycleButton<Boolean> widget = CycleButton.onOffBuilder(Boolean.parseBoolean(value(packId, setting)))
				.withTooltip(value -> tooltip(packId, setting, String.valueOf(value)))
				.create(0, 0, 150, 20, name, (button, value) -> change(packId, setting, String.valueOf(value)));
			WIDGET_REFRESH.add(() -> {
				widget.active = active(packId, setting);
				widget.setValue(Boolean.parseBoolean(value(packId, setting)));
			});
			return widget;
		}

		final CycleButton<String> widget = CycleButton.builder(value -> valueName(packId, setting, value), value(packId, setting))
			.withValues(setting.valueIds())
			.withTooltip(value -> tooltip(packId, setting, value))
			.create(0, 0, 150, 20, name, (button, value) -> change(packId, setting, value));
		WIDGET_REFRESH.add(() -> {
			widget.active = active(packId, setting);
			widget.setValue(value(packId, setting));
		});
		return widget;
	}

	static void change(String packId, GlowtonePackDeclaration.Setting setting, String value) {
		set(packId, setting, value);
		refreshWidgets();
	}

	static void stage(String packId, GlowtonePackDeclaration.Setting setting, String value) {
		if (value.equals(value(packId, setting))) return;

		values(packId).put(setting.id(), value);
		invalidate();
	}

	static void commit(String packId) {
		resetUnmet(packId);
		save(packId);
		GlowtonePackApi.changed();
		GlowtoneReload.request();
		refreshWidgets();
	}

	private static @Nullable Tooltip tooltip(String packId, GlowtonePackDeclaration.Setting setting, String value) {
		final Optional<Component> described = describe(packId, setting, value);
		final Optional<GlowtonePackImpact> impact = setting.impact();
		if (described.isEmpty() && impact.isEmpty()) return null;

		final MutableComponent lines = Component.empty();
		described.ifPresent(lines::append);
		impact.ifPresent(level -> {
			if (described.isPresent()) lines.append(CommonComponents.NEW_LINE);
			lines.append(level.describe().withStyle(ChatFormatting.GRAY));
		});
		return Tooltip.create(lines);
	}

	private static Path settingsFile(Pack.ResourcesSupplier supplier, String packId) {
		if (supplier instanceof FilePackResources.FileResourcesSupplier file) {
			return sibling(((FileResourcesSupplierAccessor) file).glowtone$content().toPath());
		}
		if (supplier instanceof PathPackResources.PathResourcesSupplier path) {
			return sibling(((PathResourcesSupplierAccessor) path).glowtone$content());
		}
		return GlowtonePlatform.INSTANCE.getConfigDirectory().resolve("glowtone").resolve("packs").resolve(sanitise(packId) + EXTENSION);
	}

	private static Path sibling(Path content) {
		final String name = content.getFileName().toString();
		final int dot = name.lastIndexOf('.');
		final String stem = dot > 0 ? name.substring(0, dot) : name;
		final Path parent = content.toAbsolutePath().getParent();
		return (parent != null ? parent : content.toAbsolutePath()).resolve(stem + EXTENSION);
	}

	private static String sanitise(String packId) {
		final StringBuilder builder = new StringBuilder(packId.length());
		for (char character : packId.toLowerCase(Locale.ROOT).toCharArray()) {
			final boolean allowed = (character >= 'a' && character <= 'z') || (character >= '0' && character <= '9')
				|| character == '_' || character == '-' || character == '.';
			builder.append(allowed ? character : '_');
		}
		return builder.toString();
	}

	private static Map<String, String> values(String packId) {
		return VALUES.computeIfAbsent(packId, id -> load(FILES.get(id)));
	}

	private static Map<String, String> load(@Nullable Path path) {
		final Map<String, String> loaded = new LinkedHashMap<>();
		if (path == null || !Files.exists(path)) return loaded;

		try (Reader reader = Files.newBufferedReader(path)) {
			for (Map.Entry<String, JsonElement> entry : GsonHelper.parse(reader).entrySet()) {
				if (entry.getValue().isJsonPrimitive()) loaded.put(entry.getKey(), entry.getValue().getAsString());
			}
		} catch (IOException | RuntimeException failure) {
			LOGGER.error("Failed to read {}", path, failure);
		}
		return loaded;
	}

	private static void save(String packId) {
		final Path path = FILES.get(packId);
		final GlowtonePackDeclaration declaration = DECLARATIONS.get(packId);
		if (path == null || declaration == null) return;

		try {
			final Path parent = path.getParent();
			if (parent != null) Files.createDirectories(parent);

			final JsonObject json = new JsonObject();
			for (GlowtonePackDeclaration.Setting setting : declaration.settings()) {
				json.addProperty(setting.id(), value(packId, setting));
			}
			try (Writer writer = Files.newBufferedWriter(path)) {
				writer.write(json.toString());
			}
		} catch (IOException failure) {
			LOGGER.error("Failed to write {}", path, failure);
		}
	}

	private GlowtonePackOptions() {}
}
