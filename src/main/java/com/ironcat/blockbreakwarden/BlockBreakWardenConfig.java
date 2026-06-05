package com.ironcat.blockbreakwarden;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Конфиг мода. Сохраняется в config/blockbreakwarden.json.
 *
 * <p>{@link #whitelistEntries} и {@link #blacklistEntries} хранят записи двух видов:
 * <ul>
 *     <li>ID блока, например {@code minecraft:oak_log};</li>
 *     <li>тэг блока с префиксом {@code #}, например {@code #minecraft:logs}.</li>
 * </ul>
 * Whitelist и blacklist — независимые списки, редактируются раздельно.
 */
public class BlockBreakWardenConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("blockbreakwarden.json");

    private static BlockBreakWardenConfig instance;

    // --- сохраняемые поля ---
    public Mode mode = Mode.OFF;
    public boolean showWarnings = true;
    public boolean warningSound = true;
    /** Записи для режима whitelist (ломать можно только их). */
    public List<String> whitelistEntries = new ArrayList<>();
    /** Записи для режима blacklist (ломать нельзя только их). */
    public List<String> blacklistEntries = new ArrayList<>();

    /**
     * Устаревшее единое поле (до 1.0.2). Оставлено только для миграции старого
     * конфига в {@link #whitelistEntries}/{@link #blacklistEntries}. Не сериализуется,
     * пока {@code null} (Gson по умолчанию не пишет null-поля).
     */
    @Deprecated
    public List<String> entries;

    /** Текущий конфиг (ленивая загрузка). */
    public static BlockBreakWardenConfig get() {
        if (instance == null) {
            load();
        }
        return instance;
    }

    public static void load() {
        BlockBreakWardenConfig loaded = null;
        try {
            if (Files.exists(PATH)) {
                try (Reader reader = Files.newBufferedReader(PATH)) {
                    loaded = GSON.fromJson(reader, BlockBreakWardenConfig.class);
                }
            }
        } catch (Exception e) {
            BlockBreakWarden.LOGGER.error("Не удалось загрузить конфиг BlockBreakWarden, использую значения по умолчанию", e);
        }

        if (loaded == null) {
            loaded = new BlockBreakWardenConfig();
        }
        // защита от повреждённого/устаревшего json
        if (loaded.mode == null) loaded.mode = Mode.OFF;
        if (loaded.whitelistEntries == null) loaded.whitelistEntries = new ArrayList<>();
        if (loaded.blacklistEntries == null) loaded.blacklistEntries = new ArrayList<>();

        // миграция старого единого списка (до 1.0.2) в список, соответствующий режиму
        if (loaded.entries != null && !loaded.entries.isEmpty()) {
            List<String> target = loaded.entriesFor(
                    loaded.mode == Mode.BLACKLIST ? Mode.BLACKLIST : Mode.WHITELIST);
            for (String entry : loaded.entries) {
                if (entry != null && !target.contains(entry)) {
                    target.add(entry);
                }
            }
        }
        loaded.entries = null; // больше не сохраняем устаревшее поле

        instance = loaded;
    }

    /**
     * Список записей для указанного режима. Для {@link Mode#OFF} возвращает
     * whitelist (нейтральный выбор по умолчанию).
     */
    public List<String> entriesFor(Mode mode) {
        return mode == Mode.BLACKLIST ? blacklistEntries : whitelistEntries;
    }

    public static void save() {
        try {
            Files.createDirectories(PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(PATH)) {
                GSON.toJson(get(), writer);
            }
        } catch (Exception e) {
            BlockBreakWarden.LOGGER.error("Не удалось сохранить конфиг BlockBreakWarden", e);
        }
    }
}
