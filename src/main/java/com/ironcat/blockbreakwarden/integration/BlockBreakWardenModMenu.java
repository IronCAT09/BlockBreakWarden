package com.ironcat.blockbreakwarden.integration;

import com.ironcat.blockbreakwarden.BlockBreakWarden;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * Точка входа Mod Menu: открывает экран настроек BlockBreakWarden из списка модов.
 */
public class BlockBreakWardenModMenu implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return BlockBreakWarden::createConfigScreen;
    }
}
