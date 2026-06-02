package com.ironcat.blockbreakwarden;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

import java.util.List;

/**
 * Решает, можно ли ломать конкретный блок при текущем режиме и списке.
 */
public final class BreakRules {

    private BreakRules() {
    }

    /**
     * Можно ли игроку сломать этот блок согласно конфигу.
     */
    public static boolean canBreak(BlockState state) {
        BlockBreakWardenConfig config = BlockBreakWardenConfig.get();
        return switch (config.mode) {
            case OFF -> true;
            // в whitelist разрешено ломать только то, что есть в списке
            case WHITELIST -> matches(state, config.entries);
            // в blacklist запрещено ломать то, что есть в списке
            case BLACKLIST -> !matches(state, config.entries);
        };
    }

    /**
     * Проверяет, подходит ли блок хотя бы под одну запись списка
     * (точный ID блока или тэг с префиксом {@code #}).
     */
    public static boolean matches(BlockState state, List<String> entries) {
        if (entries == null || entries.isEmpty()) {
            return false;
        }

        Block block = state.getBlock();
        String blockId = Registries.BLOCK.getId(block).toString();

        for (String entry : entries) {
            if (entry == null || entry.isBlank()) {
                continue;
            }
            if (entry.startsWith("#")) {
                Identifier tagId = Identifier.tryParse(entry.substring(1));
                if (tagId == null) {
                    continue;
                }
                TagKey<Block> tag = TagKey.of(RegistryKeys.BLOCK, tagId);
                if (state.isIn(tag)) {
                    return true;
                }
            } else if (blockId.equals(entry)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Нормализует пользовательский ввод в каноничную запись списка.
     *
     * @return нормализованная строка ({@code ns:block} или {@code #ns:tag})
     *         либо {@code null}, если идентификатор некорректен.
     */
    public static String normalize(String raw) {
        if (raw == null) {
            return null;
        }
        String s = raw.trim();
        if (s.isEmpty()) {
            return null;
        }
        if (s.startsWith("#")) {
            Identifier id = Identifier.tryParse(s.substring(1).trim());
            return id == null ? null : "#" + id;
        }
        Identifier id = Identifier.tryParse(s);
        return id == null ? null : id.toString();
    }
}
