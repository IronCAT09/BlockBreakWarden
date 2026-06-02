package com.ironcat.blockbreakwarden;

/**
 * Три режима работы мода. Переключаются в GUI и по кругу горячей клавишей.
 */
public enum Mode {
    /** Мод не вмешивается — ломать можно любые блоки. */
    OFF,
    /** Ломать можно ТОЛЬКО блоки/тэги из списка. */
    WHITELIST,
    /** Ломать НЕЛЬЗЯ блоки/тэги из списка, остальное разрешено. */
    BLACKLIST;

    /** Следующий режим по кругу. */
    public Mode next() {
        Mode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    /** Ключ локализации для отображаемого имени режима. */
    public String translationKey() {
        return "mode.blockbreakwarden." + name().toLowerCase();
    }
}
