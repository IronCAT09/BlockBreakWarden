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
 * <p>{@link #entries} хранит записи двух видов:
 * <ul>
 *     <li>ID блока, например {@code minecraft:oak_log};</li>
 *     <li>тэг блока с префиксом {@code #}, например {@code #minecraft:logs}.</li>
 * </ul>
 */
public class BlockBreakWardenConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("blockbreakwarden.json");

    private static BlockBreakWardenConfig instance;

    // --- сохраняемые поля ---
    public Mode mode = Mode.OFF;
    public boolean showWarnings = true;
    public boolean warningSound = true;
    public List<String> entries = new ArrayList<>();

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
        if (loaded.entries == null) loaded.entries = new ArrayList<>();

        instance = loaded;
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
